#include "bounding_region.hpp"
#include "bounding_inequalities.hpp"

static PointQ intersection(const LinComArrZ<XYEta>& line1, const LinComArrZ<XYEta>& line2) {
    // line1 = a1*a + b1*b + c1*eta
    // line2 = a2*a + b2*b + c2*eta
    // [ a1 b1 ] [ a ] = [ - c1*eta ]
    // [ a2 b2 ] [ b ] = [ - c2*eta ]
    // now solve using Cramer's rule

    // Calculating the following determinants can overflow for 32 bit integers, so we
    // convert them to 64 bits before doing the math.
    const Coeff64 a1 = line1.coeff<XYEta::X>();
    const Coeff64 b1 = line1.coeff<XYEta::Y>();
    const Coeff64 c1 = line1.coeff<XYEta::Eta>();

    const Coeff64 a2 = line2.coeff<XYEta::X>();
    const Coeff64 b2 = line2.coeff<XYEta::Y>();
    const Coeff64 c2 = line2.coeff<XYEta::Eta>();

    const auto det = a1 * b2 - b1 * a2;

// Divison by zero is also checked in the constructor,
// so we will only check here in debug mode
#ifndef NDEBUG
    if (det == 0) {
        std::ostringstream err;
        err << "degenerate system " << line1 << " = 0, " << line2 << " = 0";
        throw std::runtime_error(err.str());
    }
#endif

    // For the GMP backend, the denominator is an unsigned
    // integer. This is not always the case with the current
    // setup, and sometimes the denominator is -1, which is
    // cast to an unsigned integer and so gets really huge
    // This is not a problem for the Boost cpp thing
    // TODO report the bug to the Boost people.

    // We can't do this unfortunately, because GMP expects
    // the denominator to be unsigned, which it isn't.
    //const Rational first{b1 * c2 - b2 * c1, det};
    //const Rational second{a2 * c1 - a1 * c2, det};

    // Workaround for GMP backend
    const Rational first = Rational{b1 * c2 - b2 * c1} / det;
    const Rational second = Rational{a2 * c1 - a1 * c2} / det;

    return {first, second};
}

static Sign line_sign_at_point(const LinComArrZ<XYEta>& equation, const PointQ& point) {

    // c1 * x + c2 * y + c3 * eta
    const auto x_coeff = equation.coeff<XYEta::X>();
    const auto y_coeff = equation.coeff<XYEta::Y>();
    const auto eta_coeff = equation.coeff<XYEta::Eta>();

    const Rational result = x_coeff * point.x + y_coeff * point.y + eta_coeff;

    const auto sign = result.sign();

    if (sign > 0) {
        return Sign::POS;
    }

    if (sign < 0) {
        return Sign::NEG;
    }

    if (sign == 0) {
        return Sign::ZERO;
    }

    // Strictly speaking, this shouldn't be necessary, since integers obey
    // trichotomy, but the type system is unaware of that, so I'll leave
    // this in here.
    throw std::runtime_error(result.str() + " has no sign");
}

// TODO find a better way of doing this
#define CASE1(x, y, z) if (zero_zero_sign == x && pi_zero_sign == y && zero_pi_sign == z)

