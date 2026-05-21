#pragma once

#include <ostream> // std::ostream

#include "interval.hpp"
#include "point.hpp"

namespace geometry {

template <typename N, Topology Top>
class Rectangle final {
  private:
    Interval<N, Top> interval_x_; // x-axis
    Interval<N, Top> interval_y_; // y-axis

  public:
    // All necessary checking is done within the interval classes
    Rectangle(const Interval<N, Top>& x, const Interval<N, Top>& y)
        : interval_x_{x}, interval_y_{y} {}

    const Interval<N, Top>& interval_x() const {
        return interval_x_;
    }

    const Interval<N, Top>& interval_y() const {
        return interval_y_;
    }

    Point<N> lower_left() const {
        return {interval_x_.lower(), interval_y_.lower()};
    }

    Point<N> lower_right() const {
        return {interval_x_.upper(), interval_y_.lower()};
    }

    Point<N> upper_left() const {
        return {interval_x_.lower(), interval_y_.upper()};
    }

    Point<N> upper_right() const {
        return {interval_x_.upper(), interval_y_.upper()};
    }

    N width() const {
        return interval_x_.length();
    }

    N height() const {
        return interval_y_.length();
    }

    Point<N> center() const {
        return {interval_x_.center(), interval_y_.center()};
    }

    bool is_square() const {
        return width() == height();
    }

    friend bool operator==(const Rectangle<N, Top>& lhs, const Rectangle<N, Top>& rhs) {
        return std::tie(lhs.interval_x_, lhs.interval_y_) == std::tie(rhs.interval_x_, rhs.interval_y_);
    }

    friend bool operator!=(const Rectangle<N, Top>& lhs, const Rectangle<N, Top>& rhs) {
        return !(lhs == rhs);
    }

    friend bool operator<(const Rectangle<N, Top>& lhs, const Rectangle<N, Top>& rhs) {
        return std::tie(lhs.interval_x_, lhs.interval_y_) < std::tie(rhs.interval_x_, rhs.interval_y_);
    }

    friend bool operator>(const Rectangle<N, Top>& lhs, const Rectangle<N, Top>& rhs) {
        return rhs < lhs;
    }

    friend bool operator<=(const Rectangle<N, Top>& lhs, const Rectangle<N, Top>& rhs) {
        return !(lhs > rhs);
    }

    friend bool operator>=(const Rectangle<N, Top>& lhs, const Rectangle<N, Top>& rhs) {
        return rhs <= lhs;
    }

    friend std::ostream& operator<<(std::ostream& os, const Rectangle<N, Top>& rect) {
        return os << '(' << rect.interval_x_ << ", " << rect.interval_y_ << ')';
    }
};
}
