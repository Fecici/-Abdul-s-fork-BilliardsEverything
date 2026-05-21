#pragma once

#include <ostream>   // std::ostream
#include <stdexcept> // std::runtime_error

#include "point.hpp"
#include "types.hpp"

namespace geometry {

// A directed line segment from start -> end
// Various algorithms require that this be directed. Plus, having it undirected is just weird

template <typename N, Topology Top>
class Segment final {
  private:
    Point<N> start_;
    Point<N> end_;

    void check() const {
        if (start_ == end_) {
            throw std::runtime_error("degenerate Segment: start and end are equal");
        }
    }

  public:
    Segment(const Point<N>& start, const Point<N>& end, const Check ch = Check::Checked)
        : start_{start}, end_{end} {

        if (ch == Check::Checked) {
            check();
        }
    }

    const Point<N>& start() const {
        return start_;
    }

    const Point<N>& end() const {
        return end_;
    }

    bool is_horizontal() const {
        return start_.y == end_.y;
    }

    bool is_vertical() const {
        return start_.x == end_.x;
    }

    Point<N> midpoint() const {
        return (start_ + end_) / 2;
    }

    // The vector from start_ -> end_
    Point<N> vector() const {
        return end_ - start_;
    }

    // This is the same as the orientation of the points start -> end -> p
    int side(const Point<N>& point) const {
        return Point<N>::orientation(start_, end_, point);
    }

    // ------------------------------------------------------------------------
    // Operators
    // ------------------------------------------------------------------------

    friend bool operator==(const Segment<N, Top>& lhs, const Segment<N, Top>& rhs) {
        return std::tie(lhs.start_, lhs.end_) == std::tie(rhs.start_, rhs.end_);
    }

    friend bool operator!=(const Segment<N, Top>& lhs, const Segment<N, Top>& rhs) {
        return !(lhs == rhs);
    }

    friend bool operator<(const Segment<N, Top>& lhs, const Segment<N, Top>& rhs) {
        return std::tie(lhs.start_, lhs.end_) < std::tie(rhs.start_, rhs.end_);
    }

    friend bool operator>(const Segment<N, Top>& lhs, const Segment<N, Top>& rhs) {
        return rhs < lhs;
    }

    friend bool operator<=(const Segment<N, Top>& lhs, const Segment<N, Top>& rhs) {
        return !(lhs > rhs);
    }

    friend bool operator>=(const Segment<N, Top>& lhs, const Segment<N, Top>& rhs) {
        return rhs <= lhs;
    }

    friend std::ostream& operator<<(std::ostream& os, const Segment<N, Top>& seg) {
        return os << seg.start_ << " -> " << seg.end_;
    }
};
}
