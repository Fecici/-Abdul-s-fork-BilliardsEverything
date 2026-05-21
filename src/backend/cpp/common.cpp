#include <tbb/parallel_invoke.h>

#include "common.hpp"
#include "division.hpp"
// This file should probably be split in two

// We are going to have two versions right now. I can template them later
static void remove_factor(CodeInfo& info, const Equation<Sin>& factor, const bool pos) {

    // for Sin
    // Cos
    // Sin
    //
    // for Cos
    // Sin
    // Cos


    // DEBUG
    //std::cout << "remove_factor(sin, bool)" << std::endl;

    std::vector<std::pair<Equation<Sin>, Equation<Cos>>> sines{};
    std::vector<std::pair<Equation<Cos>, Equation<Sin>>> cosines{};

    // Find the ones we should divide out and their factored versions

    for (const auto& eq : info.sin_equations) {
        const auto opt = divide_once(eq, factor);
        if (opt) {
            auto factored = *opt;

            if (!pos) {
                // this is a negative stable info, so we need to negate the factor
                factored.scale(-1);
            }

            sines.emplace_back(eq, factored);
        }
    }

    for (const auto& eq : info.cos_equations) {
        const auto opt = divide_once(eq, factor);

        if (opt) {
            auto factored = *opt;

            if (!pos) {
                factored.scale(-1);
            }

            cosines.emplace_back(eq, factored);
        }
    }

    // DEBUG
/*
    for (const auto& p : sines) {
        std::cout << p.first << " , " << p.second << std::endl;
    }
    for (const auto& p : cosines) {
       std::cout << p.first << " , " << p.second << std::endl;
    }
ß */


    // Now remove the equations that we found and insert the factored ones
    for (const auto& p : sines) {
        info.sin_equations.erase(p.first);
        info.cos_equations.insert(p.second);
    }

    for (const auto& p : cosines) {
        info.cos_equations.erase(p.first);
        info.sin_equations.insert(p.second);
    }
}

static bool remove_factor_duplicate_stables(CodeInfo& info, const Equation<Sin>& factor, const bool pos, const bool show) {
    bool hasEquation = false;
    // for Sin
    // Cos
    // Sin
    //
    // for Cos
    // Sin
    // Cos
    if(show){
        std::cout << "Factor: " << factor << std::endl;
    }
    std::vector<std::pair<Equation<Sin>, Equation<Cos>>> sines{};
    std::vector<std::pair<Equation<Cos>, Equation<Sin>>> cosines{};

    // Find the ones we should divide out and their factored versions

    for (const auto& eq : info.sin_equations) {
        const auto opt = divide_once(eq, factor);
        if (opt) {
            auto factored = *opt;

            if (!pos) {
                // this is a negative stable info, so we need to negate the factor
                factored.scale(-1);
            }

            sines.emplace_back(eq, factored);
        }
    }

    for (const auto& eq : info.cos_equations) {
        const auto opt = divide_once(eq, factor);

        if (opt) {
            auto factored = *opt;

            if (!pos) {
                factored.scale(-1);
            }

            cosines.emplace_back(eq, factored);
        }
    }

    // label_remove_factor duplicate stables

    for (const auto& p : sines) {
        hasEquation = true;
        if (show){
            std::cout << "removing " << p.first << std::endl;
            std::cout << "leaving  " << p.second << std::endl;
        }
    }
    for (const auto& p : cosines) {
       hasEquation = true;
       if (show){
            std::cout << "removing " << p.first << std::endl;
            std::cout << "leaving  " << p.second << std::endl;
       }
    }


    // Now remove the equations that we found and insert the factored ones
    for (const auto& p : sines) {
        info.sin_equations.erase(p.first);
        info.cos_equations.insert(p.second);
    }

    for (const auto& p : cosines) {
        info.cos_equations.erase(p.first);
        info.sin_equations.insert(p.second);
    }
    return hasEquation;
}

