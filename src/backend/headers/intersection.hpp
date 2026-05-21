#pragma once

#include "general.hpp"
#include "newton.hpp"

inline Sign curve_sign_at_point(const LinComArrZ<XYEta>& equation, const Vector2<Interval>& point) {
    const auto result = evalf<Interval>(equation, point[0], point[1]);
    return sign(result);
}

inline Sign curve_sign_at_point(const Equation<Sin>& equation, const Vector2<Interval>& point) {

    mpfi_t sum;
    mpfi_init2(sum, 168);

    mpfi_t a;
    mpfi_init2(a, 168);

    mpfi_t x;
    mpfi_init2(x, 168);

    mpfi_t y;
    mpfi_init2(y, 168);

    mpfi_set_si(sum, 0);

    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff<XY::X>();
        const auto y_coeff = arg.coeff<XY::Y>();

        mpfi_mul_si(x, point[0].backend().data(), x_coeff);
        mpfi_mul_si(y, point[1].backend().data(), y_coeff);

        mpfi_add(a, x, y);

        mpfi_sin(a, a);

        mpfi_mul_si(a, a, kv.second);

        mpfi_add(sum, sum, a);
    }

    mpfi_clear(a);
    mpfi_clear(x);
    mpfi_clear(y);

    if (mpfi_is_strictly_pos(sum)) {
        mpfi_clear(sum);
        return Sign::POS;
    }

    if (mpfi_is_strictly_neg(sum)) {
        mpfi_clear(sum);
        return Sign::NEG;
    }

    if (mpfi_has_zero(sum)) {
        mpfi_clear(sum);
        return Sign::ZERO;
    }

    throw std::runtime_error("unable to find sign for sum");
}

inline Sign curve_sign_at_point(const Equation<Cos>& equation, const Vector2<Interval>& point) {

    mpfi_t sum;
    mpfi_init2(sum, 168);

    mpfi_t a;
    mpfi_init2(a, 168);

    mpfi_t x;
    mpfi_init2(x, 168);

    mpfi_t y;
    mpfi_init2(y, 168);

    mpfi_set_si(sum, 0);

    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff<XY::X>();
        const auto y_coeff = arg.coeff<XY::Y>();

        mpfi_mul_si(x, point[0].backend().data(), x_coeff);
        mpfi_mul_si(y, point[1].backend().data(), y_coeff);

        mpfi_add(a, x, y);

        mpfi_cos(a, a);

        mpfi_mul_si(a, a, kv.second);

        mpfi_add(sum, sum, a);
    }

    mpfi_clear(a);
    mpfi_clear(x);
    mpfi_clear(y);

    if (mpfi_is_strictly_pos(sum)) {
        mpfi_clear(sum);
        return Sign::POS;
    }

    if (mpfi_is_strictly_neg(sum)) {
        mpfi_clear(sum);
        return Sign::NEG;
    }

    if (mpfi_has_zero(sum)) {
        mpfi_clear(sum);
        return Sign::ZERO;
    }

    throw std::runtime_error("unable to find sign for sum");
}

// p -> q, x is the query point
// dot(v, w) is proportional to cos(theta), where theta = angle between v and w
// cos(theta) < 0 for theta > 90, = 0 for theta = 90, and > 0 for theta < 90
inline int point_sign_line(const Vector2<Real>& p, const Vector2<Real>& q, const Vector2<Real>& x) {
    const Vector2<Real> v = q - p;
    const Vector2<Real> w = x - p;

    const Real dot = v.dot(w);

    const auto sign = dot.sign();

    return sign;
}

template <typename Symbols, typename T, typename S>
Vector2<Interval> intersection_unchecked(const EquationGradient<Symbols, T>& eq0, const EquationGradient<Symbols, S>& eq1, const Vector2<Real>& a, const Vector2<Real>& b) {

    // The midpoint between a and b is the inital approximation
    const Vector2<Real> init = (a + b) / 2;

    const auto inside = [&](const Vector2<Real>& x_trial) {
        // The point x_trial is inside the solution region if both of these are positive
        return point_sign_line(a, b, x_trial) > 0 && point_sign_line(b, a, x_trial) > 0;
    };

    // The Newton object must be temporary, since it takes references
    const auto x = Newton<Symbols, T, S, decltype(inside)>{eq0, eq1, inside}.solve(init);

    const Real fudge{"1e-45"};

    const Real x0_left = x[0] - fudge;
    const Real x0_right = x[0] + fudge;

    const Real x1_left = x[1] - fudge;
    const Real x1_right = x[1] + fudge;

    // use fudge to create a larger interval
    const Interval x0_interval{x0_left, x0_right};
    const Interval x1_interval{x1_left, x1_right};

    return {x0_interval, x1_interval};
}

