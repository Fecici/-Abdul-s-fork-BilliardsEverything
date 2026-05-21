#include "refine.hpp"
#include "evalf.hpp"
#include "general.hpp"
#include "gradient.hpp"
#include "intersection.hpp"
#include "linear_derivative.hpp"

// It seems our assumption is broken here. The gradient of the curve
// and the gradient of one of the side equations are parallel.
// In that case, what do we do? I think we will skip the equation
// and come back to it later. The hopefull we won't have this problem
// then.
// 1 1 2 1 1 4 1 1 2 1 1 7 1 1 10 1 1 6 1 1 10 1 1 7

// The issue is not having multiple curves in an equation
// The problem is having multiple curves in an equation that intersect
// at a point
// Lines almost certainly intersect with stuff, but how do we get
// rid of them?

// 1 1 1 2 2 produces a 0 equation. That is, the equation is just 0.

// Now this one fails though. This happens because the epsilon is too small. Drat.
// Gah! I hate this one.
// 1 1 3 1 2 1 6

// Empty (correctly), but not in FindFormula-Everything MRR
// 1 2 1 4 1 2 1 4 1 2 1 6
// 1 1 1 1 2 1 4 1 2 1 1 1 1 4 1 1 1 1 2 1 4 1 2 1 1 1 1 4 1 1 2 1 1 4 1 1 2 1 1 4

// Nontrivial self intersecting curves. These could be bad.
// 1 3 2 3 1 4 1 5 3 1 5 2 3 1 4 1 3 2 5 1 3 5 1 4
// Both of these can't be factored
// -sin(2*x+3*y)-2*sin(4*x+y)-2*sin(4*x+3*y)-sin(6*x+y)+sin(6*x+5*y)-sin(8*x+y)+sin(8*x+5*y)+sin(10*x+3*y)+sin(10*x+5*y)
// This one is really bad, because it has a figure 8 that intersects itself in the middle
// -sin(4*x+y)-sin(4*x+3*y)+sin(6*x+5*y)+sin(8*x+5*y)+sin(10*x+3*y)+sin(10*x+5*y)

// 1 1 2 1 2 2 1 2
// x-y
// cos(0)+cos(2y)+cos(4y)-cos(2x-4y)+cos(2x)+cos(2x+4y)
// at (pi/2, pi/2)
// the cos curve self-intersects at the point, but it has no factors
// the gradient at this point is (0, 0), and there is no way to get around it
// This is why we are going to use the linear gradient test now

// 1 1 1 1 1 1 1 1 2 1 1 2
// sin(y)-sin(3y)+2sin(2x+y)
// x-y
// at (pi/2, pi/2)
// same as above

// 2 2 4 6
// -sin(x-2y)+sin(x)-sin(3x-2y)
// 3x-4y
// at (0, 0) and (6.28319, 0.471239)
// This curve has zero gradient at (0, 0), which is correct, but
// doesn't give us the information we want. What we want is pos, which gives
// us the information we need. For the stables we used the correct_zeros function
// to deal with these cases. The new linear_derivative method should deal with
// this case.
// Substituting y = 3/4*x, we get
// sin(x/2) + sin(x) - sin(3*x/2)
// graphing y = the above equation, we see that this equation is positive as we move
// along the line in the positive direction. This is what we want, the fact that it's
// positive. Differentiating four times and evaluating at x = 0 gives us positive,
// which is what we want.

static Vector2<Real> median(const Vector2<Interval>& point) {
    Real x0 = boost::multiprecision::median(point[0]);
    Real x1 = boost::multiprecision::median(point[1]);

    return {x0, x1};
}

void print_region(const IntervalPolygon& region) {
    for (const auto& p : region) {
        std::cout << p.point << ",\t" << boost::apply_visitor(EquationPrinter{}, p.equation) << std::endl;
    }
}

void print_region(const IntervalLineSegment& line_seg) {
    std::cout << line_seg.point0 << ",\t" << boost::apply_visitor(EquationPrinter{}, line_seg.equation0) << std::endl;
    std::cout << line_seg.point1 << ",\t" << boost::apply_visitor(EquationPrinter{}, line_seg.equation1) << std::endl;
}

// Strictly speaking, we don't need the derivatives for the line segment, since we could calculate
// the intersection using bisection instead of newton's method. Perhaps later I can look into that.
// But for now, for simplicity, I will stick with Newton's method.