// Calculate where the constraint intersects the (0,0) (0, 2*eta), (2*eta, 0) triangle
// First coord is a, second is b
// Both of these have an empty initial line segment
// 1 1 3 1 3 1 1 5 2 2 1 1 5 3
// 1 1 3 1 5 1 1 5 2 2 1 1 5 3
// When doing a switch statement, don't rely on the fallthrough nature: is that what you
// want, or did you just forget to deal with that case? Do each one individually
static boost::optional<RationalLineSegment> initial_line_segment(const LinComArrZ<XYEta>& constraint) {

    // The most basic constraints are that x, y, z > 0
    // This gives us the open set of the triangle
    // Then, we can translate the line segment into x and y, as we see fit
    const LinComArrZ<XYEta> a{1, 0, 0};   // x > 0
    const LinComArrZ<XYEta> b{0, 1, 0};   // y > 0
    const LinComArrZ<XYEta> c{-1, -1, 2}; // z = - x - y + 2*eta > 0

    const auto zero_zero_sign = line_sign_at_point(constraint, {0, 0});
    const auto pi_zero_sign = line_sign_at_point(constraint, {2, 0});
    const auto zero_pi_sign = line_sign_at_point(constraint, {0, 2});

    // (0, 0) -> b
    // (2, 0) -> c
    // (0, 2) -> a

    // The line segment must be part of the interior of the triangle
    // Anything lying along the edges must get thrown out
    // a_inter, b_inter, c_inter

    // As tedious as this seems, we need to deal with all 3^3 = 27 cases
    // individually

    CASE1(Sign::NEG, Sign::NEG, Sign::NEG) {
        return boost::none;
    }

    CASE1(Sign::NEG, Sign::NEG, Sign::ZERO) {
        return boost::none;
    }

    CASE1(Sign::NEG, Sign::NEG, Sign::POS) {
        const auto a_inter = intersection(constraint, a);
        const auto c_inter = intersection(constraint, c);
        return RationalLineSegment{a_inter, a, c_inter, c};
    }

    CASE1(Sign::NEG, Sign::ZERO, Sign::NEG) {
        return boost::none;
    }

    CASE1(Sign::NEG, Sign::ZERO, Sign::ZERO) {
        return boost::none;
    }

    CASE1(Sign::NEG, Sign::ZERO, Sign::POS) {
        const auto a_inter = intersection(constraint, a);
        return RationalLineSegment{a_inter, a, {2, 0}, c};
    }

    CASE1(Sign::NEG, Sign::POS, Sign::NEG) {
        const auto b_inter = intersection(constraint, b);
        const auto c_inter = intersection(constraint, c);
        return RationalLineSegment{b_inter, b, c_inter, c};
    }

    CASE1(Sign::NEG, Sign::POS, Sign::ZERO) {
        const auto b_inter = intersection(constraint, b);
        return RationalLineSegment{{0, 2}, a, b_inter, b};
    }

    CASE1(Sign::NEG, Sign::POS, Sign::POS) {
        const auto a_inter = intersection(constraint, a);
        const auto b_inter = intersection(constraint, b);
        return RationalLineSegment{a_inter, a, b_inter, b};
    }

    CASE1(Sign::ZERO, Sign::NEG, Sign::NEG) {
        return boost::none;
    }

    CASE1(Sign::ZERO, Sign::NEG, Sign::ZERO) {
        return boost::none;
    }

    CASE1(Sign::ZERO, Sign::NEG, Sign::POS) {
        const auto c_inter = intersection(constraint, c);
        return RationalLineSegment{{0, 0}, b, c_inter, c};
    }

    CASE1(Sign::ZERO, Sign::ZERO, Sign::NEG) {
        return boost::none;
    }

    CASE1(Sign::ZERO, Sign::ZERO, Sign::ZERO) {
        // Mathematically impossible
        throw std::runtime_error("zero-zero-zero case in initial_line_segment");
    }

    CASE1(Sign::ZERO, Sign::ZERO, Sign::POS) {
        return boost::none;
    }

    CASE1(Sign::ZERO, Sign::POS, Sign::NEG) {
        const auto c_inter = intersection(constraint, c);
        return RationalLineSegment{{0, 0}, b, c_inter, c};
    }

    CASE1(Sign::ZERO, Sign::POS, Sign::ZERO) {
        return boost::none;
    }

    CASE1(Sign::ZERO, Sign::POS, Sign::POS) {
        return boost::none;
    }

    CASE1(Sign::POS, Sign::NEG, Sign::NEG) {
        const auto a_inter = intersection(constraint, a);
        const auto b_inter = intersection(constraint, b);
        return RationalLineSegment{a_inter, a, b_inter, b};
    }

    CASE1(Sign::POS, Sign::NEG, Sign::ZERO) {
        const auto b_inter = intersection(constraint, b);
        return RationalLineSegment{{0, 2}, a, b_inter, b};
    }

    CASE1(Sign::POS, Sign::NEG, Sign::POS) {
        const auto b_inter = intersection(constraint, b);
        const auto c_inter = intersection(constraint, c);
        return RationalLineSegment{b_inter, b, c_inter, c};
    }

    CASE1(Sign::POS, Sign::ZERO, Sign::NEG) {
        const auto a_inter = intersection(constraint, a);
        return RationalLineSegment{a_inter, a, {2, 0}, b};
    }

    CASE1(Sign::POS, Sign::ZERO, Sign::ZERO) {
        return boost::none;
    }

    CASE1(Sign::POS, Sign::ZERO, Sign::POS) {
        return boost::none;
    }

    CASE1(Sign::POS, Sign::POS, Sign::NEG) {
        const auto a_inter = intersection(constraint, a);
        const auto c_inter = intersection(constraint, c);
        return RationalLineSegment{a_inter, a, c_inter, c};
    }

    CASE1(Sign::POS, Sign::POS, Sign::ZERO) {
        return boost::none;
    }

    CASE1(Sign::POS, Sign::POS, Sign::POS) {
        return boost::none;
    }

    std::ostringstream err;
    err << "unknown signs "
        << static_cast<size_t>(zero_zero_sign) << ", "
        << static_cast<size_t>(pi_zero_sign) << ", "
        << static_cast<size_t>(zero_pi_sign) << " in initial_line_segment";
    throw std::runtime_error(err.str());

#if 0
    switch (zero_zero_sign) {
    case Sign::NEG:
        switch (pi_zero_sign) {
        case Sign::NEG:
            switch (zero_pi_sign) {
            case Sign::NEG:
                return boost::none;
            case Sign::ZERO:
                return boost::none;
            case Sign::POS:
                const auto a_inter = intersection(constraint, a);
                const auto c_inter = intersection(constraint, c);
                return RationalLineSegment{a_inter, a, c_inter, c};
            }
        case Sign::ZERO:
            switch (zero_pi_sign) {
            case Sign::NEG:
                return boost::none;
            case Sign::ZERO:
                return boost::none;
            case Sign::POS:
                const auto a_inter = intersection(constraint, a);
                return RationalLineSegment{a_inter, a, {2, 0}, c};
            }
        case Sign::POS:
            switch (zero_pi_sign) {
            case Sign::NEG: {
                const auto b_inter = intersection(constraint, b);
                const auto c_inter = intersection(constraint, c);
                return RationalLineSegment{b_inter, b, c_inter, c};
            }
            case Sign::ZERO: {
                const auto b_inter = intersection(constraint, b);
                return RationalLineSegment{{0, 2}, a, b_inter, b};
            }
            case Sign::POS: {
                const auto a_inter = intersection(constraint, a);
                const auto b_inter = intersection(constraint, b);
                return RationalLineSegment{a_inter, a, b_inter, b};
            }
            }
        }
    case Sign::ZERO:
        switch (pi_zero_sign) {
        case Sign::NEG:
            switch (zero_pi_sign) {
            case Sign::NEG:
                return boost::none;
            case Sign::ZERO:
                return boost::none;
            case Sign::POS:
                const auto c_inter = intersection(constraint, c);
                return RationalLineSegment{{0, 0}, b, c_inter, c};
            }
        case Sign::ZERO:
            switch (zero_pi_sign) {
            case Sign::NEG:
                return boost::none;
            case Sign::ZERO:
                // Mathematically impossible
                throw std::runtime_error("zero-zero-zero case in initial_line_segment");
            case Sign::POS:
                return boost::none;
            }
        case Sign::POS:
            switch (zero_pi_sign) {
            case Sign::NEG: {
                const auto c_inter = intersection(constraint, c);
                return RationalLineSegment{{0, 0}, b, c_inter, c};
            }
            case Sign::ZERO:
                return boost::none;
            case Sign::POS:
                return boost::none;
            }
        }
    case Sign::POS:
        switch (pi_zero_sign) {
        case Sign::NEG:
            switch (zero_pi_sign) {
            case Sign::NEG: {
                const auto a_inter = intersection(constraint, a);
                const auto b_inter = intersection(constraint, b);
                return RationalLineSegment{a_inter, a, b_inter, b};
            }
            case Sign::ZERO: {
                const auto b_inter = intersection(constraint, b);
                return RationalLineSegment{{0, 2}, a, b_inter, b};
            }
            case Sign::POS: {
                const auto b_inter = intersection(constraint, b);
                const auto c_inter = intersection(constraint, c);
                return RationalLineSegment{b_inter, b, c_inter, c};
            }
            }
        case Sign::ZERO:
            switch (zero_pi_sign) {
            case Sign::NEG: {
                const auto a_inter = intersection(constraint, a);
                return RationalLineSegment{a_inter, a, {2, 0}, b};
            }
            case Sign::ZERO:
                return boost::none;
            case Sign::POS:
                return boost::none;
            }
        case Sign::POS:
            switch (zero_pi_sign) {
            case Sign::NEG: {
                const auto a_inter = intersection(constraint, a);
                const auto c_inter = intersection(constraint, c);
                return RationalLineSegment{a_inter, a, c_inter, c};
            }
            case Sign::ZERO:
                return boost::none;
            case Sign::POS:
                return boost::none;
            }
        }
    }
#endif
}

