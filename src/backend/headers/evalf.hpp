#pragma once

#include "general.hpp"
#include "trig_identities.hpp"
// One argument

template <typename N>
N evalf(const LinComArrQ<XEta>& equation, ParamType<N> x) {
    const auto x_coeff = equation.coeff(XEta::X);
    const auto eta_coeff = equation.coeff(XEta::Eta);

    return x_coeff * x + eta_coeff * boost::math::constants::half_pi<N>();
}

template <typename N>
N evalf(const LinComArrQ<YEta>& equation, ParamType<N> y) {
    const auto y_coeff = equation.coeff(YEta::Y);
    const auto eta_coeff = equation.coeff(YEta::Eta);

    return y_coeff * y + eta_coeff * boost::math::constants::half_pi<N>();
}

template <typename N>
N evalf(const LinComMapQ<Sin<LinComArrQ<XEta>>>& equation, ParamType<N> x) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff(XEta::X);
        const auto eta_coeff = arg.coeff(XEta::Eta);

        // implicit cast from integer to type N, which is usually float
        // if you change the type N, double triple check that all these
        // implicits casts and overloads work properly (like sin/cos and stuff)
        sum += N{kv.second} * sin(N{x_coeff} * x + N{eta_coeff} * boost::math::constants::half_pi<N>());
    }

    return sum;
}

template <typename N>
N evalf(const LinComMapQ<Cos<LinComArrQ<XEta>>>& equation, ParamType<N> x) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff(XEta::X);
        const auto eta_coeff = arg.coeff(XEta::Eta);

        // implicit cast from integer to type N, which is usually float
        // if you change the type N, double triple check that all these
        // implicits casts and overloads work properly (like sin/cos and stuff)
        sum += N{kv.second} * cos(N{x_coeff} * x + N{eta_coeff} * boost::math::constants::half_pi<N>());
    }

    return sum;
}

template <typename N>
N evalf(const LinComMapQ<Sin<LinComArrQ<YEta>>>& equation, ParamType<N> y) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto y_coeff = arg.coeff(YEta::Y);
        const auto eta_coeff = arg.coeff(YEta::Eta);

        // implicit cast from integer to type N, which is usually float
        // if you change the type N, double triple check that all these
        // implicits casts and overloads work properly (like sin/cos and stuff)
        sum += N{kv.second} * sin(N{y_coeff} * y + N{eta_coeff} * boost::math::constants::half_pi<N>());
    }

    return sum;
}

template <typename N>
N evalf(const LinComMapQ<Cos<LinComArrQ<YEta>>>& equation, ParamType<N> y) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto y_coeff = arg.coeff(YEta::Y);
        const auto eta_coeff = arg.coeff(YEta::Eta);

        // implicit cast from integer to type N, which is usually float
        // if you change the type N, double triple check that all these
        // implicits casts and overloads work properly (like sin/cos and stuff)
        sum += N{kv.second} * cos(N{y_coeff} * y + N{eta_coeff} * boost::math::constants::half_pi<N>());
    }

    return sum;
}

// We don't use the last two parameters for calculating this evalf
template <typename N>
N evalf(const Coeff64 value, ParamType<N>, ParamType<N>) {
    return N{value};
}

template <typename N>
N evalf(const LinComArrZ<XYEta>& equation, ParamType<N> x, ParamType<N> y) {
    const auto x_coeff = equation.coeff<XYEta::X>();
    const auto y_coeff = equation.coeff<XYEta::Y>();
    const auto eta_coeff = equation.coeff<XYEta::Eta>();

    return x_coeff * x + y_coeff * y + eta_coeff * boost::math::constants::half_pi<N>();
}

template <typename N>
N evalf(const LinComMapZ<Sin<LinComArrZ<XY>>>& equation, ParamType<N> x, ParamType<N> y) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff<XY::X>();
        const auto y_coeff = arg.coeff<XY::Y>();

        // implicit cast from integer to type N, which is usually float
        // if you change the type N, double triple check that all these
        // implicits casts and overloads work properly (like sin/cos and stuff)
        sum += kv.second * sin(x_coeff * x + y_coeff * y);
    }

    return sum;
}

template <typename N>
N evalf(const LinComMapZ<Cos<LinComArrZ<XY>>>& equation, ParamType<N> x, ParamType<N> y) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff<XY::X>();
        const auto y_coeff = arg.coeff<XY::Y>();

        sum += kv.second * cos(x_coeff * x + y_coeff * y);
    }

    return sum;
}

template <typename N>
N evalf(const LinComVecZ<Sin<LinComArrZ<XY>>>& equation, ParamType<N> x, ParamType<N> y) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff<XY::X>();
        const auto y_coeff = arg.coeff<XY::Y>();

        // implicit cast from integer to type N, which is usually float
        // if you change the type N, double triple check that all these
        // implicits casts and overloads work properly (like sin/cos and stuff)
        sum += kv.second * sin(x_coeff * x + y_coeff * y);
    }

    return sum;
}

template <typename N>
N evalf(const LinComVecZ<Cos<LinComArrZ<XY>>>& equation, ParamType<N> x, ParamType<N> y) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff<XY::X>();
        const auto y_coeff = arg.coeff<XY::Y>();

        sum += kv.second * cos(x_coeff * x + y_coeff * y);
    }

    return sum;
}

template <typename N>
N bound(const LinComMapZ<Sin<LinComArrZ<XY>>>& equation) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff<XY::X>();
        const auto y_coeff = arg.coeff<XY::Y>();
        sum += std::abs(kv.second) * (std::abs(x_coeff) + std::abs(y_coeff));
    }

    return sum;
}

template <typename N>
N bound(const LinComMapZ<Cos<LinComArrZ<XY>>>& equation) {

    N sum{0};
    for (const auto& kv : equation) {

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff<XY::X>();
        const auto y_coeff = arg.coeff<XY::Y>();
        sum += std::abs(kv.second) * (std::abs(x_coeff) + std::abs(y_coeff));
    }

    return sum;
}

static LinComMapZ<Cos<LinComArrZ<XY>>> multiply_square(const LinComMapZ<Cos<LinComArrZ<XY>>>& equation) {

    const auto& result =multiply_lin_com(equation, equation);
    return result;
}

static LinComMapZ<Cos<LinComArrZ<XY>>> multiply_cubic(const LinComMapZ<Cos<LinComArrZ<XY>>>& equation) {

    const auto& inter =multiply_lin_com(equation, equation);
    const auto& result = multiply_lin_com(equation, inter);

    return result;
}

static LinComMapZ<Cos<LinComArrZ<XY>>> multiply_square(const LinComMapZ<Sin<LinComArrZ<XY>>>& equation) {

    const auto& result =multiply_lin_com(equation, equation);
    return result;
}

static LinComMapZ<Sin<LinComArrZ<XY>>> multiply_cubic(const LinComMapZ<Sin<LinComArrZ<XY>>>& equation) {

    const auto& inter =multiply_lin_com(equation, equation);
    const auto& result = multiply_lin_com( inter,equation);

    return result;
}