// TODO go through and rewrite this using gradients
template <typename T>
boost::optional<IntervalLineSegment> refine_line_segment(const IntervalLineSegment& line_segment, const T& curve, const EquationGradient<XY, LinComArrZ<XYEta>>& constraint) {

    // Deal with zero curves here, because they are an odd case.
    if (curve.is_zero()) {
        return boost::none;
    }

    auto& point0 = line_segment.point0;
    auto& point1 = line_segment.point1;

    auto sign0 = curve_sign_at_point(curve, point0);
    auto sign1 = curve_sign_at_point(curve, point1);

    auto& equation0 = line_segment.equation0;
    auto& equation1 = line_segment.equation1;

    //std::cout << curve << std::endl;
    //std::cout << point0 << std::endl;
    //std::cout << point1 << std::endl;

    //std::cout << sign0 << " " << sign1 << std::endl;
    //std::cout << std::endl;

    // What happens is that there are problems we get an endpoint that evaluates to 0.
    // If the other endpoint evaluates to neg or pos, does it actually go neg - zero - pos - zero,
    // so there is actually a root in between? Or is it simply just neg - zero? We don't know. So
    // what we do is move the zero point in slightly, and check the sign there. This is sort of like
    // checking the midpoint, but checking the midpoint could miss a root if it is close to the endpoint
    // that evaluates to zero (so we might get negative for the midpoint, when the root is the other way).
    // A heuristic way to avoid this is to just shift in the point that evaluates to zero slightly, and
    // look at the sign there.

    if (sign0 == Sign::NEG && sign1 == Sign::NEG) {
        // neg-neg
        // we assume it is negative for the rest of the line as well
        return boost::none;
    } else if (sign0 == Sign::NEG && sign1 == Sign::ZERO) {

        auto moved1_sign = linear_derivative_sign(curve, constraint.equation, point1, point0);

        if (moved1_sign == Sign::NEG) {
            // neg-neg-zero
            return boost::none;
        } else if (moved1_sign == Sign::ZERO) {
            // neg-zero-zero
            // This is a strange situation, so give us an error if it ever
            // happens
            std::ostringstream err;
            err << "neg-zero-zero in refine_line_segment";
            throw std::runtime_error(err.str());
        } else if (moved1_sign == Sign::POS) {
            // neg-pos-zero

            // We need to be careful though that the root we find is the one in the middle,
            // not the endpoint at point1
            EquationGradient<XY, T> curve_grad{curve};
            auto inter = intersection_zero(constraint, curve_grad, median(point1), median(point0));
            //return IntervalLineSegment{inter, curve_grad, point1, equation1};
            return IntervalLineSegment{inter, curve_grad, point1, curve_grad};
        } else {
            std::ostringstream err;
            err << "unknown sign " << static_cast<size_t>(moved1_sign) << " for neg-zero in refine_line_segment";
            throw std::runtime_error(err.str());
        }
    } else if (sign0 == Sign::NEG && sign1 == Sign::POS) {
        // neg-pos
        EquationGradient<XY, T> curve_grad{curve};
        auto inter = intersection(constraint, curve_grad, median(point0), median(point1));
        return IntervalLineSegment{inter, curve_grad, point1, equation1};
    } else if (sign0 == Sign::ZERO && sign1 == Sign::NEG) {

        auto moved0_sign = linear_derivative_sign(curve, constraint.equation, point0, point1);

        if (moved0_sign == Sign::NEG) {
            // zero-neg-neg
            return boost::none;
        } else if (moved0_sign == Sign::ZERO) {
            // zero-zero-neg
            std::ostringstream err;
            err << "zero-zero-neg in refine_line_segment";
            throw std::runtime_error(err.str());
        } else if (moved0_sign == Sign::POS) {
            // zero-pos-neg

            // TODO we need to be careful though that the root we find is the one in the middle,
            // not the endpoint at point1
            EquationGradient<XY, T> curve_grad{curve};
            auto inter = intersection_zero(constraint, curve_grad, median(point0), median(point1));
            //return IntervalLineSegment{point0, equation0, inter, curve_grad};
            return IntervalLineSegment{point0, curve_grad, inter, curve_grad};
        } else {
            std::ostringstream err;
            err << "unknown sign " << static_cast<size_t>(moved0_sign) << " for zero-neg in refine_line_segment";
            throw std::runtime_error(err.str());
        }

    } else if (sign0 == Sign::ZERO && sign1 == Sign::ZERO) {

        auto moved0_sign = linear_derivative_sign(curve, constraint.equation, point0, point1);
        auto moved1_sign = linear_derivative_sign(curve, constraint.equation, point1, point0);

        if (moved0_sign == Sign::NEG && moved1_sign == Sign::NEG) {
            // We assume everything is negative acrerr the rest of the
            // line
            return boost::none;
        } else if (moved0_sign == Sign::NEG && moved1_sign == Sign::ZERO) {
            // zero-neg-zero-zero
            // Some of these cases, like this one, almost certainly indicate
            // an error in the program. Others, like the one after, do not
            // necessarily indicate errors, but I am curious to see if they
            // ever happen first.
            std::ostringstream err;
            err << "zero-neg-zero-zero in refine_line_segment";
            throw std::runtime_error(err.str());

        } else if (moved0_sign == Sign::NEG && moved1_sign == Sign::POS) {

            // strictly speaking there should be an intersection here, but I
            // want to see if this ever happens first
            std::ostringstream err;
            err << "zero-neg-pos-zero in refine_line_segment";
            throw std::runtime_error(err.str());

        } else if (moved0_sign == Sign::ZERO && moved1_sign == Sign::NEG) {

            std::ostringstream err;
            err << "zero-zero-neg-zero in refine_line_segment";
            throw std::runtime_error(err.str());

        } else if (moved0_sign == Sign::ZERO && moved1_sign == Sign::ZERO) {

            // Again, see what happens here
            std::ostringstream err;
            err << "zero-zero-zero-zero in refine_line_segment";
            std::cout << point0 << ", " << point1 << std::endl;
            std::cout << curve << std::endl;
            throw std::runtime_error(err.str());

        } else if (moved0_sign == Sign::ZERO && moved1_sign == Sign::POS) {

            std::ostringstream err;
            err << "zero-zero-pos-zero in refine_line_segment";
            throw std::runtime_error(err.str());

        } else if (moved0_sign == Sign::POS && moved1_sign == Sign::NEG) {

            // strictly speaking there should be an intersection here, but I
            // want to see if this ever happens first
            std::ostringstream err;
            err << "zero-pos-neg-zero in refine_line_segment";
            throw std::runtime_error(err.str());

        } else if (moved0_sign == Sign::POS && moved1_sign == Sign::ZERO) {

            std::ostringstream err;
            err << "zero-pos-zero-zero in refine_line_segment";
            throw std::runtime_error(err.str());

        } else if (moved0_sign == Sign::POS && moved1_sign == Sign::POS) {
            // we assume it is positive all along the rest of the line segment
            //return line_segment;
            const EquationGradient<XY, T> curve_grad{curve};
            return IntervalLineSegment{point0, curve_grad, point1, curve_grad};
        } else {
            std::ostringstream err;
            err << "signs " << static_cast<size_t>(moved0_sign) << ", " << static_cast<size_t>(moved1_sign) << " for zero-zero in refine_line_segment";
            throw std::runtime_error(err.str());
        }

    } else if (sign0 == Sign::ZERO && sign1 == Sign::POS) {

        auto moved0_sign = linear_derivative_sign(curve, constraint.equation, point0, point1);

        if (moved0_sign == Sign::NEG) {
            // zero-neg-pos

            // TODO we need to be careful though that the root we find is the one in the middle,
            // not the endpoint at point1
            EquationGradient<XY, T> curve_grad{curve};
            auto inter = intersection_zero(constraint, curve_grad, median(point0), median(point1));
            return IntervalLineSegment{inter, curve_grad, point1, equation1};
        } else if (moved0_sign == Sign::ZERO) {
            // zero-zero-pos
            std::ostringstream err;
            err << "zero-zero-pos in refine_line_segment";
            std::cout << "curve " << curve << std::endl;
            std::cout << point0 << ", " << point1 << std::endl;
            //std::cout << constraint.equation << std::endl;
            throw std::runtime_error(err.str());
        } else if (moved0_sign == Sign::POS) {
            // zero-pos-pos
            //return line_segment;
            EquationGradient<XY, T> curve_grad{curve};
            return IntervalLineSegment{point0, curve_grad, point1, equation1};
        } else {
            std::ostringstream err;
            err << "unknown sign " << static_cast<size_t>(moved0_sign) << " for zero-pos in refine_line_segment";
            throw std::runtime_error(err.str());
        }

    } else if (sign0 == Sign::POS && sign1 == Sign::NEG) {
        EquationGradient<XY, T> curve_grad{curve};
        auto inter = intersection(constraint, curve_grad, median(point0), median(point1));
        return IntervalLineSegment{point0, equation0, inter, curve_grad};
    } else if (sign0 == Sign::POS && sign1 == Sign::ZERO) {

        auto moved1_sign = linear_derivative_sign(curve, constraint.equation, point1, point0);

        if (moved1_sign == Sign::NEG) {
            // pos-neg-zero
            EquationGradient<XY, T> curve_grad{curve};
            auto inter = intersection_zero(constraint, curve_grad, median(point1), median(point0));
            return IntervalLineSegment{point0, equation0, inter, curve_grad};
        } else if (moved1_sign == Sign::ZERO) {
            // pos-zero-zero
            std::ostringstream err;
            err << "pos-zero-zero in refine_line_segment";
            throw std::runtime_error(err.str());
        } else if (moved1_sign == Sign::POS) {
            // pos-pos-zero
            //return line_segment;
            EquationGradient<XY, T> curve_grad{curve};
            return IntervalLineSegment{point0, equation0, point1, curve_grad};
        } else {
            std::ostringstream err;
            err << "unknown sign " << static_cast<size_t>(moved1_sign) << " for pos-zero in refine_line_segment";
            throw std::runtime_error(err.str());
        }

    } else if (sign0 == Sign::POS && sign1 == Sign::POS) {
        // pos-pos
        // We assume it is positive across the entire line
        return line_segment;
    } else {
        std::ostringstream err;
        err << "signs " << static_cast<size_t>(sign0) << ", " << static_cast<size_t>(sign1) << " in refine_line_segment";
        throw std::runtime_error(err.str());
    }
}

