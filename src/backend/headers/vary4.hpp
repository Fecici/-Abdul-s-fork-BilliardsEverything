#pragma once

#ifndef VARY4_HPP
#define VARY4_HPP

#include <array>
#include <string>
#include <sstream>
#include <list>
#include <vector>
#include <cmath> 
#include <stdexcept>
#include <numbers.hpp>
#include <algorithm>
#include <tuple>
#include <unordered_map>
#include <thread>
// #include "cancel_flag.hpp"

#include <math/side_sum.hpp>

#include <boost/optional.hpp>
#include <boost/asio.hpp>
#include <boost/multiprecision/mpfr.hpp>
#include <boost/math/constants/constants.hpp>
 // Needed for std::atan2 overload resolution if mixed types

#include "code_sequence.hpp"
#include "triangle_billiard4.hpp"
#include "utils.hpp"
#include "vary_cs.hpp"

using TriangleStart = std::tuple<TriangleBilliard4, std::vector<int32_t>, SideSum>;
/*
Jul 31 2025 Marco Mai
transfer from Java
*/
void iterateFireAway4(
    int32_t min, int32_t max, float64_t specMin, float64_t specMax, 
    SideSum& sideSum, TriangleBilliard4 billiard,
    std::vector<int32_t>& code,
    std::vector<std::vector<int32_t>>& codesFound, std::string reqType);

std::vector<std::vector<int32_t>> fireAway4(const int32_t movesMin, const int32_t movesMax,const float64_t xAngle, const float64_t yAngle, const std::string reqType);



#endif // VARY3_HPP

