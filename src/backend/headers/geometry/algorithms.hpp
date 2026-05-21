#pragma once

// Every algorithm should have a short proof or explanation of why it works

// TODO also add unchecked here

#include "convex_polygon.hpp"
#include "interval.hpp"
#include "point.hpp"
#include "project.hpp"
#include "rectangle.hpp"
#include "segment.hpp"

namespace geometry {

// ----------------------------------------------------------------------------
// Element
// ----------------------------------------------------------------------------

// This assumes that point is already collinear with the line segment.
template <typename N, Topology Top>
bool collinear_element(const Point<N>& point, const Segment<N, Top>& seg) {

    if (seg.is_vertical()) {
        return element(point.y, boost::get<Interval<N, Top>>(project_y(seg)));
    } else {
        return element(point.x, boost::get<Interval<N, Top>>(project_x(seg)));
    }
}

template <typename N>
bool element(const N& num, const Interval<N, Topology::Closed>& interval) {
    return interval.lower() <= num && num <= interval.upper();
}

template <typename N>
bool element(const N& num, const Interval<N, Topology::Open>& interval) {
    return interval.lower() < num && num < interval.upper();
}

template <typename N, Topology Top>
bool element(const Point<N>& point, const Rectangle<N, Top>& rectangle) {
    return element(point.x, rectangle.interval_x()) && element(point.y, rectangle.interval_y());
}

template <typename N, Topology Top>
bool element(const Point<N>& point, const Segment<N, Top>& segment) {

    if (segment.side(point) != 0) {
        return false;
    }

    return collinear_element(point, segment);
}

// In theory, this algorithm could be optimized to be O(log n)
template <typename N>
bool element(const Point<N>& point, const ConvexPolygon<N, Topology::Closed>& polygon) {

    for (size_t i = 0; i < polygon.size(); ++i) {

        const auto& vi = polygon.vertex(i);
        const auto& vii = polygon.vertex(i + 1);

        const auto orient = Point<N>::orientation(vi, vii, point);

        if (orient == 0) {
            // The point is collinear with the side

            // This polygon is closed, so points on the boundary
            // are inside the polygon

            // The polygon is strictly convex, so this is now
            // equivalent to if the point is inside the segment
            // (since there are no collinear sides)
            const Segment<N, Topology::Closed> side{vi, vii};
            return collinear_element(point, side);
        } else if (orient < 0) {
            // On the outside
            return false;
        }
        // else it has positive orientation, so continue
    }

    // p is on the same side of every edge, so it is on the inside
    return true;
}

template <typename N>
bool element(const Point<N>& point, const ConvexPolygon<N, Topology::Open>& polygon) {

    // Since the polygon has been normalized to be in a ccw orientation, the
    // point should have a ccw orientation with respect to each side
    // The polygon is also open, so being collinear with a side means not an element

    for (size_t i = 0; i < polygon.size(); ++i) {

        const auto& vi = polygon.vertex(i);
        const auto& vii = polygon.vertex(i + 1);

        const auto orient = Point<N>::orientation(vi, vii, point);

        if (orient <= 0) {
            return false;
        }
    }

    return true;
}

// ----------------------------------------------------------------------------
// Subset
// ----------------------------------------------------------------------------

// This is identical for open and closed intervals
template <typename N, Topology Top>
bool subset(const Interval<N, Top>& a, const Interval<N, Top>& b) {
    return b.lower() <= a.lower() && a.upper() <= b.upper();
}

template <typename N, Topology Top>
bool subset(const Rectangle<N, Top>& a, const Rectangle<N, Top>& b) {
    return subset(a.interval_x(), b.interval_x()) && subset(a.interval_y(), b.interval_y());
}

template <typename N, Topology Top>
bool subset(const ConvexPolygon<N, Topology::Closed>& polygon, const Rectangle<N, Top>& rectangle) {

    for (const auto& vertex : polygon) {
        if (!element(vertex, rectangle)) {
            return false;
        }
    }

    return true;
}

// A closed rectangle is a subset of a convex polygon (open or closed) iff each vertex of the first is in the other
template <typename N, Topology Top>
bool subset(const Rectangle<N, Topology::Closed>& rectangle, const ConvexPolygon<N, Top>& polygon) {

    const auto ll = rectangle.lower_left();
    const auto lr = rectangle.lower_right();

    const auto ul = rectangle.upper_left();
    const auto ur = rectangle.upper_right();

    return element(ll, polygon) && element(lr, polygon) && element(ul, polygon) && element(ur, polygon);
}

// An open rectangle is a subset of a closed convex polygon iff each vertex is in the other
// (open and open is different though)
template <typename N>
bool subset(const Rectangle<N, Topology::Open>& rectangle, const ConvexPolygon<N, Topology::Closed>& polygon) {

    const auto ll = rectangle.lower_left();
    const auto lr = rectangle.lower_right();

    const auto ul = rectangle.upper_left();
    const auto ur = rectangle.upper_right();

    return element(ll, polygon) && element(lr, polygon) && element(ul, polygon) && element(ur, polygon);
}

// ----------------------------------------------------------------------------
// Intersects
// ----------------------------------------------------------------------------

// Using separating axes, it is easier to define a disjoint function, and then define intersects
// in terms of that. I think this will work for whatever types we have (open or closed)

template <typename N>
bool disjoint(const Interval<N, Topology::Open>& a, const Interval<N, Topology::Open>& b) {
    return b.upper() <= a.lower() || a.upper() <= b.lower();
}

template <typename N>
bool disjoint(const Interval<N, Topology::Open>& a, const Interval<N, Topology::Closed>& b) {
    return b.upper() <= a.lower() || a.upper() <= b.lower();
}

template <typename N>
bool disjoint(const Interval<N, Topology::Closed>& a, const Interval<N, Topology::Open>& b) {
    return b.upper() <= a.lower() || a.upper() <= b.lower();
}

template <typename N>
bool disjoint(const Interval<N, Topology::Closed>& a, const Interval<N, Topology::Closed>& b) {
    return b.upper() < a.lower() || a.upper() < b.lower();
}

template <typename N, Topology Top1, Topology Top2>
struct Disjoint : public boost::static_visitor<bool> {

