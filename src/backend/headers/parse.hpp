#pragma once

#include "code_sequence.hpp"
#include "general.hpp"

CodeSequence parse_code_sequence(const std::string& str);

InitialAngles parse_initial_angles(const std::string& str);

LinComArrZ<XY> parse_lin_com_arr_xy(const std::string& str);

LinComArrZ<XYPi> parse_lin_com_arr_xypi(const std::string& str);

LinComArrZ<XYEta> parse_lin_com_arr_xyeta(const std::string& str);

LinComMapZ<Sin<LinComArrZ<XY>>> parse_lin_com_map_sin_xy(const std::string& str);

LinComMapZ<Cos<LinComArrZ<XY>>> parse_lin_com_map_cos_xy(const std::string& str);
