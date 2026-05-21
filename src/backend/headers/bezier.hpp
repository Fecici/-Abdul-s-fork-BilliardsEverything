#pragma once

#include "general.hpp"
#include "gradient.hpp"
#include "newton.hpp"
#include "subs.hpp"

enum class RSOne : size_t {
    R,
    S,
    One,
};

template <>
struct Enum<RSOne> final {
    static constexpr size_t size = 3;
};

std::ostream& operator<<(std::ostream& os, const RSOne sym) {
    switch (sym) {
    case RSOne::R:
        return os << 'r';
    case RSOne::S:
        return os << 's';
    case RSOne::One:
        return os << '1';
    }

    throw std::runtime_error(invalid_enum_value("RSOne", sym));
}

template <RSOne Symbol>
LinComR<Cos<EnumComR<RSOne>>> diff(const LinComR<Sin<EnumComR<RSOne>>>& equation) {

    LinComBuilderR<Cos<EnumComR<RSOne>>> builder;

    // d/dx a*sin(y) = -a*cos(y) * dy/dx
    for (const auto& kv : equation) {
        const auto& arg = kv.first.get_arg();

        // dy/dx
        const auto arg_coeff = arg.coeff<Symbol>();

        // a * dy/dx
        builder.add(kv.second * arg_coeff, Cos<EnumComR<RSOne>>{arg});
    }

    return builder.build();
}

template <RSOne Symbol>
LinComR<Sin<EnumComR<RSOne>>> diff(const LinComR<Cos<EnumComR<RSOne>>>& equation) {

    LinComBuilderR<Sin<EnumComR<RSOne>>> builder;

    // d/dx a*cos(y) = -a*sin(y) * dy/dx
    for (const auto& kv : equation) {
        const auto& arg = kv.first.get_arg();

        // dy/dx
        const auto arg_coeff = arg.coeff<Symbol>();

        // a * dy/dx
        builder.sub(kv.second * arg_coeff, Sin<EnumComR<RSOne>>{arg});
    }

    return builder.build();
}

template <typename Equation>
class EquationGradient<RSOne, Equation> final {
  public:
    Equation equation;
    decltype(diff<RSOne::R>(equation)) diff0;
    decltype(diff<RSOne::S>(equation)) diff1;

    explicit EquationGradient(const Equation& eq)
        : equation{eq},
          diff0{diff<RSOne::R>(eq)},
          diff1{diff<RSOne::S>(eq)} {
    }
};

// RSOne

template <typename N>
N evalf(const LinComR<Sin<EnumComR<RSOne>>>& equation, const N& r, const N& s) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.get_arg();
        const auto& r_coeff = arg.coeff<RSOne::R>();
        const auto& s_coeff = arg.coeff<RSOne::S>();
        const auto& one_coeff = arg.coeff<RSOne::One>();

        // implicit cast from integer to type N, which is usually float
        // if you change the type N, double triple check that all these
        // implicits casts and overloads work properly (like sin/cos and stuff)
        sum += kv.second * sin(r_coeff * r + s_coeff * s + one_coeff);
    }

    return sum;
}

template <typename N>
N evalf(const LinComR<Cos<EnumComR<RSOne>>>& equation, const N& r, const N& s) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.get_arg();
        const auto& r_coeff = arg.coeff<RSOne::R>();
        const auto& s_coeff = arg.coeff<RSOne::S>();
        const auto& one_coeff = arg.coeff<RSOne::One>();

        sum += kv.second * cos(r_coeff * r + s_coeff * s + one_coeff);
    }

    return sum;
}

// TODO replace Vector2 with an eigen vector. It is silly to have two classe for that.
// TODO get rid of the builder classes, and make things public again

// Bezier interpolation
// https://web.archive.org/web/20131225210855/http://people.sc.fsu.edu/~jburkardt/html/bezier_interpolation.html

// Tells if two vectors are parallel. To do this, we look at the determinant of the matrix
// formed of [a b]
bool parallel(const Vector2<Interval>& a, const Vector2<Interval>& b) {
    const Interval det = a.coord0 * b.coord1 - a.coord1 * b.coord0;
    const auto det_sign = sign(det);

    return det_sign == Sign::ZERO;
}

// Given two lines L0 = p0 + r*v0, L1 = p1 + s*v1, find the intersection (r, s), or return
// none if there is no solution (system is indeterminate)
Vector2<Real> parametric_intersection(const Vector2<Real>& p0, const Vector2<Real>& v0, const Vector2<Real>& p1, const Vector2<Real>& v1) {

    // L0 = L1
    // => p0 + r*v0 = p1 * s*v1
    // => r*v0 - s*v1 = p1 - p0
    // => [v0 -v1] * [r s] = p1 - p0
    // a = [v0, -v1]
    Matrix2<Real> a;
    a(0, 0) = v0[0];
    a(1, 0) = v0[1];
    a(0, 1) = Real{-v1[0]};
    a(1, 1) = Real{-v1[1]};

    const auto lu = a.fullPivLu();

    const Vector2<Real> b = p1 - p0;

    // a * [r, s] = b
    const Vector2<Real> soln = lu.solve(b);

    return soln;
}