template boost::optional<IntervalLineSegment> refine_line_segment<Equation<Sin>>(const IntervalLineSegment& line_segment, const Equation<Sin>& curve, const EquationGradient<XY, LinComArrZ<XYEta>>& constraint);

template boost::optional<IntervalLineSegment> refine_line_segment<Equation<Cos>>(const IntervalLineSegment& line_segment, const Equation<Cos>& curve, const EquationGradient<XY, LinComArrZ<XYEta>>& constraint);

// We can just use end and begin, because they can return
// a constant iterator if necessary. The only difference
// is that cbegin/cend guarentee a constant iterator.
// It is better to use the std versions of begin and end
// instead of the member versions. Why? I dunno, it is
// more general and looks pretty.
// cbegin and cend are best when used with auto, eg
// const auto it = std::begin(container); // The iterator variable is const, but what about the data it points to?
// const auto it = std::cbegin(containe); // Now we have a guarantee of const data

// Normally, you can achieve pretty good memory safety in C++ by using the containers
// and avoiding C features like malloc/free, pointers, etc.
// BUT, once you start using iterators, that all goes out the window. You gotta watch
// out for iterator invalidation, writing past the end of a container, dereferencing
// end, yadayadayada. Sheesh.

// Suppose you have the case (POS, ZERO, NEG) POS
// Then there is an intersection between the zero and the pos.
// However, there is also an intersection at point0, because it
// is ZERO there. We don't want to converge to that root, not at all.
// Previously, we would use prev_moved instead of point0 to try to
// influence the root finder toward the other root and not point0,
// but in the 1 1 3 1 2 1 6 case, this didn't work. So we will
// assume the root is in the "second half" of the curve, and use
// the midpoint of point0 and point1 as the left endpoint for the
// root finder. Heuristically, it makes sense that the intersection
// would occur there, and in version 55 that is the assumpton we make
// using the old midpoint MRR, and so far that hasn't caused us issues.
// So, we will assume that this is the case. If however, it turns out not
// to be the case, the finder should converge to one of the endpoints of the
// interval, in which case our zero-curve-sign double check should be able
// to catch it. The "proper" way to do this would be to project the midpoint
// down on to the side equation, find the point of intersection, and check
// that the sign is negative there, which would ensure that we have bracketed
// the root. However, this is much more involved, and involves finding another root
// intersection
struct ZeroInfo final {

