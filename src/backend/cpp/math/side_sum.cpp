#include "math/side_sum.hpp"

/*
Jul 31 2025 Marco Mai
transfer from Java
*/
SideSum::SideSum(const std::array<int32_t, 3>& coeffs,const float64_t& x,const float64_t& y,const float64_t& z)
    : coeffs(coeffs), x(x), y(y), z(z) {}



SideSum SideSum::create(const float64_t& x, const float64_t& y) {
    return SideSum({0, 0, 0}, x, y, boost::math::constants::pi<float64_t>() - x - y);
}


void SideSum::add(int32_t num) {
    coeffs[num] += 1;
}

void SideSum::sub(int32_t num) {
    coeffs[num] -= 1;
}

float64_t SideSum::sum() {
    return coeffs[0] * x + coeffs[1] * y + coeffs[2] * z;
}


SideSum SideSum::copy() const {
    return SideSum(coeffs, x, y, z);
}

std::string SideSum::to_string() const {
    std::ostringstream oss;
    oss << coeffs[0] << " * A + " << coeffs[1]
        << " * B + " << coeffs[2] << " * C";
    return oss.str();
}