    const Interval<N, Top1>& a;

    explicit Disjoint(const Interval<N, Top1>& a_)
        : a{a_} {}

    bool operator()(const N& p) const {
        return !element(p, a);
    }

    bool operator()(const Interval<N, Top2>& b) const {
        return disjoint(a, b);
    }
};

template <typename N, Topology Top1, Topology Top2>
bool disjoint(const Interval<N, Top1>& a, const boost::variant<N, Interval<N, Top2>>& b) {

    return boost::apply_visitor(Disjoint<N, Top1, Top2>{a}, b);
}

template <typename N, Topology Top1, Topology Top2>
bool disjoint(const boost::variant<N, Interval<N, Top1>>& b, const Interval<N, Top2>& a) {

    return boost::apply_visitor(Disjoint<N, Top2, Top1>{a}, b);
}

// Our segments, rectangles, and polygons are always convex (open or closed)

// The basic idea is this: suppose two convex sets A and B intersect at a point x. Then, along
// any axis that you project A and B, their projections will intersect, because they have
// a point in common. Taking the contrapositive, if a single projection does not intersect
// (a single projection is disjoint) then they do not intersect.
// For our shapes, the idea is that it is sufficient to project only along certain special axes.
// These are the only axes you need to check. I think this is true.
// If disjoint on one of our axes, then disjoint. (proved this above)
// If intersect on all axes, then intersect.

// Hyperplane separation theorem
// https://en.wikipedia.org/wiki/Hyperplane_separation_theorem
// Separation Theorem I - if disjoint => such a separation exists. And clearly, if such a separation exists, then they
// are disjoint. This is true for all compact sets.
// Separation Theorem II - sameses for open sets. However, this does not apply to open segments, since they aren't strictly
// open.

template <typename N, Topology Top1, Topology Top2, template <typename, Topology> class Shape>
bool separating_axis(const Segment<N, Top1>& segment, const Shape<N, Top2>& shape) {

    const auto vec = segment.vector();

    const Point<N> axis{-vec.y, vec.x};

    const auto line_seg_project = project(segment, axis);
    const auto shape_project = project(shape, axis);

    return disjoint(line_seg_project, shape_project);
}

// Project this and the shape along the sides of the rectangle
template <typename N, Topology Top1, Topology Top2, template <typename, Topology> class Shape>
bool separating_axis(const Rectangle<N, Top1>& rectangle, const Shape<N, Top2>& shape) {

    // The axes for the rectangle are just the x and y axes

    const auto shape_project_x = project_x(shape);
    const auto shape_project_y = project_y(shape);

    // The projections of this rectangle along the x and y
    // axes are just interval_x() and interval_y()

    // If either of the intervals are disjoint, then
    // there is a separating axis

    return disjoint(rectangle.interval_x(), shape_project_x) || disjoint(rectangle.interval_y(), shape_project_y);
}

// The shape just needs to have a project() method
template <typename N, Topology Top1, Topology Top2, template <typename, Topology> class Shape>
bool separating_axis(const ConvexPolygon<N, Top1>& polygon, const Shape<N, Top2>& shape) {

    for (size_t i = 0; i < polygon.size(); ++i) {
        const auto& p0 = polygon.vertex(i);
        const auto& p1 = polygon.vertex(i + 1);

        // Vector from p0 to p1
        // The order and perpendicular vector are rather arbitrary
        // as long as they lie on the same line
        const auto edge = p1 - p0;
        // Perpendicular to the edge
        const Point<N> axis{-edge.y, edge.x};

        const auto poly_project = project(polygon, axis);
        const auto shape_project = project(shape, axis);

        if (disjoint(poly_project, shape_project)) {
            return true;
        }
    }

    return false;
}

template <typename Shape1, typename Shape2>
bool disjoint(const Shape1& shape1, const Shape2& shape2) {
    return separating_axis(shape1, shape2) || separating_axis(shape2, shape1);
}

template <typename T, typename S>
bool intersects(const T& a, const S& b) {
    return !disjoint(a, b);
}

// ----------------------------------------------------------------------------
// Intersection
// ----------------------------------------------------------------------------

template <typename N>
boost::variant<Empty, N, Interval<N, Topology::Closed>>
intersection(const Interval<N, Topology::Closed>& a, const Interval<N, Topology::Closed>& b) {

    const auto lower = max(a.lower(), b.lower());
    const auto upper = min(a.upper(), b.upper());

    const auto order = lower.compare(upper);

    if (order > 0) {
        // lower > upper, so empty set
        return Empty{};
    }

    if (order == 0) {
        // lower == upper, so a single point
        return lower;
    }

    // lower < upper, so proper interval
    return Interval<N, Topology::Closed>{lower, upper};
}

// Based on the Liang-Barsky Algorithm
template <typename N>
boost::variant<Empty, Point<N>, Segment<N, Topology::Closed>>
intersection(const Rectangle<N, Topology::Closed>& rect, const Segment<N, Topology::Closed>& seg) {

    const auto delta = seg.vector();

    const std::array<std::pair<N, N>, 4> pqs{{{-delta.x, seg.start().x - rect.interval_x().lower()},
                                              {delta.x, rect.interval_x().upper() - seg.start().x},
                                              {-delta.y, seg.start().y - rect.interval_y().lower()},
                                              {delta.y, rect.interval_y().upper() - seg.start().y}}};

    N t0 = 0;
    N t1 = 1;

    for (const auto& pq : pqs) {

        const auto& p = pq.first;
        const auto& q = pq.second;

        const auto psign = p.sign();
        const auto qsign = q.sign();

        if (psign < 0) {

            N t = q / p;

            if (t > t0) {
                t0 = t;
            }

        } else if (psign > 0) {

            N t = q / p;

            if (t < t1) {
                t1 = t;
            }

        } else {
            // psign == 0

            if (qsign < 0) {
                // segment is parallel to the side and outside the rectangle
                return Empty{};
            }
        }
    }

    const auto order = t0.compare(t1);

    if (order > 0) {
        // t0 > t1, so empty intersection
        return Empty{};
    }

    if (order == 0) {
        // t0 == t1, so single point intersection
        auto point = delta * t0;
        point += seg.start();
        return point;
    }

    // Else t0 < t1, so intersection is a segment

    auto new_start = delta * t0;
    auto new_end = delta * t1;
    new_start += seg.start();
    new_end += seg.start();

    return Segment<N, Topology::Closed>{new_start, new_end};
}

// This is essentially identical to the above code, except that if the endpoints of the segment are in
// the intersection, then we return an empty set. This ensures that we only need to return closed segments.
template <typename N>
boost::variant<Empty, Point<N>, Segment<N, Topology::Closed>>
special_intersection(const Rectangle<N, Topology::Closed>& rect, const Segment<N, Topology::Open>& seg) {

    const auto delta = seg.vector();

    const std::array<std::pair<N, N>, 4> pqs{{{-delta.x, seg.start().x - rect.interval_x().lower()},
                                              {delta.x, rect.interval_x().upper() - seg.start().x},
                                              {-delta.y, seg.start().y - rect.interval_y().lower()},
                                              {delta.y, rect.interval_y().upper() - seg.start().y}}};

    N t0 = 0;
    N t1 = 1;

    for (const auto& pq : pqs) {

        const auto& p = pq.first;
        const auto& q = pq.second;

        const auto psign = p.sign();
        const auto qsign = q.sign();

        if (psign < 0) {

            N t = q / p;

            if (t > t0) {
                t0 = t;
            }

        } else if (psign > 0) {

            N t = q / p;

            if (t < t1) {
                t1 = t;
            }

        } else {
            // psign == 0

            if (qsign < 0) {
                // segment is parallel to the side and outside the rectangle
                return Empty{};
            }
        }
    }

    const auto order = t0.compare(t1);

    if (order > 0) {
        // t0 > t1, so empty intersection
        return Empty{};
    }

    if (order == 0) {
        // t0 == t1, so single point intersection

        if (t0 == 0 || t0 == 1) {
            // The endpoints are not in the intersection
            return Empty{};
        }

        auto point = delta * t0;
        point += seg.start();
        return point;
    }

    // Else t0 < t1, so intersection is a segment
    if (t0 == 0 || t1 == 1) {
        // This is the single point of difference for the special intersection
        return Empty{};
    }

    auto new_start = delta * t0;
    auto new_end = delta * t1;
    new_start += seg.start();
    new_end += seg.start();

    return Segment<N, Topology::Closed>{new_start, new_end};
}
}