static bool remove_factor_half_duplicate_stables(CodeInfo& info, const Equation<Sin>& factor, const bool pos) {
    bool hasEquation = false;
    // for Sin
    // Cos
    // Sin
    //
    // for Cos
    // Sin
    // Cos
    std::cout << "Factor: " << factor << std::endl;
    std::vector<std::pair<Equation<Sin>, Equation<Cos>>> sines{};
    std::vector<std::pair<Equation<Cos>, Equation<Sin>>> cosines{};

    // Find the ones we should divide out and their factored versions

    for (const auto& eq : info.sin_equations) {
        const auto opt = divide_once(eq, factor);
        if (opt) {
            auto factored = *opt;

            if (!pos) {
                // this is a negative stable info, so we need to negate the factor
                factored.scale(-1);
            }

            sines.emplace_back(eq, factored);
        }
    }

    for (const auto& eq : info.cos_equations) {
        const auto opt = divide_once(eq, factor);

        if (opt) {
            auto factored = *opt;

            if (!pos) {
                factored.scale(-1);
            }

            cosines.emplace_back(eq, factored);
        }
    }

    // label_remove_factor half

//    for (const auto& p : sines) {
//        std::cout << p.first << " , " << p.second << std::endl;
//    }
//    for (const auto& p : cosines) {
//       std::cout << p.first << " , " << p.second << std::endl;
//    }


    // Now remove the equations that we found and insert the factored ones
    for (const auto& p : sines) {
        info.sin_equations.erase(p.first);
        info.cos_equations.insert(p.second);//george sept15,2021 uncommented this 1 of 4
        std::cout << "removing " << p.first << std::endl;
        std::cout << "leaving  " << p.second << std::endl;
        hasEquation = true;
    }

    for (const auto& p : cosines) {
        info.cos_equations.erase(p.first);
        info.sin_equations.insert(p.second);//george sept15,2021 uncommented this 2 of 4
        std::cout << "removing " << p.first << std::endl;
        std::cout << "leaving  " << p.second << std::endl;
        hasEquation = true;
    }

    return hasEquation;

}


// We are going to have two versions right now. I can template them later
static void remove_factor(CodeInfo& info, const Equation<Cos>& factor, const bool pos) {

    // for Sin
    // Cos
    // Sin
    //
    // for Cos
    // Sin
    // Cos
    // DEBUG
    //std::cout << "remove_factor(cos, bool)" << std::endl;

    std::vector<std::pair<Equation<Sin>, Equation<Sin>>> sines{};
    std::vector<std::pair<Equation<Cos>, Equation<Cos>>> cosines{};

    // Find the ones we should divide out and their factored versions

    for (const auto& eq : info.sin_equations) {
        const auto opt = divide_once(eq, factor);
        if (opt) {
            auto factored = *opt;

            if (!pos) {
                // this is a negative stable info, so we need to negate the factor
                factored.scale(-1);
            }

            sines.emplace_back(eq, factored);
        }
    }

    for (const auto& eq : info.cos_equations) {
        const auto opt = divide_once(eq, factor);

        if (opt) {
            auto factored = *opt;

            if (!pos) {
                factored.scale(-1);
            }

            cosines.emplace_back(eq, factored);
        }
    }

    // DEBUG
  /*
    for (const auto& p : sines) {
            std::cout << p.first << " , " << p.second << std::endl;
        }
        for (const auto& p : cosines) {
           std::cout << p.first << " , " << p.second << std::endl;
        }
 */

    // Now remove the equations that we found and insert the factored ones
    for (const auto& p : sines) {
        info.sin_equations.erase(p.first);
        info.sin_equations.insert(p.second);
    }

    for (const auto& p : cosines) {
        info.cos_equations.erase(p.first);
        info.cos_equations.insert(p.second);
    }
}