    Sign prev_sign;
    Sign next_sign;

    explicit ZeroInfo(const Sign prev_sign_, const Sign next_sign_)
        : prev_sign{prev_sign_}, next_sign{next_sign_} {}

    friend bool operator==(const ZeroInfo& lhs, const ZeroInfo& rhs) {
        return (lhs.prev_sign == rhs.prev_sign) && (lhs.next_sign == rhs.next_sign);
    }
};

struct Corner final {

    Sign corner_sign;
    boost::optional<ZeroInfo> zero_info; // if the sign is 0, we have extra information here

    // For Sign::POS and Sign::NEG
    explicit Corner(const Sign corner_sign_)
        : corner_sign{corner_sign_}, zero_info{boost::none} {}

    explicit Corner(const Sign corner_sign_, const ZeroInfo& zero_info_)
        : corner_sign{corner_sign_}, zero_info{zero_info_} {}

    friend std::ostream& operator<<(std::ostream& os, const Corner& corner) {
        if (corner.zero_info) {
            auto& info = *corner.zero_info;
            return os << '(' << info.prev_sign << ", " << corner.corner_sign << ", " << info.next_sign << ')';
        } else {
            return os << corner.corner_sign;
        }
    }
};

// The logic of this function requires mutation
static void correct_zeros(std::vector<Corner>& corners) {

    // In this case, the derivatives are equal, but the non-zero sign on the
    // other end indicates that the sign should really be non-zero.
    // In some cases, like 1 1 3 1 2 6, fudging gives us the correct sign.
    // However, in others, like 1 1 2 1 1 4 1 1 2 1 1 7 1 1 10 1 1 6 1 1 10 1 1 7,
    // fudging gives us the wrong sign. I'm not sure how to always correctly
    // find the sign, so I will just assume that the sign matches the non-zero one,
    // which so far always seems to be correct.

    auto size = corners.size();

    for (size_t i = 0; i < size; i += 1) {
        // Yikes! Be careful with mutability
        auto& corner0 = corners.at(i);
        auto& corner1 = corners.at((i + 1) % size);

        std::pair<Sign, Sign> signs = {corner0.corner_sign, corner1.corner_sign};

        // We need to change the P/N, (Z, Z, _) and (_, Z, Z), P/N

        if (signs == std::pair<Sign, Sign>{Sign::NEG, Sign::ZERO}) {
            auto& zero_info1 = *corner1.zero_info;
            if (zero_info1.prev_sign == Sign::ZERO) {
                zero_info1.prev_sign = Sign::NEG;
            }
        } else if (signs == std::pair<Sign, Sign>{Sign::POS, Sign::ZERO}) {
            auto& zero_info1 = *corner1.zero_info;
            if (zero_info1.prev_sign == Sign::ZERO) {
                zero_info1.prev_sign = Sign::POS;
            }
        } else if (signs == std::pair<Sign, Sign>{Sign::ZERO, Sign::NEG}) {
            auto& zero_info0 = *corner0.zero_info;
            if (zero_info0.next_sign == Sign::ZERO) {
                zero_info0.next_sign = Sign::NEG;
            }
        } else if (signs == std::pair<Sign, Sign>{Sign::ZERO, Sign::POS}) {
            auto& zero_info0 = *corner0.zero_info;
            if (zero_info0.next_sign == Sign::ZERO) {
                zero_info0.next_sign = Sign::POS;
            }
        }
    }
}