// B(1/3) = 4/9 * g0 * r + 2/9 * g3 * s + 20/27 * p0 + 7/27 * p3
EnumComR<RSOne> calc_b13x(const Vector2<Real>& p0, const Vector2<Real>& g0, const Vector2<Real>& p3, const Vector2<Real>& g3) {

    EnumComBuilderR<RSOne> sum{};

    Real coeff;

    // 4/9 * g0 * r
    coeff = g0.coord0;
    coeff *= Rational{4, 9};

    sum.add(coeff, RSOne::R);

    // 2/9 * g3 * s
    coeff = g3.coord0;
    coeff *= Rational{2, 9};

    sum.add(coeff, RSOne::S);

    // 20/27 * p0 * One
    coeff = p0.coord0;
    coeff *= Rational{20, 27};

    sum.add(coeff, RSOne::One);

    // 7/27 * p3 * One
    coeff = p3.coord0;
    coeff *= Rational{7, 27};

    sum.add(coeff, RSOne::One);

    return sum.build();
}

// TODO find some way of templating this
// B(1/3) = 4/9 * g0 * r + 2/9 * g3 * s + 20/27 * p0 + 7/27 * p3
EnumComR<RSOne> calc_b13y(const Vector2<Real>& p0, const Vector2<Real>& g0, const Vector2<Real>& p3, const Vector2<Real>& g3) {

    EnumComBuilderR<RSOne> sum{};

    Real coeff;

    // 4/9 * g0 * r
    coeff = g0.coord1;
    coeff *= Rational{4, 9};

    sum.add(coeff, RSOne::R);

    // 2/9 * g3 * s
    coeff = g3.coord1;
    coeff *= Rational{2, 9};

    sum.add(coeff, RSOne::S);

    // 20/27 * p0 * One
    coeff = p0.coord1;
    coeff *= Rational{20, 27};

    sum.add(coeff, RSOne::One);

    // 7/27 * p3 * One
    coeff = p3.coord1;
    coeff *= Rational{7, 27};

    sum.add(coeff, RSOne::One);

    return sum.build();
}

// B(2/3) = 2/9 * g0 * r + 4/9 * g3 * s + 7/27 * p0 + 20/27 * p3
EnumComR<RSOne> calc_b23x(const Vector2<Real>& p0, const Vector2<Real>& g0, const Vector2<Real>& p3, const Vector2<Real>& g3) {

    EnumComBuilderR<RSOne> sum{};

    Real coeff;

    // 2/9 * g0 * r
    coeff = g0.coord0;
    coeff *= Rational{2, 9};

    sum.add(coeff, RSOne::R);

    // 4/9 * g3 * s
    coeff = g3.coord0;
    coeff *= Rational{4, 9};

    sum.add(coeff, RSOne::S);

    // 7/27 * p0 * One
    coeff = p0.coord0;
    coeff *= Rational{7, 27};

    sum.add(coeff, RSOne::One);

    // 20/27 * p3 * One
    coeff = p3.coord0;
    coeff *= Rational{20, 27};

    sum.add(coeff, RSOne::One);

    return sum.build();
}

// B(2/3) = 2/9 * g0 * r + 4/9 * g3 * s + 7/27 * p0 + 20/27 * p3
EnumComR<RSOne> calc_b23y(const Vector2<Real>& p0, const Vector2<Real>& g0, const Vector2<Real>& p3, const Vector2<Real>& g3) {

    EnumComBuilderR<RSOne> sum{};

    Real coeff;

    // 2/9 * g0 * r
    coeff = g0.coord1;
    coeff *= Rational{2, 9};

    sum.add(coeff, RSOne::R);

    // 4/9 * g3 * s
    coeff = g3.coord1;
    coeff *= Rational{4, 9};

    sum.add(coeff, RSOne::S);

    // 7/27 * p0 * One
    coeff = p0.coord1;
    coeff *= Rational{7, 27};

    sum.add(coeff, RSOne::One);

    // 20/27 * p3 * One
    coeff = p3.coord1;
    coeff *= Rational{20, 27};

    sum.add(coeff, RSOne::One);

    return sum.build();
}

