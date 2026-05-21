#pragma once

#include <algorithm> // std::minmax

#include <boost/variant.hpp> // boost::variant

#include "convex_polygon.hpp"
#include "point.hpp"
#include "rectangle.hpp"
#include "segment.hpp"

// TODO uncheck this

namespace geometry {

// ------------------------------------------------------------------------
// Segment
// ------------------------------------------------------------------------

// These could return a point or an interval
template <typename N, Topology Top>
boost::variant<N, Interval<N, Top>> project_impl(const N& first, const N& second) {

    const auto order = first.compare(second);

    if (order < 0) {
        return Interval<N, Top>{first, second};
    }

    if (order > 0) {
        return Interval<N, Top>{second, first};
    }

    // Else they are equal, so just return the point
    return first;
}

template <typename N, Topology Top>
boost::variant<N, Interval<N, Top>> project_x(const Segment<N, Top>& seg) {

    return project_impl<N, Top>(seg.start().x, seg.end().x);
}

template <typename N, Topology Top>
boost::variant<N, Interval<N, Top>> project_y(const Segment<N, Top>& seg) {

    return project_impl<N, Top>(seg.start().y, seg.end().y);
}

template <typename N, Topology Top>
boost::variant<N, Interval<N, Top>> project(const Segment<N, Top>& seg, const Point<N>& axis) {

    const auto start_dot = Point<N>::dot(seg.start(), axis);
    const auto end_dot = Point<N>::dot(seg.end(), axis);

    return project_impl<N, Top>(start_dot, end_dot);
}

// ------------------------------------------------------------------------
// Rectangle
// ------------------------------------------------------------------------

// Rectangles are always non-degenerate, so projection will always return intervals,
// never points.

template <typename N, Topology Top>
Interval<N, Top> project_x(const Rectangle<N, Top>& rect) {
    return rect.interval_x;
}

template <typename N, Topology Top>
Interval<N, Top> project_y(const Rectangle<N, Top>& rect) {
    return rect.interval_y;
}

template <typename N, Topology Top>
Interval<N, Top> project(const Rectangle<N, Top>& rect, const Point<N>& axis) {

    const auto ll_dot = Point<N>::dot(rect.lower_left(), axis);

    const auto ul_dot = Point<N>::dot(rect.upper_left(), axis);

    const auto ur_dot = Point<N>::dot(rect.upper_right(), axis);

    const auto lr_dot = Point<N>::dot(rect.lower_right(), axis);

    const auto minmax = std::minmax({ll_dot, ul_dot, ur_dot, lr_dot});

    return {minmax.first, minmax.second};
}

// ------------------------------------------------------------------------
// Convex Polygon
// ------------------------------------------------------------------------

// Polygons are also always non-degenerate, so they always return intervals,
// never points.

// Find the min and max x values of all the points
template <typename N, Topology Top>
Interval<N, Top> project_x(const ConvexPolygon<N, Top>& poly) {

    const auto& first = poly.vertex(0);

    // Note: the polygon is normalized, so the first point always has the smallest
    // x coordinate.
    const auto min = first.x;
    auto max = first.x;

    for (size_t i = 1; i < poly.size(); ++i) {

        const auto& point = poly.vertex(i);

        if (point.x > max) {
            max = point.x;
        }
    }

    return {min, max};
}

// Find the min and max y values of all the points
template <typename N, Topology Top>
Interval<N, Top> project_y(const ConvexPolygon<N, Top>& poly) {

    const auto& first = poly.vertex(0);

    auto min = first.y;
    auto max = first.y;

    for (size_t i = 1; i < poly.size(); ++i) {

        const auto& point = poly.vertex(i);

        // We know min <= max, so we only need to do the following comparisons

        if (point.y < min) {
            min = point.y;
        } else if (point.y > max) {
            max = point.y;
        }

        // else min <= y <= max, so there are no changes to make
    }

    return {min, max};
}

template <typename N, Topology Top>
Interval<N, Top> project(const ConvexPolygon<N, Top>& poly, const Point<N>& axis) {

    const auto& first = poly.vertex(0);

    const auto first_dot = Point<N>::dot(first, axis);

    auto min = first_dot;
    auto max = first_dot;

    for (size_t i = 1; i < poly.size(); ++i) {

        const auto& point = poly.vertex(i);

        const auto dot = Point<N>::dot(point, axis);

        // We know min <= max, so we only need to do the following comparisons

        if (dot < min) {
            min = dot;
        } else if (dot > max) {
            max = dot;
        }

        // else min <= dot <= max, so there are no changes to make
    }

    return {min, max};
}
}
