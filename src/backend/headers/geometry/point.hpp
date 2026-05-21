#pragma once

#include <ostream>   // std::ostream
#include <stdexcept> // std::runtime_error
#include <tuple>     // std::tie

namespace geometry {

template <typename N>
class Point final {

  public:
    N x;
    N y;

    Point(const N& x_, const N& y_)
        : x{x_}, y{y_} {}

    void operator+=(const Point<N>& point) {
        x += point.x;
        y += point.y;
    }

    void operator-=(const Point<N>& point) {
        x -= point.x;
        y -= point.y;
    }

    void operator*=(const N& scale) {
        x *= scale;
        y *= scale;
    }

    void operator/=(const N& scale) {

        if (scale == 0) {
            throw std::runtime_error("division by zero in Point::operator/=");
        }

        x /= scale;
        y /= scale;
    }

    // These could go outside, but I think it is nicer if it is inside
    friend Point<N> operator+(Point<N> lhs, const Point<N>& rhs) {
        lhs += rhs;
        return lhs;
    }

    friend Point<N> operator-(Point<N> lhs, const Point<N>& rhs) {
        lhs -= rhs;
        return lhs;
    }

    friend Point<N> operator*(Point<N> point, const N& scale) {
        point *= scale;
        return point;
    }

    friend Point<N> operator/(Point<N> point, const N& scale) {

        if (scale == 0) {
            throw std::runtime_error("division by zero in Point::operator/");
        }

        point /= scale;
        return point;
    }

    friend bool operator==(const Point<N>& lhs, const Point<N>& rhs) {
        return std::tie(lhs.x, lhs.y) == std::tie(rhs.x, rhs.y);
    }

    friend bool operator!=(const Point<N>& lhs, const Point<N>& rhs) {
        return !(lhs == rhs);
    }

    friend bool operator<(const Point<N>& lhs, const Point<N>& rhs) {
        return std::tie(lhs.x, lhs.y) < std::tie(rhs.x, rhs.y);
    }

    friend bool operator>(const Point<N>& lhs, const Point<N>& rhs) {
        return rhs < lhs;
    }

    friend bool operator<=(const Point<N>& lhs, const Point<N>& rhs) {
        return !(lhs > rhs);
    }

    friend bool operator>=(const Point<N>& lhs, const Point<N>& rhs) {
        return rhs <= lhs;
    }

    friend std::ostream& operator<<(std::ostream& os, const Point<N>& point) {
        return os << '(' << point.x << ", " << point.y << ')';
    }

    static N dot(const Point<N>& u, const Point<N>& v) {
        return u.x * v.x + u.y * v.y;
    }

    // z component of u x v, where u and v are extended to three dimensions
    static N cross(const Point<N>& u, const Point<N>& v) {
        return u.x * v.y - u.y * v.x;
    }

    // Given three points u, v, w, this function returns their orientation.
    // (Note that this is equal to twice the signed area of the triangle uvw)
    // This is the shoelace formula for the special case of a triangle
    // The result is
    // +1 for CCW
    //  0 for collinear
    // -1 for CW
    static int orientation(const Point<N>& u, const Point<N>& v, const Point<N>& w) {

        const N a = (v.x - u.x) * (w.y - u.y);
        const N b = (w.x - u.x) * (v.y - u.y);

        const auto order = a.compare(b);

        // Get the signum
        return (order > 0) - (order < 0);
    }
};
}