// v is the point that goes through 0.
// We move in by a 1/4. If that doesn't work,
// we move in again.
template <typename Symbols, typename T, typename S>
Vector2<Interval> intersection_zero(const EquationGradient<Symbols, T>& eq0, const EquationGradient<Symbols, S>& eq1, const Vector2<Real>& v, const Vector2<Real>& w) {

    // Midpoint between v and w
    const Vector2<Real> midpoint = (v + w) / 2;

    // Check interval from midpoint to w
    // Most of the time the roots will be in this interval
    // We need to check this interval first for 1 1 23 1 2 1 25 1 1 48
    auto inter = intersection_unchecked(eq0, eq1, midpoint, w);

    auto eq0_sign = curve_sign_at_point(eq0.equation, inter);
    auto eq1_sign = curve_sign_at_point(eq1.equation, inter);

    if (eq0_sign == Sign::ZERO && eq1_sign == Sign::ZERO) {
        return inter;
    }

    // TODO we could use the straight line technique to get a heuristic
    // idea of where to start the root checking

    // Need for
    // 1 1 18 1 1 52 1 1 18 1 1 54 1 1 19 1 2 1 52 1 1 18 1 1 52 1 2 1 19 1 1 54

    const Vector2<Real> left_quarter = (v + midpoint) / 2;

    const Vector2<Real> right_quarter = (midpoint + w) / 2;

    inter = intersection_unchecked(eq0, eq1, left_quarter, right_quarter);

    eq0_sign = curve_sign_at_point(eq0.equation, inter);
    eq1_sign = curve_sign_at_point(eq1.equation, inter);

    if (eq0_sign == Sign::ZERO && eq1_sign == Sign::ZERO) {
        return inter;
    }

    // Now check between v_fudge and midpoint
    // Need this for
    // 1 1 1 1 2 1 5 2 3 1 2 1 3 2 5 1 2 1 1 1 1 4 1 1 2 1 1 5 2 4 2 5 1 1 2 1 1 4

    // Vector from v to w
    const auto direction = w - v;

    const auto fudge = direction / 10000.0;

    const auto v_fudge = v + fudge;

    inter = intersection_unchecked(eq0, eq1, v_fudge, midpoint);

    eq0_sign = curve_sign_at_point(eq0.equation, inter);
    eq1_sign = curve_sign_at_point(eq1.equation, inter);

    if (eq0_sign == Sign::ZERO && eq1_sign == Sign::ZERO) {
        return inter;
    }

    // The fallthrough case should always be an error case
    std::ostringstream err;
    err << "Signs " << eq0_sign << ", " << eq1_sign
        << " for:" << '\n'
        << eq0.equation << '\n'
        << eq1.equation << '\n'
        << "at intersection " << inter << '\n'
        << "between points " << v << ", " << w;

    throw std::runtime_error(err.str());
}

template <typename Symbols, typename T, typename S>
Vector2<Interval> intersection(const EquationGradient<Symbols, T>& eq0, const EquationGradient<Symbols, S>& eq1, const Vector2<Real>& v, const Vector2<Real>& w) {

    auto inter = intersection_unchecked(eq0, eq1, v, w);

    auto eq0_sign = curve_sign_at_point(eq0.equation, inter);
    auto eq1_sign = curve_sign_at_point(eq1.equation, inter);

    if (eq0_sign == Sign::ZERO && eq1_sign == Sign::ZERO) {
        return inter;
    }

    // 1 1 4 1 1 14 1 1 4 1 1 16 1 1 7 1 2 1 14 1 1 4 1 1 14 1 2 1 7 1 1 16

    // Find the midpoint, and now try using that
    const Vector2<Real> m = (v + w) / 2;

    // Try with v, m
    inter = intersection_unchecked(eq0, eq1, v, m);

    eq0_sign = curve_sign_at_point(eq0.equation, inter);
    eq1_sign = curve_sign_at_point(eq1.equation, inter);

    if (eq0_sign == Sign::ZERO && eq1_sign == Sign::ZERO) {
        return inter;
    }

    // Try with m, w
    inter = intersection_unchecked(eq0, eq1, m, w);

    eq0_sign = curve_sign_at_point(eq0.equation, inter);
    eq1_sign = curve_sign_at_point(eq1.equation, inter);

    if (eq0_sign == Sign::ZERO && eq1_sign == Sign::ZERO) {
        return inter;
    }

    // The fallthrough case should always be an error, if there is one
    std::ostringstream err;
    err << "Signs " << eq0_sign << ", " << eq1_sign
        << " for:" << '\n'
        << eq0.equation << '\n'
        << eq1.equation << '\n'
        << "at intersection " << inter << '\n'
        << "between points " << v << ", " << w;
    throw std::runtime_error(err.str());
}

// T is the type of curve
// S is the type of equation in the variant
// WARNING: always make instances of this class temporaries
template <typename Symbols, typename T>
class IntersectionVariant final : public boost::static_visitor<Vector2<Interval>> {
  private:
    const EquationGradient<Symbols, T>& curve;
    const Vector2<Real>& point0;
    const Vector2<Real>& point1;

  public:
    explicit IntersectionVariant(const EquationGradient<Symbols, T>& curve_, const Vector2<Real>& point0_, const Vector2<Real>& point1_)
        : curve{curve_},
          point0{point0_},
          point1{point1_} {
    }

    // This is the same for all the types in the variant. All we need is the type, and then
    // we can template the rest
    template <typename S>
    Vector2<Interval> operator()(const EquationGradient<Symbols, S>& eq_grad) const {
        return intersection(eq_grad, curve, point0, point1);
    }
};

template <typename Symbols, typename T>
class IntersectionZeroVariant final : public boost::static_visitor<Vector2<Interval>> {
  private:
    const EquationGradient<Symbols, T>& curve;
    const Vector2<Real>& point0;
    const Vector2<Real>& point1;

  public:
    explicit IntersectionZeroVariant(const EquationGradient<Symbols, T>& curve_, const Vector2<Real>& point0_, const Vector2<Real>& point1_)
        : curve{curve_},
          point0{point0_},
          point1{point1_} {
    }

    // This is the same for all the types in the variant. All we need is the type, and then
    // we can template the rest
    template <typename S>
    Vector2<Interval> operator()(const EquationGradient<Symbols, S>& eq_grad) const {
        return intersection_zero(eq_grad, curve, point0, point1);
    }
};
