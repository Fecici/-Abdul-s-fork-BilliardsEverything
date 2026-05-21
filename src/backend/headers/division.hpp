#pragma once

#include "evaluator.hpp"
#include "general.hpp"

#if 0
// This cannot be parallelized, since we are doing a fold (essentially)
template <template <typename> class Trig>
void cover_polygon(Inserter& insert, Evaluator& eval,
                   const OpenConvexPolygonQ& polygon, const ClosedRectangleQ& rect,
                   bool subset, const uint32_t mags_left) {

    // The bigger rect was not a subset, so check if the smaller rect is a subset
    if (!subset) {
        subset = geometry::subset(rect, polygon);

        if (!subset && !geometry::intersects(rect, polygon)) {
            // We don't need to worry about coloring this rectangle, so ignore it
            return;
        }
        // Else if it is a subset or it intersects, then we can continue
    }

    const auto c = center(rect);
    const auto rx = radius_x(rect);
    const auto ry = radius_y(rect);

    for (const auto& kv : insert.sines) {
        const auto& eq = kv.first;
        const auto& info = kv.second;
        if (info.still_in_the_running) {

            const auto pos = eval.is_positive(eq, info.bx, info.by, c, rx, ry);

            if (pos && mags_left == 0) {
                // keep going
            }

            if (pos && mags_left != 0) {
                // don't check subdivisions
            }

            if (!pos and mags_left == 0) {
                // failed for good
            }

            if (!pos and mags_left != 0) {
                // try subdividing
            }

        }
    }

    if (mags_left != 0) {
        // Else we have magnifications left, so try subdividing

        const auto quarters = subdivide(rect);

        for (const auto& quarter : quarters) {
            if (!cover_polygon(polygon, quarter, eq, bx, by, subset, mags_left - 1, eval)) {
                // if we were unable to cover a rect, short-circuit false
                return false;
            }
        }

        // Each quarter was covered, so the entire square was covered too
        return true;
    } else {
        // There are no magnifications left, so everything that has currently failed has
        // permanently failed
    }

    // no magnifications left, so false
    return false;
}

template <template <typename> class Trig>
bool cover_segment(const ClosedSegmentQ& seg,
                   const Equation<Trig>& eq, const Coeff64 bx, const Coeff64 by,
                   const uint32_t mags_left, Evaluator& eval) {

    const auto c = center(seg);
    const auto rx = radius_x(seg);
    const auto ry = radius_y(seg);

    if (eval.is_positive(eq, bx, by, c, rx, ry)) {
        // The equation is positive over this segment
        return true;
    }

    if (mags_left != 0) {
        // Else we have magnifications left, so try subdividing

        const auto sub_segs = subdivide(seg);

        for (const auto& sub_seg : sub_segs) {
            if (!cover_segment(sub_seg, eq, bx, by, mags_left - 1, eval)) {
                // If we were unable to cover a segment, short-circuit false
                return false;
            }
        }

        // Both sub-segments were covered, so the entire segment was covered too
        return true;
    }

    // no magnifications left, so false
    return false;
}

// If it fails on the current square, you check the smaller square to see if that failed.
// If an equation fails on a square where we cannot subdivide, then it fails for good,
// and it is no longer in the running.
// If it is still in the running and positive on the current square, we do not need to check any subdivisions
// If it is positive on the current square, then we do not need to check any subdivisions
// If it is not positive on the current square, then

struct ChopInfo {
    Coeff64 bx;
    Coeff64 by;
    bool still_in_the_running;
    bool check_subsquare;

    explicit ChopInfo(const Coeff64 bx_, const Coeff64 by_, const bool pos_)
        : bx{bx_}, by{by_}, pos{pos_} {}
};

struct Inserter {
    // TODO change to EqVec
    // There may be duplicates when doing insertion, so we must have a map
    std::map<Equation<Sin>, ChopInfo> sines;
    std::map<Equation<Cos>, ChopInfo> cosines;

    explicit Inserter() : sines{}, cosines{} {}

    void insert(const Equation<Sin>& eq) {

        const auto b = gradient_bounds(eq);

        sines.emplace(eq, ChopInfo{b.first, b.second, true});
    }

    void insert(const Equation<Cos>& eq) {

        const auto b = gradient_bounds(eq);

        cosines.emplace(eq, ChopInfo{b.first, b.second, true});
    }
};
#endif

struct Inserter {
    Curves curves;
    Evaluator eval;
    const PointQ& center;
    const Rational& rx;
    const Rational& ry;

    explicit Inserter(const PointQ& center_, const Rational& rx_, const Rational& ry_)
        : curves{}, eval{64}, center{center_}, rx{rx_}, ry{ry_} {}

    void insert(const Equation<Sin>& eq) {

        const auto b = gradient_bounds(eq);

        if (!eval.is_positive(eq, b.first, b.second, center, rx, ry)) {
            curves.first.insert(eq);
        }
    }

    void insert(const Equation<Cos>& eq) {

        const auto b = gradient_bounds(eq);

        if (!eval.is_positive(eq, b.first, b.second, center, rx, ry)) {
            curves.second.insert(eq);
        }
    }
};

void divide_out_lines_lr(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com, CurvesLR& curves, const LeftRight& left_right);

void divide_out_lines_lr(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com, CurvesLR& curves, const LeftRight& left_right);

void divide_out_lines(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com, const XYZ first, const XYZ second, Inserter& insert);

void divide_out_lines(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com, const XYZ first, const XYZ second, Inserter& insert);

void divide_out_lines(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com, Curves& curves, const XYZ first, const XYZ second);

void divide_out_lines(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com, Curves& curves, const XYZ first, const XYZ second);

boost::optional<LinComMapZ<Cos<LinComArrZ<XY>>>>
divide_once(const LinComMapZ<Sin<LinComArrZ<XY>>>& eq, const LinComMapZ<Sin<LinComArrZ<XY>>>& factor);

boost::optional<LinComMapZ<Sin<LinComArrZ<XY>>>>
divide_once(const LinComMapZ<Sin<LinComArrZ<XY>>>& eq, const LinComMapZ<Cos<LinComArrZ<XY>>>& factor);

boost::optional<LinComMapZ<Sin<LinComArrZ<XY>>>>
divide_once(const LinComMapZ<Cos<LinComArrZ<XY>>>& eq, const LinComMapZ<Sin<LinComArrZ<XY>>>& factor);

boost::optional<LinComMapZ<Cos<LinComArrZ<XY>>>>
divide_once(const LinComMapZ<Cos<LinComArrZ<XY>>>& eq, const LinComMapZ<Cos<LinComArrZ<XY>>>& factor);