void remove_factor(CodeInfo& stable_neg_info, const CodePair& unstable, CodeInfo& stable_pos_info) {

    const auto constraint = unstable.sequence.constraint(unstable.angles.first, unstable.angles.second);

    const auto x_coeff = constraint.coeff(XYEta::X);
    const auto y_coeff = constraint.coeff(XYEta::Y);
    const auto eta_coeff = constraint.coeff(XYEta::Eta);

    const LinComArrZ<XY> arg{x_coeff, y_coeff};

    // ax + by + ceta = 0 is the constraint
    // Rearrange, and we get ax + by = -ceta
    // if c is even, then we sin both sides, and get
    // sin(ax + by) = sin(-ceta) = 0
    // if c is odd, then we cos both sides, and get
    // cos(ax + by) = cos(-ceta) = 0
    // And that is how we get the factor

    if (eta_coeff % 2 == 0) {
        // We shouldn't need to do a simplification here (x_coeff > 0), and indeed,
        // taking out a sign may mess up whether we flip or not. Actually, as long
        // as we account for the sign when doing the division, it doesn't matter.
        const Sin<LinComArrZ<XY>> sin{arg};
        const Equation<Sin> factor{{{sin, 1}}};

        //std::cout << triple << ", " << factor << std::endl;

        const bool same = (eta_coeff / 2) % 2 == 0;

        // neg is normally not positive (false), and then if
        // same is true, that means same != false, so neg is now
        // false, which is what we want. Ditto for pos

        // == is the predicate with the following truth table
        // T _ is _
        // F _ is !_

        const auto neg = (same == false);
        const auto pos = (same == true);

        remove_factor(stable_neg_info, factor, neg);
        remove_factor(stable_pos_info, factor, pos);
    } else {

        const Cos<LinComArrZ<XY>> cos{arg};
        const Equation<Cos> factor{{{cos, 1}}};

        //std::cout << triple << ", " << factor << std::endl;

        const bool same = ((eta_coeff - 1) / 2) % 2 == 0;

        const auto neg = (same == false);
        const auto pos = (same == true);

        remove_factor(stable_neg_info, factor, neg);
        remove_factor(stable_pos_info, factor, pos);
    }
}

static bool remove_factor_half_duplicate_stables(CodeInfo& info, const Equation<Cos>& factor, const bool pos) {
    bool hasEquation = false;
    std::cout << "Factor: " << factor << std::endl;
    std::vector<std::pair<Equation<Sin>, Equation<Sin>>> sines{};
    std::vector<std::pair<Equation<Cos>, Equation<Cos>>> cosines{};

    // Find the ones we should divide out and their factored versions

    for (const auto& eq : info.sin_equations) {
        const auto opt = divide_once(eq, factor);
        if (opt) {
            auto factored = *opt;

            if (!pos) {
                // this is a negative stable info, so we need to negate the factor
                factored.scale(-1);
            }

            sines.emplace_back(eq, factored);
        }
    }

    for (const auto& eq : info.cos_equations) {
        const auto opt = divide_once(eq, factor);

        if (opt) {
            auto factored = *opt;

            if (!pos) {
                factored.scale(-1);
            }

            cosines.emplace_back(eq, factored);
        }
    }

    // label_remove_factor_half_duplicate_stables
//    for (const auto& p : sines) {
//            std::cout << p.first << " , " << p.second << std::endl;
//        }
//        for (const auto& p : cosines) {
//           std::cout << p.first << " , " << p.second << std::endl;
//        }

    // Now remove the equations that we found and insert the factored ones
    for (const auto& p : sines) {
        info.sin_equations.erase(p.first);
        info.sin_equations.insert(p.second);
        std::cout << "removing " << p.first << std::endl;
        std::cout << "leaving  " << p.second << std::endl;
        hasEquation = true;
    }

    for (const auto& p : cosines) {
        info.cos_equations.erase(p.first);
        info.cos_equations.insert(p.second);
        std::cout << "removing " << p.first << std::endl;
        std::cout << "leaving  " << p.second << std::endl;
        hasEquation = true;
    }
    return hasEquation;
}