template <typename Symbols, typename T>
Vector2<Real> intersection_box(const EquationGradient<Symbols, T>& eq0, const EquationGradient<Symbols, T>& eq1, const Vector2<Real>& max) {

    // We take the center of the box as the initial approximation
    const Vector2<Real> init = 3.0 * max / 4.0;

    const auto inside = [&](const Vector2<Real>& x_trial) {
        // x_trial is in [0, max] for each coord
        return 0 <= x_trial[0] && x_trial[0] <= max[0] &&
               0 <= x_trial[1] && x_trial[1] <= max[1];
    };

    // The Newton object must be temporary, since it takes references
    // Sometimes, as in the 1 2 2 3 3 case, we don't want to reduce the norm. The equations
    // can get rather bumpy in the solution box, and getting over those bumps requires increasing
    // the norm. However, this is safe to do, since we must still stay inside the solution box,
    // and I'm pretty sure there will only ever be one root inside that box.
    // The equations are very bumpy, and the root finder gets lost
    const auto x = Newton<Symbols, T, T, decltype(inside)>{eq0, eq1, inside, true}.solve(init);

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
    // TODO do some zero checking

    return Vector2<Real>{x[0], x[1]};
}

// even number of flips, same orientation
// odd number, different orientation

template <typename T>
std::pair<Vector2<Real>, Vector2<Real>> cubic_bezier(const Vector2<Interval>& a, const EquationGradient<XY, T>& eq_grad, const Vector2<Interval>& b, const bool same_orient) {

    // TODO should we cache all these gradients so we don't have to calculate them each time?
    const auto a_gradient = gradient(eq_grad, a);
    const auto a_dir = same_orient ? Vector2<Interval>{a_gradient.coord1, -a_gradient.coord0} : Vector2<Interval>{-a_gradient.coord1, a_gradient.coord0}; // Rotate 90 CW or CCW

    const auto b_gradient = gradient(eq_grad, b);
    const auto b_dir = same_orient ? Vector2<Interval>{-b_gradient.coord1, b_gradient.coord0} : Vector2<Interval>{b_gradient.coord1, -b_gradient.coord0}; // Rotate 90 CCW or CW

    const auto p0_tmp = median(a);
    const auto p3_tmp = median(b);

    const Vector2<Real> p0{p0_tmp.coord0, p0_tmp.coord1};
    const Vector2<Real> p3{p3_tmp.coord0, p3_tmp.coord1};

    //std::cout << eq_grad.equation << std::endl;
    //std::cout << a << std::endl;
    //std::cout << a_dir << std::endl;

    //std::cout << b << std::endl;
    //std::cout << b_dir << std::endl;

    if (parallel(a_dir, b_dir)) {

        // The equation is actually a straight line, so we return
        // two points 1/3 and 2/3 of the way between a and b

        // Vector 1/3 of the way from p0 to p3
        const auto diff = (p3 - p0) / 3.0;

        const auto p1 = p0 + diff;
        const auto p2 = p3 - diff;
        return {p1, p2};

    } else {

        // is not a straight line
        const auto g0_tmp = median(a_dir);
        const auto g3_tmp = median(b_dir);

        const Vector2<Real> g0{g0_tmp.coord0, g0_tmp.coord1};
        const Vector2<Real> g3{g3_tmp.coord0, g3_tmp.coord1};

        const auto b13x = calc_b13x(p0_tmp, g0_tmp, p3_tmp, g3_tmp);
        const auto b13y = calc_b13y(p0_tmp, g0_tmp, p3_tmp, g3_tmp);

        const auto b23x = calc_b23x(p0_tmp, g0_tmp, p3_tmp, g3_tmp);
        const auto b23y = calc_b23y(p0_tmp, g0_tmp, p3_tmp, g3_tmp);

        //std::cout << b13x << std::endl;
        //std::cout << b13y << std::endl;
        //std::cout << b23x << std::endl;
        //std::cout << b23y << std::endl;

        // f(B(1/3)) = 0
        const auto fb13 = subs(eq_grad.equation, b13x, b13y);

        // f(B(2/3))
        const auto fb23 = subs(eq_grad.equation, b23x, b23y);

        std::cout << fb13 << std::endl;
        std::cout << fb23 << std::endl;

        const EquationGradient<RSOne, std::remove_const_t<decltype(fb13)>> eq_grad_fb13{fb13};
        const EquationGradient<RSOne, std::remove_const_t<decltype(fb23)>> eq_grad_fb23{fb23};

        // L1 = a + r*a_dir
        // L2 = b + s*b_dir
        const auto max = parametric_intersection(p0, g0, p3, g3);
        std::cout << "max = " << std::endl;
        std::cout << max << std::endl;

        // We want to find (r, s) such that fb13 = 0 and fb23 = 0
        // We need to do this within the box 0 <= r <= inter.r and
        // 0 <= s <= inter.s
        const auto inter = intersection_box(eq_grad_fb13, eq_grad_fb23, max);

        const Vector2<Real> p1 = p0 + inter.coord0 * g0;
        const Vector2<Real> p2 = p3 + inter.coord1 * g3;

        return {p1, p2};
    }
}
