#pragma once

#include "general.hpp"
#include "gradient.hpp"
#include "newton.hpp"

// For now, I'm going to throw the symbols and other associated things for the bezier stuff
// in those files. That way it's out of the way, but still there if I need it.

enum class XYOne : size_t {
    X,
    Y,
    One,
};

template <>
struct Enum<XYOne> final {
    static constexpr size_t size = 3;
};

std::ostream& operator<<(std::ostream& os, const XYOne sym) {
    switch (sym) {
    case XYOne::X:
        return os << 'x';
    case XYOne::Y:
        return os << 'y';
    case XYOne::One:
        return os << '1';
    }

    throw std::runtime_error(invalid_enum_value("XYOne", sym));
}

template <XY Symbol>
Real diff(const LinComArrR<XYOne>& equation);

template <>
Real diff<XY::X>(const LinComArrR<XYOne>& equation) {
    return equation.coeff<XYOne::X>();
}

template <>
Real diff<XY::Y>(const LinComArrR<XYOne>& equation) {
    return equation.coeff<XYOne::Y>();
}

// XYOne
template <typename N>
N evalf(const LinComArrR<XYOne>& equation, ParamType<N> x, ParamType<N> y) {
    const auto& x_coeff = equation.coeff<XYOne::X>();
    const auto& y_coeff = equation.coeff<XYOne::Y>();
    const auto& one_coeff = equation.coeff<XYOne::One>();

    return x_coeff * x + y_coeff * y + one_coeff;
}

template <typename N>
N evalf(const Real& value, ParamType<N>, ParamType<N>) {
    return value;
}

// So, given four points, we can form the bezier curve from those points.

// Given a line L = p + t*v, convert it into an equation ax + by + c = 0
LinComArrR<XYOne> parametric_to_implicit(const Vector2<Real>& p, const Vector2<Real>& v) {

    const Real& x_coeff = v.coord1;
    const Real& y_coeff = -v.coord0;
    const Real& one_coeff = v.coord0 * p.coord1 - p.coord0 * v.coord1;

    return LinComArrR<XYOne>{x_coeff, y_coeff, one_coeff};
}

template <typename T, typename S>
Vector2<Real> intersection_line(const EquationGradient<XY, T>& eq0, const EquationGradient<XY, S>& eq1, const Vector2<Real>& tmp) {

    const Vector2<Real> init{tmp.coord0, tmp.coord1};

    // TODO do the math properly so we can say the root always has to be on one side of the
    // line ab (due to convexity)
    const auto inside = [](const Vector2<Real>&) {
        return true;
    };

    // The Newton object must be temporary, since it takes references
    const auto x = Newton<XY, T, S, decltype(inside)>{eq0, eq1, inside}.solve(init);

    // TODO do some zero checking
    /*
    const Real fudge{"1e-45"};

    const Real x0_left = x[0] - fudge;
    const Real x0_right = x[0] + fudge;

    const Real x1_left = x[1] - fudge;
    const Real x1_right = x[1] + fudge;

    // use fudge to create a larger interval
    const Interval x0_interval{x0_left, x0_right};
    const Interval x1_interval{x1_left, x1_right};

    const Vector2<Interval> inter{x0_interval, x1_interval};
    */

    return x;
}

template <typename T>
std::pair<Vector2<Real>, Vector2<Real>> cubic_bezier_interp(const Vector2<Real>& a, const EquationGradient<XY, T>& eq_grad, const Vector2<Real>& b) {

    // Vector from a -> b
    const auto ab = b - a;
    // Rotate ab by 90 degrees (it doesn't matter which way)
    const Vector2<Real> dir{-ab.coord1, ab.coord0};

    const auto step = ab / 3.0;

    const auto point13 = a + step;

    const auto line13 = parametric_to_implicit(point13, dir);
    const EquationGradient<XY, LinComArrR<XYOne>> eq_grad13{line13};

    const auto y1 = intersection_line(eq_grad, eq_grad13, point13);

    const auto point23 = b - step;
    const auto line23 = parametric_to_implicit(point23, dir);

    const EquationGradient<XY, LinComArrR<XYOne>> eq_grad23{line23};
    const auto y2 = intersection_line(eq_grad, eq_grad23, point23);

    const Vector2<Real> y0{a.coord0, a.coord1};
    const Vector2<Real> y3{b.coord0, b.coord1};

    // https://web.archive.org/web/20131225210855/http://people.sc.fsu.edu/~jburkardt/html/bezier_interpolation.html
    // P0 =      y0
    // P1 = ( -5 y0 + 18 y1 -  9 y2 + 2 y3 ) / 6
    // P2 = (  2 y0 -  9 y1 + 18 y2 - 5 y3 ) / 6
    // P3 =                             y3

    const Vector2<Real> p1 = (-5 * y0 + 18 * y1 - 9 * y2 + 2 * y3) / 6.0;
    const Vector2<Real> p2 = (2 * y0 - 9 * y1 + 18 * y2 - 5 * y3) / 6.0;

    return {p1, p2};
}