static bool remove_factor_duplicate_stables(CodeInfo& info, const Equation<Cos>& factor, const bool pos, const bool show) {
    bool hasEquation = false;
    // for Sin
    // Cos
    // Sin
    //
    // for Cos
    // Sin
    // Cos
    if(show){
        std::cout << "Factor: " << factor << std::endl;
    }
    std::vector<std::pair<Equation<Sin>, Equation<Sin>>> sines{};
    std::vector<std::pair<Equation<Cos>, Equation<Cos>>> cosines{};

    // Find the ones we should divide out and their factored versions

    for (const auto& eq : info.sin_equations) {
        const auto opt = divide_once(eq, factor);
        if (opt) {
            auto factored = *opt;

            if (!pos) {
                // this is a negative stable info, so we need to negate the factor
                factored.scale(-1);
            }

            sines.emplace_back(eq, factored);
        }
    }

    for (const auto& eq : info.cos_equations) {
        const auto opt = divide_once(eq, factor);

        if (opt) {
            auto factored = *opt;

            if (!pos) {
                factored.scale(-1);
            }

            cosines.emplace_back(eq, factored);
        }
    }

    // label_remove_factor Duplicate Stables

    for (const auto& p : sines) {
            hasEquation = true;
            if (show){
                std::cout << "removing " << p.first << std::endl;
                std::cout << "leaving  " << p.second << std::endl;
            }
        }
    for (const auto& p : cosines) {
            hasEquation = true;
            if (show){
                std::cout << "removing " << p.first << std::endl;
                std::cout << "leaving  " << p.second << std::endl;
            }
        }


    // Now remove the equations that we found and insert the factored ones
    for (const auto& p : sines) {
        info.sin_equations.erase(p.first);
        info.sin_equations.insert(p.second);
    }

    for (const auto& p : cosines) {
        info.cos_equations.erase(p.first);
        info.cos_equations.insert(p.second);
    }
    return hasEquation;
}

std::pair<bool, bool> remove_factor_duplicate_stables(CodeInfo& stable_neg_info, const CodePair& unstable, CodeInfo& stable_pos_info, const bool show) {
    bool first = false;
    bool second = false;
    const auto constraint = unstable.sequence.constraint(unstable.angles.first, unstable.angles.second);

    const auto x_coeff = constraint.coeff(XYEta::X);
    const auto y_coeff = constraint.coeff(XYEta::Y);
    const auto eta_coeff = constraint.coeff(XYEta::Eta);

    const LinComArrZ<XY> arg{x_coeff, y_coeff};

    // ax + by + ceta = 0 is the constraint
    // Rearrange, and we get ax + by = -ceta
    // if c is even, then we sin both sides, and get
    // sin(ax + by) = sin(-ceta) = 0
    // if c is odd, then we cos both sides, and get
    // cos(ax + by) = cos(-ceta) = 0
    // And that is how we get the factor

    if (eta_coeff % 2 == 0) {
        // We shouldn't need to do a simplification here (x_coeff > 0), and indeed,
        // taking out a sign may mess up whether we flip or not. Actually, as long
        // as we account for the sign when doing the division, it doesn't matter.
        const Sin<LinComArrZ<XY>> sin{arg};
        const Equation<Sin> factor{{{sin, 1}}};

        //std::cout << triple << ", " << factor << std::endl;

        const bool same = (eta_coeff / 2) % 2 == 0;

        // neg is normally not positive (false), and then if
        // same is true, that means same != false, so neg is now
        // false, which is what we want. Ditto for pos

        // == is the predicate with the following truth table
        // T _ is _
        // F _ is !_

        const auto neg = (same == false);
        const auto pos = (same == true);

        first = remove_factor_duplicate_stables(stable_neg_info, factor, neg, show);
        second = remove_factor_duplicate_stables(stable_pos_info, factor, pos, show);

    } else {

        const Cos<LinComArrZ<XY>> cos{arg};
        const Equation<Cos> factor{{{cos, 1}}};

        //std::cout << triple << ", " << factor << std::endl;

        const bool same = ((eta_coeff - 1) / 2) % 2 == 0;

        const auto neg = (same == false);
        const auto pos = (same == true);

        first = remove_factor_duplicate_stables(stable_neg_info, factor, neg, show);
        second = remove_factor_duplicate_stables(stable_pos_info, factor, pos, show);
    }

   return std::make_pair(first, second);
}

