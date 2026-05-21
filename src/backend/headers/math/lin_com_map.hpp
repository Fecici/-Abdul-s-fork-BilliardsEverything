#pragma once

#include <initializer_list> // std::initializer_list
#include <map>              // std::map
#include <ostream>          // std::ostream
#include <sstream>          // std::ostringstream

#include "arith.hpp" // gcd

namespace math {

template <typename T, typename N>
class LinComMap final {
  private:
    // A class invariant is that this map cannot have a zero coefficient in it.
    // We do this by keeping this private and ensuring the public API maintains this invariant.
    std::map<T, N> coeffs;

  public:
    using value_type = typename decltype(coeffs)::value_type;
    using iterator = typename decltype(coeffs)::iterator;
    using const_iterator = typename decltype(coeffs)::const_iterator;

    explicit LinComMap(const std::initializer_list<value_type>& init)
        : coeffs{init} {}

    iterator begin() {
        return std::begin(coeffs);
    }

    iterator end() {
        return std::end(coeffs);
    }

    const_iterator begin() const {
        return std::begin(coeffs);
    }

    const_iterator end() const {
        return std::end(coeffs);
    }

    void add(const T& expr) {

        add(1, expr);
    }

    void sub(const T& expr) {

        sub(1, expr);
    }

    void add(const N coeff, const T& expr) {

        const auto it = coeffs.find(expr);

        if (it == std::end(coeffs)) {
            // expr not in the map
            if (coeff != 0) {
                coeffs.emplace(expr, coeff);
            }
        } else {
            // expr in the map

            const auto sum = it->second + coeff;
            if (sum == 0) {
                coeffs.erase(it);
            } else {
                it->second = sum;
            }
        }
    }

    void sub(const N coeff, const T& expr) {

        const auto it = coeffs.find(expr);

        if (it == std::end(coeffs)) {
            // expr not in the map
            if (coeff != 0) {
                coeffs.emplace(expr, -coeff);
            }
        } else {
            // expr in the map

            const auto diff = it->second - coeff;
            if (diff == 0) {
                coeffs.erase(it);
            } else {
                it->second = diff;
            }
        }
    }

    void add(const LinComMap<T, N>& other) {
        for (const auto& kv : other) {
            add(kv.second, kv.first);
        }
    }

    void sub(const LinComMap<T, N>& other) {
        for (const auto& kv : other) {
            sub(kv.second, kv.first);
        }
    }

    void scale(const N scale) {

        if (scale == 0) {
            coeffs.clear();
        } else {
            for (auto& kv : coeffs) {
                kv.second *= scale;
            }
        }
    }

    N content() const {

        N g = 0;

        // Perfect example of a foldl right here
        for (const auto& kv : coeffs) {
            // The gcd is always positive
            g = gcd(g, kv.second);
        }

        return g;
    }

    void divide_content() {

        const auto g = content();

        if (g != 0) {
            for (auto& kv : coeffs) {
                kv.second /= g;
            }
        }
    }

    size_t size() const {
        return coeffs.size();
    }

    bool is_zero() const {
        // The sum of no elements is zero
        return coeffs.empty();
    }

    N coeff(const T& expr) const {

        const auto it = coeffs.find(expr);

        if (it == std::end(coeffs)) {
            // default to 0 for things not in the map
            return 0;
        } else {
            return it->second;
        }
    }

    friend bool operator==(const LinComMap<T, N>& lhs, const LinComMap<T, N>& rhs) {
        return lhs.coeffs == rhs.coeffs;
    }

    friend bool operator<(const LinComMap<T, N>& lhs, const LinComMap<T, N>& rhs) {
        return lhs.coeffs < rhs.coeffs;
    }

    friend std::ostream& operator<<(std::ostream& os, const LinComMap<T, N>& lin_com) {

        if (lin_com.is_zero()) {
            return os << '0';
        }

        // set to false once we add the first coefficient
        bool front = true;

        for (const auto& kv : lin_com) {

            const auto& expr = kv.first;
            const auto& coeff = kv.second;

            // add a positive sign if it is positive and not at the front
            if ((coeff > 0) && !front) {
                os << '+';
            }

            if (coeff == 1) {
                // do nothing
            } else if (coeff == -1) {
                os << '-';
            } else if (coeff != 0) {
                os << coeff;
            } else {
                std::ostringstream err{};
                err << expr << " has zero coefficient in LinComMap";
                throw std::runtime_error(err.str());
            }

            os << expr;
            front = false;
        }

        return os;
    }
};
}