template <typename T>
std::vector<Corner> calculate_corners(const IntervalPolygon& polygon, const T& curve) {

    auto size = polygon.size();

    std::vector<Corner> corners;
    for (size_t i = 0; i < size; ++i) {
        auto& int_pair = polygon.at(i);
        auto curve_sign = curve_sign_at_point(curve, int_pair.point);

        //std::cout << curve << ", " << int_pair.point.coord0.str() << ", " << int_pair.point.coord1.str() << ", ";

        if (curve_sign == Sign::NEG) {
            corners.emplace_back(Sign::NEG);
        } else if (curve_sign == Sign::ZERO) {
            // (i - 1) % size won't work!
            auto& prev_int_pair = i == 0 ? polygon.at(size - 1) : polygon.at(i - 1);

            // The gradient of a function f(x, y) at any location (x, y) points in the direction
            // of greatest increase of the function (so it pulls you towards local maximums).
            // For points on the level set f(x, y) = 0, the gradient is perpendicular to the level set.
            // Now, the level set separates the positive region of the function from the negative regions,
            // but does the gradient point toward the positive or negative side? I believe it points toward
            // the positive side, since the function would be increasing in that direction. Then, we just
            // rotate the gradient accordingly and off we go.

            // TODO in theory, we only need to factor out the line that goes through the point, not
            // all of them. But we will see
            // TODO we could also cache the fully factored version too
            // Have to think about that
            // When we get a zero gradient, then there is a line going through the point. We need
            // to factor out that line to find the gradient information
            // TODO when this happens, should we remember the equation, and continue refining
            // with others hoping that the troublesome point gets cut out?
            auto prev_gradient = boost::apply_visitor(GradientVariant{int_pair.point}, prev_int_pair.equation);
            auto next_gradient = boost::apply_visitor(GradientVariant{int_pair.point}, int_pair.equation);

            Vector2<Interval> prev_dir{-prev_gradient[1], prev_gradient[0]}; // rotate 90 CCW
            Vector2<Interval> next_dir{next_gradient[1], -next_gradient[0]}; // rotate 90 CW

            EquationGradient<XY, T> eq_grad{curve};
            auto curve_gradient = gradient(eq_grad, int_pair.point);

            auto prev_dot = prev_dir.dot(curve_gradient);
            auto next_dot = next_dir.dot(curve_gradient);

            auto prev_sign = sign(prev_dot);
            auto next_sign = sign(next_dot);

            corners.emplace_back(Sign::ZERO, ZeroInfo{prev_sign, next_sign});

        } else if (curve_sign == Sign::POS) {
            corners.emplace_back(Sign::POS);
        } else {
            throw std::runtime_error(invalid_enum_value("Sign", curve_sign));
        }
    }

    correct_zeros(corners);

    return corners;
}

