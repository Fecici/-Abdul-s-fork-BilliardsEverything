#pragma once

#include "general.hpp"

// Use overloading for the type, and template for the symbol

template <XY Symbol>
Coeff64 diff(const LinComArrZ<XYEta>& equation);

template <>
Coeff64 diff<XY::X>(const LinComArrZ<XYEta>& equation);

template <>
Coeff64 diff<XY::Y>(const LinComArrZ<XYEta>& equation);

template <XY Symbol>
LinComMapZ<Cos<LinComArrZ<XY>>> diff(const LinComMapZ<Sin<LinComArrZ<XY>>>& equation) {

    LinComMapZ<Cos<LinComArrZ<XY>>> builder{};

    // d/dx a*sin(y) = a*cos(y) * dy/dx
    for (const auto& kv : equation) {
        const auto& arg = kv.first.arg;

        // dy/dx
        const auto arg_coeff = arg.coeff<Symbol>();

        // a * dy/dx
        builder.add(kv.second * arg_coeff, Cos<LinComArrZ<XY>>{arg});
    }

    return builder;
}

template <XY Symbol>
LinComMapZ<Sin<LinComArrZ<XY>>> diff(const LinComMapZ<Cos<LinComArrZ<XY>>>& equation) {

    LinComMapZ<Sin<LinComArrZ<XY>>> builder{};

    // d/dx a*cos(y) = -a*sin(y) * dy/dx
    for (const auto& kv : equation) {
        const auto& arg = kv.first.arg;

        // dy/dx
        const auto arg_coeff = arg.coeff<Symbol>();

        // a * dy/dx
        builder.sub(kv.second * arg_coeff, Sin<LinComArrZ<XY>>{arg});
    }

    return builder;
}

// Rational numbers
// -----------------------------------------------------
LinComMapQ<Cos<LinComArrQ<XEta>>> diff(const LinComMapQ<Sin<LinComArrQ<XEta>>>& equation);

LinComMapQ<Sin<LinComArrQ<XEta>>> diff(const LinComMapQ<Cos<LinComArrQ<XEta>>>& equation);

LinComMapQ<Cos<LinComArrQ<YEta>>> diff(const LinComMapQ<Sin<LinComArrQ<YEta>>>& equation);

LinComMapQ<Sin<LinComArrQ<YEta>>> diff(const LinComMapQ<Cos<LinComArrQ<YEta>>>& equation);

// We need the gradient of all the boundary equations for
// - finding the intersection between the boundary and the cut curve
// - when there is an intersection, the cut curve will become a boundary
// curve, so we need its gradient
template <typename Symbols, typename Equation>
class EquationGradient;

template <typename Equation>
class EquationGradient<XY, Equation> final {
  public:
    Equation equation;
    decltype(diff<XY::X>(equation)) diff0;
    decltype(diff<XY::Y>(equation)) diff1;

    explicit EquationGradient(const Equation& eq)
        : equation{eq},
          diff0{diff<XY::X>(eq)},
          diff1{diff<XY::Y>(eq)} {
    }
};
