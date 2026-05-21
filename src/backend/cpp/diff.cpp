#include "diff.hpp"

template <>
Coeff64 diff<XY::X>(const LinComArrZ<XYEta>& equation) {
    return equation.coeff<XYEta::X>();
}

template <>
Coeff64 diff<XY::Y>(const LinComArrZ<XYEta>& equation) {
    return equation.coeff<XYEta::Y>();
}

// Rational numbers
// -----------------------------------------------------
LinComMapQ<Cos<LinComArrQ<XEta>>> diff(const LinComMapQ<Sin<LinComArrQ<XEta>>>& equation) {

    LinComMapQ<Cos<LinComArrQ<XEta>>> sum{};

    // d/dx a*sin(y) = a*cos(y) * dy/dx
    for (const auto& kv : equation) {
        const auto& arg = kv.first.arg;

        // dy/dx
        const auto arg_coeff = arg.coeff(XEta::X);

        // a * dy/dx
        sum.add(kv.second * arg_coeff, Cos<LinComArrQ<XEta>>{arg});
    }

    return sum;
}

LinComMapQ<Sin<LinComArrQ<XEta>>> diff(const LinComMapQ<Cos<LinComArrQ<XEta>>>& equation) {

    LinComMapQ<Sin<LinComArrQ<XEta>>> sum{};

    // d/dx a*cos(y) = -a*sin(y) * dy/dx
    for (const auto& kv : equation) {
        const auto& arg = kv.first.arg;

        // dy/dx
        const auto arg_coeff = arg.coeff(XEta::X);

        // a * dy/dx
        sum.sub(kv.second * arg_coeff, Sin<LinComArrQ<XEta>>{arg});
    }

    return sum;
}

LinComMapQ<Cos<LinComArrQ<YEta>>> diff(const LinComMapQ<Sin<LinComArrQ<YEta>>>& equation) {

    LinComMapQ<Cos<LinComArrQ<YEta>>> sum{};

    // d/dx a*sin(y) = a*cos(y) * dy/dx
    for (const auto& kv : equation) {
        const auto& arg = kv.first.arg;

        // dy/dx
        const auto arg_coeff = arg.coeff(YEta::Y);

        // a * dy/dx
        sum.add(kv.second * arg_coeff, Cos<LinComArrQ<YEta>>{arg});
    }

    return sum;
}

LinComMapQ<Sin<LinComArrQ<YEta>>> diff(const LinComMapQ<Cos<LinComArrQ<YEta>>>& equation) {

    LinComMapQ<Sin<LinComArrQ<YEta>>> sum{};

    // d/dx a*cos(y) = -a*sin(y) * dy/dx
    for (const auto& kv : equation) {
        const auto& arg = kv.first.arg;

        // dy/dx
        const auto arg_coeff = arg.coeff(YEta::Y);

        // a * dy/dx
        sum.sub(kv.second * arg_coeff, Sin<LinComArrQ<YEta>>{arg});
    }

    return sum;
}
