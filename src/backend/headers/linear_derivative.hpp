#pragma once

#include "evalf.hpp"
#include "general.hpp"

#if 0

// maybe take out a minus sign, and normalize the eta cofficient somehow?
// (like muliples of pi or 2pi for example?)
template <typename Trig, typename Eta>
std::pair<Coeff64, Trig<LinComArrZ<XY>>> simplify_trig_eta_q(const LinComArrQ<Eta>& arg);

// take out a minus sign if necessary
// return 0 if arg is 0
template <typename Trig>
std::pair<Coeff64, Sin<LinComArrZ<XY>>> simplify_sin_xy(const LinComArrZ<XY>& arg) {

    const auto x_coeff = arg.coeff<XY::X>();
    const auto y_coeff = arg.coeff<XY::Y>();

    const auto leading_coeff = (x_coeff != 0) ? x_coeff : y_coeff;

    const auto sign = signum(leading_coeff);

    auto new_arg = arg;
    new_arg.scale(sign);

    const Sin<LinComArrZ<XY>> sin{new_arg};

    // sin(-x) = -sin(x)
    return {sign, sin};
}

// Take out a minus sign
// If the eta coeff is an integer, we could simplify that too,
// but I don't think it's worth it right now
std::pair<Coeff64, Cos<LinComArrQ<Xeta>>> simplify_cos_xy(const LinComArrQ<XEta>& arg) {

    const auto x_coeff = arg.coeff(XEta::X);
    const auto eta_coeff = arg.coeff(XEta::Eta);

    const auto leading_coeff = (x_coeff != 0) ? x_coeff : eta_coeff;

    const auto sign = signum(leading_coeff);

    auto new_arg = arg;
    new_arg.scale(sign);

    // cos(-x) = cos(x)
    const Cos<LinComArrQ<XY>> cos{new_arg};

    return cos;
}
#endif

// Unfortunately, alias templates cannot be specialized,
// and I'm not aware of a cleaner way of doing this
template <XY Symbol>
struct Eta final {};

template <>
struct Eta<XY::X> final {
    using Type = XEta;
};

template <>
struct Eta<XY::Y> final {
    using Type = YEta;
};

template <XY Symbol>
LinComArrQ<typename Eta<Symbol>::Type> in_terms_of(const LinComArrZ<XY>& lin_com, const LinComArrZ<XYEta>& constraint);

template <>
LinComArrQ<XEta> in_terms_of<XY::X>(const LinComArrZ<XY>& lin_com, const LinComArrZ<XYEta>& constraint);

template <>
LinComArrQ<YEta> in_terms_of<XY::Y>(const LinComArrZ<XY>& lin_com, const LinComArrZ<XYEta>& constraint);

template <XY Symbol, template <typename> class Trig>
LinComMapQ<Trig<LinComArrQ<typename Eta<Symbol>::Type>>> in_terms_of(const LinComMapZ<Trig<LinComArrZ<XY>>>& equation, const LinComArrZ<XYEta>& line) {

    using EtaSymbol = typename Eta<Symbol>::Type;

    LinComMapQ<Trig<LinComArrQ<EtaSymbol>>> sum{};
    for (const auto& kv : equation) {
        const auto& trig_arg = kv.first.arg;
        const auto trig_coeff = kv.second;

        const auto trig_arg_eta = in_terms_of<Symbol>(trig_arg, line);
        //const auto trig = simplify_trig(trig_xeta);
        Trig<LinComArrQ<EtaSymbol>> trig_eta{trig_arg_eta};

        sum.add(trig_coeff, trig_eta);
    }

    return sum;
}

inline Sign negate_sign(const Sign sign) {
    switch (sign) {
    case Sign::NEG:
        return Sign::POS;
    case Sign::ZERO:
        return Sign::ZERO;
    case Sign::POS:
        return Sign::NEG;
    }
    throw std::runtime_error(invalid_enum_value("Sign", sign));
}

template <template <typename> class Trig, typename T>
Sign linear_derivative_sign(const LinComMapQ<Trig<LinComArrQ<T>>>& equation, const Interval& point, const bool increasing) {

    // Same type as equation
    auto diff_same = equation;

    // Different type than equation
    decltype(diff(equation)) diff_different{};

    constexpr uint32_t max_iters = 10;
    for (uint32_t n = 1; n < max_iters; ++n) {

        // We now need to find the nth derivative of eq, and evaluate at the point
        if (n % 2 == 0) {
            diff_same = diff(diff_different);

            const auto eval = evalf<Interval>(diff_same, point);
            const auto s = sign(eval);

            // The derivative gives us the sign in the increasing direction
            // However, because this is an even derivative, the sign is the same
            // in both directions
            if (s != Sign::ZERO) {
                return s;
            }

        } else {
            diff_different = diff(diff_same);

            const auto eval = evalf<Interval>(diff_different, point);
            const auto s = sign(eval);

            // The derivative gives us the sign in the increasing direction
            // If we have an odd derivative, then it is the opposite sign
            // in the opposite direction

            if (s != Sign::ZERO) {
                // odd derivative, so we need to take into account the direction
                if (increasing) {
                    // the
                    return s;
                } else {
                    return negate_sign(s);
                }
            }
        }
    }

    std::ostringstream err{};
    err << "unable to calculate linear derivative sign for\n"
        << equation << '\n'
        << "at " << point << '\n';

    if (increasing) {
        err << "in increasing direction";
    } else {
        err << "in decreasing direction";
    }

    throw std::runtime_error(err.str());
}

inline bool is_increasing(const Order order) {
    switch (order) {
    case Order::Less:
        return false;
    case Order::Equal:
        throw std::runtime_error("cannot have equal order in is_increasing");
    case Order::Greater:
        return true;
    }
    throw std::runtime_error(invalid_enum_value("Order", order));
}

// we need to find the sign of the curve at a along the line in the direction of b
template <template <typename> class Trig>
Sign linear_derivative_sign(const LinComMapZ<Trig<LinComArrZ<XY>>>& curve, const LinComArrZ<XYEta>& line, const Vector2<Interval>& a, const Vector2<Interval>& b) {

    // for now, we will always assume that x and y are both non zero. Then we solve for y, and substititute that in
    // for x. We will will also check to see if there are any points aside from (0, 0) or the pi/2 ones that happen
    const auto y_coeff = line.coeff(XYEta::Y);

    // TODO maybe eliminate the variable with the smallest absolute value coefficient?
    // That might work
    if (y_coeff == 0) {
        // solve for x, and rewrite with y
        const auto eq = in_terms_of<XY::Y>(curve, line);
        // if b[1] < a[1], then we are moving in the direction as y decreases (and v.v)
        const auto sign = compare_interval(b[1], a[1]);

        const auto increasing = is_increasing(sign);

        return linear_derivative_sign(eq, a[1], increasing);
    } else {
        // solve for y, and rewrite with x
        const auto eq = in_terms_of<XY::X>(curve, line);
        // if b[0] < a[0], then we are moving in the direction as x decreases (and v.v)
        const auto sign = compare_interval(b[0], a[0]);

        const auto increasing = is_increasing(sign);

        return linear_derivative_sign(eq, a[0], increasing);
    }
}