bool remove_factor_duplicate_stables_2(CodeInfo& stable_neg_info, const CodePair& unstable) {
    bool hasEquation = false;
    const auto constraint = unstable.sequence.constraint(unstable.angles.first, unstable.angles.second);

    const auto x_coeff = constraint.coeff(XYEta::X);
    const auto y_coeff = constraint.coeff(XYEta::Y);
    const auto eta_coeff = constraint.coeff(XYEta::Eta);

    const LinComArrZ<XY> arg{x_coeff, y_coeff};

    // ax + by + ceta = 0 is the constraint
    // Rearrange, and we get ax + by = -ceta
    // if c is even, then we sin both sides, and get
    // sin(ax + by) = sin(-ceta) = 0
    // if c is odd, then we cos both sides, and get
    // cos(ax + by) = cos(-ceta) = 0
    // And that is how we get the factor

    if (eta_coeff % 2 == 0) {
        // We shouldn't need to do a simplification here (x_coeff > 0), and indeed,
        // taking out a sign may mess up whether we flip or not. Actually, as long
        // as we account for the sign when doing the division, it doesn't matter.
        const Sin<LinComArrZ<XY>> sin{arg};
        const Equation<Sin> factor{{{sin, 1}}};

        //std::cout << triple << ", " << factor << std::endl;

        const bool same = (eta_coeff / 2) % 2 == 0;

        // neg is normally not positive (false), and then if
        // same is true, that means same != false, so neg is now
        // false, which is what we want. Ditto for pos

        // == is the predicate with the following truth table
        // T _ is _
        // F _ is !_

        const auto neg = (same == false);
        hasEquation = remove_factor_half_duplicate_stables(stable_neg_info, factor, neg);
    } else {

        const Cos<LinComArrZ<XY>> cos{arg};
        const Equation<Cos> factor{{{cos, 1}}};

        //std::cout << triple << ", " << factor << std::endl;

        const bool same = ((eta_coeff - 1) / 2) % 2 == 0;

        const auto neg = (same == false);

        hasEquation = remove_factor_half_duplicate_stables(stable_neg_info, factor, neg);
    }
    return hasEquation;
}

uint32_t digits_to_bits(const uint32_t digits) {
    // This is the function that boost uses, and so we'll keep it for now

    // log2(10) ~ 1000/301

    return (digits * 1000) / 301 + ((digits * 1000) % 301 ? 2 : 1);
}

template <template <typename> class Trig>
bool all_positive(const std::vector<std::pair<EqVec<Trig>, Coeff64>>& eqs, const PointQ& center, const Rational& radius, Evaluator& eval) {

    for (const auto& p : eqs) {

        const auto pos = eval.is_positive(p.first, p.second, center, radius);

        if (!pos) {
            return false;
        }
    }

    return true;
}

// the first is if the square was
bool is_positive(const StableInfo& info, const ClosedRectangleQ& square, const PointQ& center, const Rational& radius, Evaluator& eval) {

    // We can only try to color a square if it is contained within the bounding polygon
    // Because we throw some equations out earlier, it is possible that squares outside
    // the polygon could be colored, which we don't want
    const auto result = subset(square, info.polygon) &&
                        all_positive(info.sines, center, radius, eval) &&
                        all_positive(info.cosines, center, radius, eval);

    return result;
}

struct GetMidpoint : public boost::static_visitor<boost::optional<PointQ>> {

    boost::optional<PointQ> operator()(const geometry::Empty) const {
        return boost::none;
    }

    boost::optional<PointQ> operator()(const PointQ& point) const {
        return point;
    }

    boost::optional<PointQ> operator()(const ClosedSegmentQ& seg) const {
        return seg.midpoint();
    }
};

struct GetIntersection : public boost::static_visitor<boost::optional<ClosedSegmentQ>> {

