#pragma once

#include <algorithm> // std::minmax, etc.
#include <ostream>   // std::ostream
#include <sstream>   // std::ostringstream
#include <vector>    // std::vector

#include "point.hpp"
#include "types.hpp"

namespace geometry {

// Let v0, v1, ..., v(n-1) be n points in the plane, where n >= 3.
template <typename N, Topology Top>
class ConvexPolygon final {

  private:
    std::vector<Point<N>> vertices;

    // This is all assumed to be closed intervals. Really unfortunate that we can't reuse the other
    // algorithm for this.
    static bool collinear_element(const Point<N>& point, const Point<N>& start, const Point<N>& end) {

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

    // Taken from Computational Geometry in C
    //https://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect/
    // l0_start and l0_end defines a closed line segment. Note that the points are not
    // necissarily assumed to be distinct, and this will not cause an error if they are
    static bool intersects(const Point<N>& l0_start, const Point<N>& l0_end,
                           const Point<N>& l1_start, const Point<N>& l1_end) {

        const auto orient0 = Point<N>::orientation(l0_start, l0_end, l1_start);
        const auto orient1 = Point<N>::orientation(l0_start, l0_end, l1_end);
        const auto orient2 = Point<N>::orientation(l1_start, l1_end, l0_start);
        const auto orient3 = Point<N>::orientation(l1_start, l1_end, l0_end);

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

    void check() const {

        const auto size = vertices.size();

        if (size < 3) {
            std::ostringstream oss{};
            oss << "degenerate ConvexPolygon: less than three vertices" << '\n'
                << *this;
            throw std::runtime_error(oss.str());
        }

        // First, the polygon must be simple. All non-adjecent edges must not intersect, and
        // adjacent edges can only interesct at their shared vertex.
        // Second, it must be strictly convex.

        // Note that a corollary of being simple is that all vertices must be distinct.
        // This is included in the following checks.

        // First check that all non-adjacent edges do not intersect.
        // If non-adjacent edges share a vertex, this will be caught here.

        // There are (n choose 2) pairs of distinct edges in total. There are n pairs of
        // adjacent edges, so there are (n choose 2) - n = 1/2*n*(n - 3) pairs of
        // non-adjacent edges. We need to check that all of these do not intersect.

        // All (n choose 2) pairs of distinct edges can be iterated through in the following
        // loop:

        // for (size_t i = 0; i < size; ++i)
        //     for (size_t j = i + 1; j < size; ++j)

        // Howevever, we do not want to examine pairs of adjacent edges. We can eliminate
        // most of them by doing
        //     for (size_t j = i + 2; j < size; ++j)
        // This eliminates n - 1 of the adjacent edges. The last one is the pair (0, size - 1)
        // which is eliminated with a simple check.

        // This is currently an O(n^2) loop. There are O(n logn) sweep-line algorithms,
        // but those require trees and are somewhat complicated. This is fine for now.
        for (size_t i = 0; i < size - 2; ++i) {
            // i is the index of the current edge
            // because of the upper bound on j, we only need to do up to size - 2

            // This will currently throw an exception if the vertices are the same.
            // This is normally caught in part 2.

            // i + 1 is always less than size - 1, so no need to mod
            //const Segment<N, Topology::Closed> e0{vertices.at(i),
            //vertices.at(i + 1)};

            for (size_t j = i + 2; j < size; ++j) {
                // j is the index of the next non-adjacent edge
                if (i == 0 && j == size - 1)
                    continue;

                const auto& l0_start = vertices.at(i);
                const auto& l0_end = vertices.at(i + 1);

                const auto& l1_start = vertices.at(j);
                const auto& l1_end = vertices.at((j + 1) % size);

                // j + 1 on the other hand can be equal to size, which requires the mod
                //const Segment<N, Topology::Closed> e1{vertices.at(j),
                //vertices.at((j + 1) % size)};

                //if (intersects(e0, e1)) {
                if (intersects(l0_start, l0_end, l1_start, l1_end)) {
                    std::ostringstream oss{};
                    oss << "degenerate ConvexPolygon: non-adjacent edges intersect" << '\n'
                        << *this;
                    throw std::runtime_error(oss.str());
                }
            }
        }

        // Now we check that adjacent edges only intersect at the common vertex,
        // and that the polygon is strictly convex.

        const auto& v0 = vertices.at(0);
        const auto& v1 = vertices.at(1);
        const auto& v2 = vertices.at(2);

        const auto first_sign = Point<N>::orientation(v0, v1, v2);

        if (first_sign == 0) {
            std::ostringstream oss{};
            oss << "degenerate ConvexPolygon: collinear vertices" << '\n'
                << *this;
            throw std::runtime_error(oss.str());
        }

        for (size_t i = 1; i < size; ++i) {
            const auto& vi = vertices.at(i);
            const auto& vii = vertices.at((i + 1) % size);
            const auto& viii = vertices.at((i + 2) % size);

            const auto sign = Point<N>::orientation(vi, vii, viii);

            if (sign == 0) {
                std::ostringstream oss{};
                oss << "degenerate ConvexPolygon: collinear vertices" << '\n'
                    << *this;
                throw std::runtime_error(oss.str());
            } else if (sign != first_sign) {
                std::ostringstream oss{};
                oss << "degenerate ConvexPolygon: non-convex polygon" << '\n'
                    << *this;
                throw std::runtime_error(oss.str());
            }
        }
    }

    void normalize() {

        // The polygon is convex, so all edges have the same orientation
        const auto orient = Point<N>::orientation(vertices.at(0), vertices.at(1), vertices.at(2));

        // If the polygon is oriented CW, then we reverse it to have a CCW orientation
        if (orient < 0) {
            std::reverse(std::begin(vertices), std::end(vertices));
        }

        // Now we rotate the vector so the smallest point is first
        // The minimum element is unique since all points are distinct
        const auto min = std::min_element(std::begin(vertices), std::end(vertices));
        std::rotate(std::begin(vertices), min, std::end(vertices));
    }

    void print_points(std::ostream& os) const {

        bool first = true;
        for (const auto& vertex : vertices) {
            if (!first) {
                os << ", ";
            }

            first = false;

            os << vertex;
        }
    }

  public:
    using value_type = typename decltype(vertices)::value_type;
    using const_iterator = typename decltype(vertices)::const_iterator;

    ConvexPolygon(const std::vector<Point<N>>& vertices_, const Check ch = Check::Checked)
        : vertices{vertices_} {

        if (ch == Check::Checked) {
            check();
        }

        normalize();
    }

    const_iterator begin() const {
        return std::begin(vertices);
    }

    const_iterator end() const {
        return std::end(vertices);
    }

    const_iterator cbegin() const {
        return std::cbegin(vertices);
    }

    const_iterator cend() const {
        return std::cend(vertices);
    }

    size_t size() const {
        return vertices.size();
    }

    const Point<N>& vertex(const size_t i) const {
        return vertices.at(i % vertices.size());
    }

    // ------------------------------------------------------------------------
    // Operators
    // ------------------------------------------------------------------------

    friend bool operator==(const ConvexPolygon<N, Top>& lhs, const ConvexPolygon<N, Top>& rhs) {
        return lhs.vertices == rhs.vertices;
    }

    friend bool operator!=(const ConvexPolygon<N, Top>& lhs, const ConvexPolygon<N, Top>& rhs) {
        return !(lhs == rhs);
    }

    friend bool operator<(const ConvexPolygon<N, Top>& lhs, const ConvexPolygon<N, Top>& rhs) {
        return lhs.vertices < rhs.vertices;
    }

    friend bool operator>(const ConvexPolygon<N, Top>& lhs, const ConvexPolygon<N, Top>& rhs) {
        return rhs < lhs;
    }

    friend bool operator<=(const ConvexPolygon<N, Top>& lhs, const ConvexPolygon<N, Top>& rhs) {
        return !(lhs > rhs);
    }

    friend bool operator>=(const ConvexPolygon<N, Top>& lhs, const ConvexPolygon<N, Top>& rhs) {
        return rhs <= lhs;
    }

    friend std::ostream& operator<<(std::ostream& os, const ConvexPolygon<N, Top>& poly) {

        os << '[';

        poly.print_points(os);

        os << ']';
        return os;
    }
};
}