// Each pair is a point, and the equation connecting it to the next point in the vector
// We assume the curve intersects the interior of each side_equation curve segment at
// most once
template <typename T>
boost::optional<IntervalPolygon> refine_polygon(const IntervalPolygon& polygon, const T& curve) {

    // Deal with zero curves right here, since they are rather odd and could mess up
    // the assumptions of the refining algorithm.
    if (curve.is_zero()) {
        return boost::none;
    }

    auto corners = calculate_corners(polygon, curve);
    //std::cout << corners << std::endl;

    IntervalPolygon new_polygon{};

    size_t size = polygon.size();

    for (size_t i = 0; i < size; i += 1) {
        auto& point0 = polygon.at(i).point;
        auto& point1 = polygon.at((i + 1) % size).point;

        auto& side_equation = polygon.at(i).equation;

        auto& corner0 = corners.at(i);
        auto& corner1 = corners.at((i + 1) % size);

        //std::cout << corner0 << ", " << corner1 << std::endl;
        //std::cout << point0 << ", " << point1 << std::endl;
        //std::cout << std::endl;

        std::pair<Sign, Sign> signs = {corner0.corner_sign, corner1.corner_sign};

        // The nice thing with this technique is that it doesn't matter if there are
        // spurious lines in the curve. It works anyway!
        if (signs == std::pair<Sign, Sign>{Sign::NEG, Sign::NEG}) {
            continue;
        } else if (signs == std::pair<Sign, Sign>{Sign::NEG, Sign::ZERO}) {

            auto& zero_info1 = *corner1.zero_info;

            if (zero_info1 == ZeroInfo{Sign::NEG, Sign::NEG}) {
                continue;
            } else if (zero_info1 == ZeroInfo{Sign::NEG, Sign::ZERO}) {
                // 1 1 1 1 2 1 4 1 2 1 1 1 1 4 1 1 1 1 2 1 4 1 2 1 1 1 1 4 1 1 2 1 1 4 1 1 2 1 1 4
                continue;
            } else if (zero_info1 == ZeroInfo{Sign::NEG, Sign::POS}) {
                continue;
            } else if (zero_info1 == ZeroInfo{Sign::POS, Sign::POS}) {
                // 1 1 1 1 2 1 3 1 1 4
                EquationGradient<XY, T> curve_grad{curve};
                auto inter = boost::apply_visitor(IntersectionZeroVariant<XY, T>{curve_grad, median(point1), median(point0)}, side_equation);
                new_polygon.emplace_back(inter, side_equation);
            } else {
                std::ostringstream err;
                err << corner0 << ' ' << corner1 << " in refine_polygon";
                std::cout << curve << std::endl;
                std::cout << boost::apply_visitor(EquationPrinter{}, side_equation) << std::endl;
                std::cout << point0 << std::endl;
                std::cout << point1 << std::endl;
                throw std::runtime_error(err.str());
            }

        } else if (signs == std::pair<Sign, Sign>{Sign::NEG, Sign::POS}) {
            EquationGradient<XY, T> curve_grad{curve};
            auto inter = boost::apply_visitor(IntersectionVariant<XY, T>{curve_grad, median(point0), median(point1)}, side_equation);
            new_polygon.emplace_back(inter, side_equation);

        } else if (signs == std::pair<Sign, Sign>{Sign::ZERO, Sign::NEG}) {

            auto& zero_info0 = *corner0.zero_info;

            if (zero_info0 == ZeroInfo{Sign::NEG, Sign::NEG}) {
                continue;
            } else if (zero_info0 == ZeroInfo{Sign::ZERO, Sign::NEG}) {
                // 1 1 1 1 2 1 4 1 2 1 1 1 1 4 1 1 1 1 2 1 4 1 2 1 1 1 1 4 1 1 2 1 1 4 1 1 2 1 1 4
                continue;
            } else if (zero_info0 == ZeroInfo{Sign::POS, Sign::NEG}) {
                EquationGradient<XY, T> curve_grad{curve};
                new_polygon.emplace_back(point0, curve_grad);
            } else if (zero_info0 == ZeroInfo{Sign::POS, Sign::POS}) {
                // 1 1 2 1 1 5 1 1 8
                EquationGradient<XY, T> curve_grad{curve};
                auto inter = boost::apply_visitor(IntersectionZeroVariant<XY, T>{curve_grad, median(point0), median(point1)}, side_equation);
                new_polygon.emplace_back(point0, side_equation);
                new_polygon.emplace_back(inter, curve_grad);
            } else {
                std::ostringstream err;
                err << corner0 << ' ' << corner1 << " in refine_polygon";
                throw std::runtime_error(err.str());
            }

        } else if (signs == std::pair<Sign, Sign>{Sign::ZERO, Sign::ZERO}) {

            auto& zero_info0 = *corner0.zero_info;
            auto& zero_info1 = *corner1.zero_info;
            std::array<Sign, 4> zero_signs = {{zero_info0.prev_sign, zero_info0.next_sign,
                                                     zero_info1.prev_sign, zero_info1.next_sign}};

            if (zero_signs == std::array<Sign, 4>{{Sign::NEG, Sign::NEG, Sign::NEG, Sign::NEG}}) {
                // 1 1 1 1 2 1 6
                continue;
            } else if (zero_signs == std::array<Sign, 4>{{Sign::NEG, Sign::NEG, Sign::NEG, Sign::POS}}) {
                // 1 1 2 1 1 5 1 1 8
                continue;
            } else if (zero_signs == std::array<Sign, 4>{{Sign::NEG, Sign::ZERO, Sign::ZERO, Sign::NEG}}) {
                // 1 1 1 1 2 1 4 1 2 1 1 1 1 4 1 1 1 1 2 1 4 1 2 1 1 1 1 4 1 1 2 1 1 4 1 1 2 1 1 4
                continue;
            } else if (zero_signs == std::array<Sign, 4>{{Sign::NEG, Sign::POS, Sign::POS, Sign::POS}}) {
                // 1 1 2 1 1 4 1 1 2 1 1 7 1 1 10 1 1 6 1 1 10 1 1 7
                new_polygon.emplace_back(point0, side_equation);

            } else if (zero_signs == std::array<Sign, 4>{{Sign::ZERO, Sign::POS, Sign::POS, Sign::POS}}) {
                // TODO put this in
                new_polygon.emplace_back(point0, side_equation);

            } else if (zero_signs == std::array<Sign, 4>{{Sign::POS, Sign::NEG, Sign::NEG, Sign::NEG}}) {
                // 1 1 1 1 2 1 1 2 1 1 2 1 2 2 2
                EquationGradient<XY, T> curve_grad{curve};
                new_polygon.emplace_back(point0, curve_grad);
            } else if (zero_signs == std::array<Sign, 4>{{Sign::POS, Sign::NEG, Sign::NEG, Sign::POS}}) {
                new_polygon.emplace_back(point0, EquationGradient<XY, T>{curve});
            } else if (zero_signs == std::array<Sign, 4>{{Sign::POS, Sign::ZERO, Sign::ZERO, Sign::POS}}) {
                // TODO look at curve vs. side_equation
                // 1 1 1
                EquationGradient<XY, T> curve_grad{curve};
                new_polygon.emplace_back(point0, curve_grad);
            } else if (zero_signs == std::array<Sign, 4>{{Sign::POS, Sign::POS, Sign::POS, Sign::NEG}}) {
                new_polygon.emplace_back(point0, side_equation);
            } else if (zero_signs == std::array<Sign, 4>{{Sign::POS, Sign::POS, Sign::POS, Sign::POS}}) {
                // 1 1 1 1 2 1 1 2 1 1 2 1 2 2 2
                new_polygon.emplace_back(point0, side_equation);
            } else {
                std::cout << "curve: " << curve << std::endl;
                std::cout << "side_equation: " << boost::apply_visitor(EquationPrinter{}, side_equation) << std::endl;
                std::cout << point0 << std::endl;
                std::cout << point1 << std::endl;

                std::ostringstream err{};
                err << corner0 << ' ' << corner1 << " in refine_polygon";
                throw std::runtime_error(err.str());
            }

        } else if (signs == std::pair<Sign, Sign>{Sign::ZERO, Sign::POS}) {

            auto& zero_info0 = *corner0.zero_info;

            if (zero_info0 == ZeroInfo{Sign::NEG, Sign::NEG}) {
                // 1 1 3 1 2 1 6
                EquationGradient<XY, T> curve_grad{curve};
                auto inter = boost::apply_visitor(IntersectionZeroVariant<XY, T>{curve_grad, median(point0), median(point1)}, side_equation);
                new_polygon.emplace_back(inter, side_equation);
            } else if (zero_info0 == ZeroInfo{Sign::NEG, Sign::POS}) {
                new_polygon.emplace_back(point0, side_equation);
            } else if (zero_info0 == ZeroInfo{Sign::ZERO, Sign::POS}) {
                // 1 1 1
                new_polygon.emplace_back(point0, side_equation);
            } else if (zero_info0 == ZeroInfo{Sign::POS, Sign::NEG}) {
                // 1 1 2 1 1 5 1 1 8
                EquationGradient<XY, T> curve_grad{curve};
                auto inter = boost::apply_visitor(IntersectionZeroVariant<XY, T>{curve_grad, median(point0), median(point1)}, side_equation);
                new_polygon.emplace_back(point0, curve_grad);
                new_polygon.emplace_back(inter, side_equation);
            } else if (zero_info0 == ZeroInfo{Sign::POS, Sign::POS}) {
                // 1 3 3
                new_polygon.emplace_back(point0, side_equation);
            } else {
                std::ostringstream err;
                err << corner0 << ' ' << corner1 << " in refine_polygon";
                std::cout << curve << std::endl;
                std::cout << boost::apply_visitor(EquationPrinter{}, side_equation) << std::endl;
                std::cout << point0 << std::endl;
                std::cout << point1 << std::endl;
                throw std::runtime_error(err.str());
            }

        } else if (signs == std::pair<Sign, Sign>{Sign::POS, Sign::NEG}) {
            EquationGradient<XY, T> curve_grad{curve};
            auto inter = boost::apply_visitor(IntersectionVariant<XY, T>{curve_grad, median(point0), median(point1)}, side_equation);
            new_polygon.emplace_back(point0, side_equation);
            new_polygon.emplace_back(inter, curve_grad);
        } else if (signs == std::pair<Sign, Sign>{Sign::POS, Sign::ZERO}) {

            auto& zero_info1 = *corner1.zero_info;

            if (zero_info1 == ZeroInfo{Sign::NEG, Sign::NEG}) {
                // 1 1 1 1 2 1 3 1 1 4 1 2 2 2 1 4 1 1 3 1 2 1 1 1 1 3 1 1 4 1 1 2 1 1 4 1 1 3
                EquationGradient<XY, T> curve_grad{curve};
                auto inter = boost::apply_visitor(IntersectionZeroVariant<XY, T>{curve_grad, median(point1), median(point0)}, side_equation);
                new_polygon.emplace_back(point0, side_equation);
                new_polygon.emplace_back(inter, curve_grad);
            } else if (zero_info1 == ZeroInfo{Sign::NEG, Sign::POS}) {
                // 1 1 1 1 2 1 3 1 1 4
                EquationGradient<XY, T> curve_grad{curve};
                auto inter = boost::apply_visitor(IntersectionZeroVariant<XY, T>{curve_grad, median(point1), median(point0)}, side_equation);
                new_polygon.emplace_back(point0, side_equation);
                new_polygon.emplace_back(inter, curve_grad);
            } else if (zero_info1 == ZeroInfo{Sign::POS, Sign::NEG}) {
                new_polygon.emplace_back(point0, side_equation);
            } else if (zero_info1 == ZeroInfo{Sign::POS, Sign::ZERO}) {
                // 1 1 1
                new_polygon.emplace_back(point0, side_equation);
            } else if (zero_info1 == ZeroInfo{Sign::POS, Sign::POS}) {
                new_polygon.emplace_back(point0, side_equation);
            } else {
                std::ostringstream err;
                err << corner0 << ' ' << corner1 << " in refine_polygon";
                std::cout << curve << std::endl;
                std::cout << boost::apply_visitor(EquationPrinter{}, side_equation) << std::endl;
                std::cout << point0 << std::endl;
                std::cout << point1 << std::endl;
                throw std::runtime_error(err.str());
            }

        } else if (signs == std::pair<Sign, Sign>{Sign::POS, Sign::POS}) {
            // We assume it is positive across the entire line
            new_polygon.emplace_back(point0, side_equation);
        } else {
            std::ostringstream err;
            err << corner0 << ' ' << corner1 << " in refine_polygon";
            throw std::runtime_error(err.str());
        }
    }

    // TODO have examples where it is 0 and 1
    if (new_polygon.size() < 2) {
        // Reduced to empty polygon
        return boost::none;
    } else if (new_polygon.size() == 2) {
        throw std::runtime_error("size 2 MRR polygon");
    } else {
        return new_polygon;
    }
}

template boost::optional<IntervalPolygon> refine_polygon<Equation<Sin>>(const IntervalPolygon& polygon, const Equation<Sin>& curve);
template boost::optional<IntervalPolygon> refine_polygon<Equation<Cos>>(const IntervalPolygon& polygon, const Equation<Cos>& curve);