    boost::optional<ClosedSegmentQ> operator()(const geometry::Empty) const {
        return boost::none;
    }

    boost::optional<ClosedSegmentQ> operator()(const PointQ& point) const {
        return boost::none;
    }

    boost::optional<ClosedSegmentQ> operator()(const ClosedSegmentQ& seg) const {
        return seg;
    }
};

template <typename N>
bool collinear_element(const geometry::Point<N>& point, const geometry::Point<N>& start, const geometry::Point<N>& end) {

        if (start.x == end.x) {
            // vertical
            const auto minmax = std::minmax(start.y, end.y);
            return minmax.first <= point.y && point.y <= minmax.second;
        } else {
            // not vertical
            const auto minmax = std::minmax(start.x, end.x);
            return minmax.first <= point.x && point.x <= minmax.second;
        }
    }

template <typename N>
bool intersects(const geometry::Point<N>& l0_start, const geometry::Point<N>& l0_end, const geometry::Point<N>& l1_start, const geometry::Point<N>& l1_end) {

        const auto orient0 = geometry::Point<N>::orientation(l0_start, l0_end, l1_start);
        const auto orient1 = geometry::Point<N>::orientation(l0_start, l0_end, l1_end);
        const auto orient2 = geometry::Point<N>::orientation(l1_start, l1_end, l0_start);
        const auto orient3 = geometry::Point<N>::orientation(l1_start, l1_end, l0_end);

        // Proper intersection. In this case, the line segments intersect on their
        // interiors at a single point.
        if (orient0 != 0 && orient1 != 0 && orient2 != 0 && orient3 != 0) {
            if ((orient0 != orient1) && (orient2 != orient3)) {
                return true;
            }
        }

        // Non proper intersection. In this case, one of the endpoints
        // touches the other line segment
        if (orient0 == 0 && collinear_element(l1_start, l0_start, l0_end)) {
            return true;
        }

        if (orient1 == 0 && collinear_element(l1_end, l0_start, l0_end)) {
            return true;
        }

        if (orient2 == 0 && collinear_element(l0_start, l1_start, l1_end)) {
            return true;
        }

        if (orient3 == 0 && collinear_element(l0_end, l1_start, l1_end)) {
            return true;
        }

        // Else, they don't intersect

        return false;
}

bool is_positive(const TripleInfo& info, const ClosedRectangleQ& square, const PointQ& center, const Rational& radius, Evaluator& eval) {

    const auto inter = special_intersection(square, info.unstable_info.segment);

    const auto midpoint = boost::apply_visitor(GetMidpoint{}, inter);

    if (midpoint) {

        // TODO we could also use a smaller square in this case too
        // TODO we also need to replace the intersects with a different check, one that
        // checks if the region we are looking at is a subset of the polygon
        const auto stable_pos_works = //intersects(square, info.stable_pos_info.polygon) &&
            all_positive(info.stable_pos_info.sines, center, radius, eval) &&
            all_positive(info.stable_pos_info.cosines, center, radius, eval);

        const auto unstable_works =
            all_positive(info.unstable_info.sines, *midpoint, radius, eval) &&
            all_positive(info.unstable_info.cosines, *midpoint, radius, eval);

        // We should do a check. If the square intersects the bounding polygon of either of the regions,
        // then we need to check that region. If it does not, then we don't need to check. This deals
        // with subtle issues like when the square is right on the side of a bounding polygon, but
        // strictly speaking don't intersect, since the bounding polygon is open.
        // We will leave it like this for now.

        // Normally, we need the square to be a subset of the bounding polygon. Is that a problem here?

        // And a smaller square here too
        const auto stable_neg_works = //intersects(square, info.stable_neg_info.polygon) &&
            all_positive(info.stable_neg_info.sines, center, radius, eval) &&
            all_positive(info.stable_neg_info.cosines, center, radius, eval);
       
        return stable_pos_works && unstable_works && stable_neg_works;
    }

    return false;
}

