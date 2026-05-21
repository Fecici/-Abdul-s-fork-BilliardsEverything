#include "linear_derivative.hpp"

template <>
LinComArrQ<XEta> in_terms_of<XY::X>(const LinComArrZ<XY>& lin_com, const LinComArrZ<XYEta>& constraint) {

    // a x + b y + c eta = 0
    // so y = -a/b x - c/b eta
    // Then we have dx + ey is the lin_com
    // So lin_com = dx + e(-a/b x - c/b eta)
    //            = dx - ea/b x - ec/b eta
    //            = (db - ea)/b x - ec/b eta

    // In C++17, we can simply use a structured binding to do this
    const auto a = constraint.coeff(XYEta::X);
    const auto b = constraint.coeff(XYEta::Y);
    const auto c = constraint.coeff(XYEta::Eta);

    const auto d = lin_com.coeff(XY::X);
    const auto e = lin_com.coeff(XY::Y);

    // Remember that you can't initialize with the b directly,
    const Rational x_coeff = Rational{d * b - e * a} / b;
    const Rational eta_coeff = Rational{-e * c} / b;

    return LinComArrQ<XEta>{x_coeff, eta_coeff};
}

template <>
LinComArrQ<YEta> in_terms_of<XY::Y>(const LinComArrZ<XY>& lin_com, const LinComArrZ<XYEta>& constraint) {

    // a x + b y + c eta = 0
    // so x = -b/a y - c/a eta
    // Then we have d x + e y is the lin_com
    // So lin_com = d(-b/a y - c/a eta) + e y
    //            = -db/a y - dc/a eta + e y
    //            = (ea - db)/a y - dc/a eta

    // In C++17, we can simply use a structured binding to do this
    const auto a = constraint.coeff(XYEta::X);
    const auto b = constraint.coeff(XYEta::Y);
    const auto c = constraint.coeff(XYEta::Eta);

    const auto d = lin_com.coeff(XY::X);
    const auto e = lin_com.coeff(XY::Y);

    // Remember that you can't initialize with the b directly,
    const Rational y_coeff = Rational{e * a - d * b} / a;
    const Rational eta_coeff = Rational{-d * c} / a;

    return LinComArrQ<YEta>{y_coeff, eta_coeff};
}
