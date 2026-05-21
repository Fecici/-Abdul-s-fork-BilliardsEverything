#pragma once

#ifndef VARY3_HPP
#define VARY3_HPP

#include <array>
#include <string>
#include <sstream>
#include <list>
#include <vector>
#include <cmath> 
#include <stdexcept>
#include <numbers.hpp>
#include <algorithm>
#include <unordered_map>
#include <thread>
// #include "cancel_flag.hpp"

#include <math/side_sum.hpp>

#include <boost/optional.hpp>
#include <boost/multiprecision/mpfr.hpp>
#include <boost/math/constants/constants.hpp>
 // Needed for std::atan2 overload resolution if mixed types

#include "code_sequence.hpp"
#include "triangle_billiard.hpp"
#include "utils.hpp"
#include "vary_cs.hpp"

using high_prec_t = boost::multiprecision::mpfr_float_50;
/*
Jul 31 2025 Marco Mai
transfer from Java
*/
// static std::unordered_map<std::string, CodeType> stringToCodeType = {
//     {"oso", CodeType::OSO},
//     {"osno", CodeType::OSNO},
//     {"ons", CodeType::ONS},
//     {"cs", CodeType::CS},
//     {"cns", CodeType::CNS}
// };


void iterateFireAway3(
    int32_t min, int32_t max, float64_t specMin, float64_t specMax, float64_t initPosition,
    SideSum& sideSum, TriangleBilliard billiard,
    std::vector<int32_t>& code,
    std::vector<std::vector<int32_t>>& codesFound, std::string reqType);

std::vector<std::vector<int32_t>> fireAway3(const int32_t movesMin, const int32_t movesMax,const float64_t xAngle, const float64_t yAngle,const float64_t pos, const std::string reqType);



#endif // VARY3_HPP