Integer get_cost(const StableInfo& info) {

    Integer cost = 0;

    for (const auto& eq : info.sines) {
        cost += eq.first.size();
    }

    for (const auto& eq : info.cosines) {
        cost += eq.first.size();
    }

    return cost;
}

static cover::Cover cover_square(const ClosedConvexPolygonQ& polygon, const SinglePair& single_pair, const StableInfo& info, const ClosedRectangleQ& square, const uint32_t prec, const uint32_t extra_depth) {

    // TODO only try to color a square if it intersects the polygon
    // we could in theory optimize this: if a square is already inside a polygon,
    // we do not need to do another intersection check
    if (!intersects(square, polygon)) {
        // Do nothing, since empty
        return cover::Empty{};
    }

    Evaluator eval{prec};

    const auto center = square.center();
    const Rational radius = square.width() / 2;

    if (is_positive(info, square, center, radius, eval)) {
        return cover::Single{single_pair};
    }

    if (extra_depth != 0) {
        // We have extra magnifications left, so we can try subdividing

        const auto quarters = subdivide(square);

        // Contain cover::Empty by default, but these get overwritten, so it doesn't matter
        cover::Cover cover0{};
        cover::Cover cover1{};
        cover::Cover cover2{};
        cover::Cover cover3{};

        const auto l0 = [&] {
            cover0 = cover_square(polygon, single_pair, info, std::get<0>(quarters), prec, extra_depth - 1);
        };

        const auto l1 = [&] {
            cover1 = cover_square(polygon, single_pair, info, std::get<1>(quarters), prec, extra_depth - 1);
        };

        const auto l2 = [&] {
            cover2 = cover_square(polygon, single_pair, info, std::get<2>(quarters), prec, extra_depth - 1);
        };

        const auto l3 = [&] {
            cover3 = cover_square(polygon, single_pair, info, std::get<3>(quarters), prec, extra_depth - 1);
        };

        tbb::parallel_invoke(l0, l1, l2, l3);

        return cover::Divide{std::move(cover0), std::move(cover1), std::move(cover2), std::move(cover3)};
    }

    // no extra magnifications, so not covered
    return cover::Empty{};
}

static cover::Cover cover_square(const ClosedConvexPolygonQ& polygon, const TriplePair& triple, const TripleInfo& triple_info,
                                 const StableInfo& stable_neg, const StableInfo& stable_pos,
                                 const ClosedRectangleQ& square, const uint32_t prec, const uint32_t extra_depth) {

    // TODO only try to color a square if it intersects the polygon
    // we could in theory optimize this: if a square is already inside a polygon,
    // we do not need to do another intersection check
    if (!intersects(square, polygon)) {
        // Do nothing, since empty
        return cover::Empty{};
    }

    Evaluator eval{prec};

    const auto center = square.center();
    const Rational radius = square.width() / 2;

    if (is_positive(triple_info, square, center, radius, eval)) {
        return cover::Triple{triple};
    }

    if (is_positive(stable_neg, square, center, radius, eval)) {
        return cover::Single{SinglePair{triple.stable_neg}};
    }

    if (is_positive(stable_pos, square, center, radius, eval)) {
        return cover::Single{SinglePair{triple.stable_pos}};
    }

    if (extra_depth != 0) {
        // We have extra magnifications left, so we can try subdividing

        const auto quarters = subdivide(square);

        // Contain cover::Empty by default, but these get overwritten, so it doesn't matter
        cover::Cover cover0{};
        cover::Cover cover1{};
        cover::Cover cover2{};
        cover::Cover cover3{};

        const auto l0 = [&] {
            cover0 = cover_square(polygon, triple, triple_info, stable_neg, stable_pos, std::get<0>(quarters), prec, extra_depth - 1);
        };

        const auto l1 = [&] {
            cover1 = cover_square(polygon, triple, triple_info, stable_neg, stable_pos, std::get<1>(quarters), prec, extra_depth - 1);
        };

        const auto l2 = [&] {
            cover2 = cover_square(polygon, triple, triple_info, stable_neg, stable_pos, std::get<2>(quarters), prec, extra_depth - 1);
        };

        const auto l3 = [&] {
            cover3 = cover_square(polygon, triple, triple_info, stable_neg, stable_pos, std::get<3>(quarters), prec, extra_depth - 1);
        };

        tbb::parallel_invoke(l0, l1, l2, l3);

        return cover::Divide{std::move(cover0), std::move(cover1), std::move(cover2), std::move(cover3)};
    }

    // no extra magnifications, so not covered
    return cover::Empty{};
}


