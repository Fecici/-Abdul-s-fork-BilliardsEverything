#pragma once

#include "general.hpp"

// Intended for used with signed integers
template <typename Int>
Int signum(const Int val) {
    return (0 < val) - (val < 0);
}

std::pair<Coeff64, Sin<LinComArrZ<XY>>> simplify_sin_xy(const LinComArrZ<XY>& arg);

std::pair<Coeff64, Cos<LinComArrZ<XY>>> simplify_cos_xy(const LinComArrZ<XY>& arg);

std::pair<Coeff64, Sin<LinComArrZ<XY>>> simplify_sin_xypi(const LinComArrZ<XYPi>& arg);

std::pair<Coeff64, Cos<LinComArrZ<XY>>> simplify_cos_xypi(const LinComArrZ<XYPi>& arg);

std::pair<Coeff64, Cos<LinComArrZ<XY>>> simplify_sin_xyeta(const LinComArrZ<XYEta>& arg);

std::pair<Coeff64, Sin<LinComArrZ<XY>>> simplify_cos_xyeta(const LinComArrZ<XYEta>& arg);

LinComMapZ<Sin<LinComArrZ<XY>>> multiply_lin_com(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos, const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_sin);

LinComMapZ<Cos<LinComArrZ<XY>>> multiply_lin_com(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_sin1, const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_sin2);

LinComMapZ<Cos<LinComArrZ<XY>>> multiply_lin_com(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos1, const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos2);
LinComMapZ<Sin<LinComArrZ<XY>>> multiply_lin_com(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_sin, const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos);

Coeff64 simplify_lin_com_zero_zero(const LinComMapZ<Sin<LinComArrZ<XY>>>&);

Coeff64 simplify_lin_com_zero_zero(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com);

Coeff64 simplify_lin_com_zero_pi2(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com);

Coeff64 simplify_lin_com_zero_pi2(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com);

Coeff64 simplify_lin_com_pi2_zero(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com);

Coeff64 simplify_lin_com_pi2_zero(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com);

Coeff64 simplify_lin_com_pi2_pi2(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com);

Coeff64 simplify_lin_com_pi2_pi2(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com);
//LinComMapZ<Cos<LinComArrZ<XY>>> add_lin_com(LinComMapZ<Cos<LinComArrZ<XY>>> lin_com_cos1, const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos2);
//LinComMapZ<Sin<LinComArrZ<XY>>> add_lin_com(LinComMapZ<Sin<LinComArrZ<XY>>> lin_com_cos1, const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_cos2);
LinComMapZ<Cos<LinComArrZ<XY>>> get_final_result_formula(const LinComMapZ<Cos<LinComArrZ<XY>>> lin_com, const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com2, const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com3);
LinComMapZ<Sin<LinComArrZ<XY>>> get_final_result_formula(const LinComMapZ<Sin<LinComArrZ<XY>>> lin_com, const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com2, const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com3);