#define CASE2(x, y) if (sign0 == x && sign1 == y)

static boost::optional<RationalLineSegment> refine_line_segment(const LinComArrZ<XYEta>& line, const RationalLineSegment& line_segment, const LinComArrZ<XYEta>& constraint) {

    const auto& point0 = line_segment.point0;
    const auto& point1 = line_segment.point1;

    const auto sign0 = line_sign_at_point(line, point0);
    const auto sign1 = line_sign_at_point(line, point1);

    const auto& line0 = line_segment.line0;
    const auto& line1 = line_segment.line1;

    CASE2(Sign::NEG, Sign::NEG) {
        return boost::none;
    }

    CASE2(Sign::NEG, Sign::ZERO) {
        return boost::none;
    }

    CASE2(Sign::NEG, Sign::POS) {
        const auto inter = intersection(constraint, line);
        return RationalLineSegment{inter, line, point1, line1};
    }

    CASE2(Sign::ZERO, Sign::NEG) {
        return boost::none;
    }

    CASE2(Sign::ZERO, Sign::ZERO) {
        // 1 2 3 2
        // This is an empty region
        return boost::none;
    }

    CASE2(Sign::ZERO, Sign::POS) {
        // we could use line or line0, keep the boundary line for
        // simplicity
        //return RationalLineSegment{point0, line0, point1, line1};
        return line_segment;
    }

    CASE2(Sign::POS, Sign::NEG) {
        const auto inter = intersection(constraint, line);
        return RationalLineSegment{point0, line0, inter, line};
    }

    CASE2(Sign::POS, Sign::ZERO) {
        // could use line or line1, use line1 for simplicity
        //return {{{{point0, line0}, {point1, line1}}}};
        return line_segment;
    }

    CASE2(Sign::POS, Sign::POS) {
        return line_segment;
    }

    std::ostringstream err;
    err << "unknown signs "
        << static_cast<size_t>(sign0) << ", "
        << static_cast<size_t>(sign1) << "in refine_line_segment rational";
    throw std::runtime_error(err.str());

#if 0
    switch (sign0) {
    case Sign::NEG:
        switch (sign1) {
        case Sign::NEG:
            return boost::none;
        case Sign::ZERO:
            return boost::none;
        case Sign::POS:
            const auto inter = intersection(constraint, line);
            return RationalLineSegment{inter, line, point1, line1};
        }
    case Sign::ZERO:
        switch (sign1) {
        case Sign::NEG:
            return boost::none;
        case Sign::ZERO:
            // 1 2 3 2
            // This is an empty region
            return boost::none;
        case Sign::POS:
            // we could use line or line0, keep the boundary line for
            // simplicity
            //return RationalLineSegment{point0, line0, point1, line1};
            return line_segment;
        }
    case Sign::POS:
        switch (sign1) {
        case Sign::ZERO:
            // could use line or line1, use line1 for simplicity
            //return {{{{point0, line0}, {point1, line1}}}};
            return line_segment;
        case Sign::POS:
            return line_segment;
        case Sign::NEG:
            // Because switch statements are stupid in C++, this one needs to go at the end because it
            // declares a variable
            const auto inter = intersection(constraint, line);
            return RationalLineSegment{point0, line0, inter, line};
        }
    }
#endif
}