cover::Cover CoverAll::operator()(const cover::Empty) const {

    if (intersects(square, polygon)) {
        throw std::runtime_error("empty intersects polygon");
    }

    return cover::Empty{};
}

cover::Cover CoverAll::operator()(const cover::Single& single) const {

    const auto& stable_info = stable_infos.at(single.single_pair);

    return cover_square(polygon, single.single_pair, stable_info, square, prec, extra_depth);
}

cover::Cover CoverAll::operator()(const cover::Triple& triple) const {

    const auto& triple_pair = triple.triple_pair;

    const auto& triple_info = triple_infos.at(triple_pair);
    const auto& stable_neg = stable_infos.at(SinglePair{triple_pair.stable_neg});
    const auto& stable_pos = stable_infos.at(SinglePair{triple_pair.stable_pos});

    return cover_square(polygon, triple.triple_pair, triple_info, stable_neg, stable_pos, square, prec, extra_depth);
}

cover::Cover CoverAll::operator()(const cover::Divide& divide) const {

    const auto quarter_squares = subdivide(square);
    const auto& quarter_covers = divide.quarters.get();

    // These values get overwritten anyway
    cover::Cover cover0{};
    cover::Cover cover1{};
    cover::Cover cover2{};
    cover::Cover cover3{};

    const auto l0 = [&] {
        const CoverAll verifier{std::get<0>(quarter_squares), polygon, stable_infos, triple_infos, prec, extra_depth};
        cover0 = boost::apply_visitor(verifier, quarter_covers.get<0>());
    };

    const auto l1 = [&] {
        const CoverAll verifier{std::get<1>(quarter_squares), polygon, stable_infos, triple_infos, prec, extra_depth};
        cover1 = boost::apply_visitor(verifier, quarter_covers.get<1>());
    };

    const auto l2 = [&] {
        const CoverAll verifier{std::get<2>(quarter_squares), polygon, stable_infos, triple_infos, prec, extra_depth};
        cover2 = boost::apply_visitor(verifier, quarter_covers.get<2>());
    };

    const auto l3 = [&] {
        const CoverAll verifier{std::get<3>(quarter_squares), polygon, stable_infos, triple_infos, prec, extra_depth};
        cover3 = boost::apply_visitor(verifier, quarter_covers.get<3>());
    };

    tbb::parallel_invoke(l0, l1, l2, l3);

    return cover::Divide{std::move(cover0), std::move(cover1), std::move(cover2), std::move(cover3)};
}

CoverInfo cover_to_info(const ClosedConvexPolygonQ& polygon, const ClosedRectangleQ& square, const cover::Cover& cover) {

    CoverInfo cover_info{0};

    CoverVisitor cover_visitor{cover_info, polygon, square, 0};
    boost::apply_visitor(cover_visitor, cover);

    return cover_info;
}

std::string center_degrees(const ClosedRectangleQ& rect) {
    const auto center = rect.center();

    const auto x = static_cast<Float>(center.x * 90);
    const auto y = static_cast<Float>(center.y * 90);

    std::ostringstream oss{};
    oss.precision(std::numeric_limits<Float>::max_digits10);
    oss << x << ' ' << y;
    return oss.str();
}

// There are two main concerns with the references-in-cover issue:
// One, the elements the references point to must not go out of scope before
// the references do, else we have a lifetime issue
// Two, the elements the references point to must not move around, like a realloc
// or something. If this is the case, the refences will now point to different elements
// or no elements at all
