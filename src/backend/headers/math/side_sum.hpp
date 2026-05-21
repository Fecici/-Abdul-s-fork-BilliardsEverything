#pragma once

#ifndef SIDESUM_HPP
#define SIDESUM_HPP

#include <array>
#include <string>
#include <sstream>
#include <cmath>
#include <numbers.hpp>
#include <cstdint>
#include <boost/multiprecision/mpfr.hpp>


#include "numbers.hpp"


class SideSum {
public:


private:
    std::array<int32_t, 3> coeffs{};
    float64_t x;
    float64_t y;
    float64_t z;


   
    SideSum(const std::array<int32_t, 3>& coeffs,const float64_t& x,const float64_t& y,const float64_t& z);

public:
    static SideSum create(const float64_t& x,const float64_t& y);

    void add(int32_t num);
    void sub(int32_t num);

    float64_t sum();

    SideSum copy() const;

    std::string to_string() const;
};

#endif // SIDESUM_HPP