boost::optional<RationalLineSegment> calculate_bounding_line_segment(const std::vector<CodeNumber>& code_numbers, const std::vector<XYZ>& code_angles, const LinComArrZ<XYEta>& constraint) {

    const auto inequalities = calculate_bounding_inequalities(code_numbers, code_angles);

    auto line_segment = initial_line_segment(constraint);

    if (!line_segment) {
        return boost::none;
    }

    for (const auto& line : inequalities) {
        line_segment = refine_line_segment(line, *line_segment, constraint);
        if (!line_segment) {
            return boost::none;
        }
    }

    return line_segment;
}

static boost::optional<RationalPolygon> refine_polygon(const RationalPolygon& polygon, const LinComArrZ<XYEta>& line) {

    const auto signs = [&]() {
        std::vector<Sign> vec;
        for (const auto& rat_pair : polygon) {
            const auto sign = line_sign_at_point(line, rat_pair.point);
            vec.push_back(sign);
        }

        return vec;
    }();

    RationalPolygon new_polygon;

    const size_t size = polygon.size();

    // TODO can we avoid the bounds checking here?
    for (size_t i = 0; i < size; i += 1) {
        const auto& point = polygon.at(i).point;
        const auto& side_line = polygon.at(i).side_line;

        const auto sign0 = signs.at(i);
        const auto sign1 = signs.at((i + 1) % size);

        if (sign0 == Sign::NEG && sign1 == Sign::NEG) {
            continue;
        } else if (sign0 == Sign::NEG && sign1 == Sign::ZERO) {
            continue;
        } else if (sign0 == Sign::NEG && sign1 == Sign::POS) {
            const auto inter = intersection(side_line, line);
            new_polygon.emplace_back(inter, side_line);

        } else if (sign0 == Sign::ZERO && sign1 == Sign::NEG) {
            new_polygon.emplace_back(point, line);
        } else if (sign0 == Sign::ZERO && sign1 == Sign::ZERO) {
            new_polygon.emplace_back(point, side_line);
        } else if (sign0 == Sign::ZERO && sign1 == Sign::POS) {
            new_polygon.emplace_back(point, side_line);

        } else if (sign0 == Sign::POS && sign1 == Sign::NEG) {
            new_polygon.emplace_back(point, side_line);
            const auto inter = intersection(side_line, line);
            new_polygon.emplace_back(inter, line);
        } else if (sign0 == Sign::POS && sign1 == Sign::ZERO) {
            new_polygon.emplace_back(point, side_line);
        } else if (sign0 == Sign::POS && sign1 == Sign::POS) {
            new_polygon.emplace_back(point, side_line);

        } else {
            std::ostringstream err;
            err << "unknown signs " << static_cast<size_t>(sign0) << ", "
                << static_cast<size_t>(sign1) << " in refine_polygon rational";
            throw std::runtime_error(err.str());
        }

#if 0
        // Should be 12 breaks
        switch (sign0) {
        case Sign::NEG:
            switch (sign1) {
            case Sign::NEG:
                break;
            case Sign::ZERO:
                break;
            case Sign::POS:
                const auto inter = intersection(side_line, line);
                new_polygon.emplace_back(inter, side_line);
                break;
            }
            break;
        case Sign::ZERO:
            switch (sign1) {
            case Sign::NEG:
                new_polygon.emplace_back(point, line);
                break;
            case Sign::ZERO:
                new_polygon.emplace_back(point, side_line);
                break;
            case Sign::POS:
                new_polygon.emplace_back(point, side_line);
                break;
            }
            break;
        case Sign::POS:
            new_polygon.emplace_back(point, side_line);
            switch (sign1) {
            case Sign::ZERO:
                break;
            case Sign::POS:
                break;
            case Sign::NEG:
                const auto inter = intersection(side_line, line);
                new_polygon.emplace_back(inter, line);
                break;
            }
            break;
        }
#endif
    }

    // This polygon must be an open set, so polyons with just two points are
    // illegal (since those would just be a straight line).

    // Reduced to empty polygon
    if (new_polygon.size() < 3) {
        return boost::none;
    }

    return new_polygon;
}

boost::optional<RationalPolygon> calculate_bounding_polygon(const std::vector<CodeNumber>& code_numbers, const std::vector<XYZ>& code_angles) {

    // each inequality is > 0
    const auto inequalities = calculate_bounding_inequalities(code_numbers, code_angles);

    const LinComArrZ<XYEta> a{1, 0, 0};   // x > 0
    const LinComArrZ<XYEta> b{0, 1, 0};   // y > 0
    const LinComArrZ<XYEta> c{-1, -1, 2}; // z = - x - y + 2*eta > 0

    // The initial bounding polygon
    // The order of these points is VERY important.
    // It determines if you iterate around the polygon in a CW or CCW
    // direction when you refine it. This in turn affects how you rotate
    // the gradient vectors when checking the zero corner case
    // Currently, we are going around CCW.
    RationalPolygon polygon = {
        RationalPair{{0, 0}, b},
        RationalPair{{2, 0}, c},
        RationalPair{{0, 2}, a}};

    for (const auto& line : inequalities) {

        const auto new_polygon = refine_polygon(polygon, line);

        // If the polygon is empty, return none
        if (!new_polygon) {
            return boost::none;
        }

        polygon = *new_polygon;
    }

    return polygon;
}
