#pragma once

#include <sstream> // std::ostringstream
#include <vector>  // std::vector

#include "lin_com_map.hpp"

namespace math {

// This class is an immutable version of a LinComMap. Once an equation has been constructed,
// it will often be kept around to do calculations with. Because of the sheer number of equations
// we have to deal with, it is important to use a memory-efficient and cache-friendly representation,
// which is what this class provides.

// To ensure the class invariants are maintained, this can only be constructed by converting
// from a LinComMap.
template <typename T, typename N>
class LinComVec final {
  private:
    //std::vector<std::pair<T, N>> coeffs;

  public:
    std::vector<std::pair<T, N>> coeffs;

    using value_type = typename decltype(coeffs)::value_type;
    using const_iterator = typename decltype(coeffs)::const_iterator;

    explicit LinComVec()
        : coeffs{} {}

    explicit LinComVec(const LinComMap<T, N>& lin_com_map) {

        for (const auto& kv : lin_com_map) {
            coeffs.emplace_back(kv.first, kv.second);
        }
    }

    const_iterator begin() const {
        return std::begin(coeffs);
    }

    const_iterator end() const {
        return std::end(coeffs);
    }

    size_t size() const {
        return coeffs.size();
    }

    bool is_zero() const {
        // The sum of no elements is zero
        return coeffs.empty();
    }

    friend bool operator==(const LinComVec<T, N>& lhs, const LinComVec<T, N>& rhs) {
        return lhs.coeffs == rhs.coeffs;
    }

    friend bool operator<(const LinComVec<T, N>& lhs, const LinComVec<T, N>& rhs) {
        return lhs.coeffs < rhs.coeffs;
    }

    friend std::ostream& operator<<(std::ostream& os, const LinComVec<T, N>& lin_com) {


        bool first = true;
        for (const auto& kv : lin_com) {
        // order is trig_coeff, x_coeff, y_coeff
            const auto trig_coeff = kv.second;
            const auto x_coeff = kv.first.arg.coeff(XY::X);
            const auto y_coeff = kv.first.arg.coeff(XY::Y);
            //if (x_coeff == 0 && y_coeff == 0) continue;
            if (!first) {
                os << ' ';
            }
            else {
                std::ostringstream temp{};
                temp << kv.first;
                if (temp.str()[0] == 's') {
                    os << "sin ";
                }
                else {
                    os << "cos ";
                }
            }

            os << trig_coeff << ' ' << x_coeff << ' ' << y_coeff;

            first = false;
        }
        return os;
    }
};
}
