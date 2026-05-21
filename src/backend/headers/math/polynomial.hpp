#pragma once

#include <initializer_list> // std::initializer_list
#include <map>              // std::map
#include <sstream>          // std::ostringstream

#include <boost/optional.hpp>

#include "monomial.hpp"

namespace math {

template <typename E, typename N>
class Polynomial final {
  private:
    // A class invariant is that this map cannot have a zero coefficient in it.
    // We do this by keeping this private and ensuring the public API maintains this invariant.
    std::map<Monomial<E, N>, N> terms;

  public:
    using value_type = typename decltype(terms)::value_type;

    // In a map, the elements are ordered from smallest to largest.
    // However, when iterating over a polynomial, we want to go from largest to smallest.
    using iterator = typename decltype(terms)::reverse_iterator;
    using const_iterator = typename decltype(terms)::const_reverse_iterator;

    using reverse_iterator = typename decltype(terms)::iterator;
    using const_reverse_iterator = typename decltype(terms)::const_iterator;

    explicit Polynomial(const std::initializer_list<value_type>& init)
        : terms{init} {}

    iterator begin() {
        return std::rbegin(terms);
    }

    iterator end() {
        return std::rend(terms);
    }

    const_iterator begin() const {
        return std::rbegin(terms);
    }

    const_iterator end() const {
        return std::rend(terms);
    }

    reverse_iterator rbegin() {
        return std::begin(terms);
    }

    reverse_iterator rend() {
        return std::end(terms);
    }

    const_reverse_iterator rbegin() const {
        return std::begin(terms);
    }

    const_reverse_iterator rend() const {
        return std::end(terms);
    }

    boost::optional<std::pair<Monomial<E, N>, N>> leading_term() const {
        const auto loc = begin();
        if (loc == end()) {
            return boost::none;
        } else {
            return std::make_pair(loc->first, loc->second);
        }
    }

    void add(const Polynomial<E, N>& other) {
        for (const auto& kv : other) {
            add(kv.second, kv.first);
        }
    }

    void sub(const Polynomial<E, N>& other) {
        for (const auto& kv : other) {
            sub(kv.second, kv.first);
        }
    }

    void add(const Monomial<E, N>& mono, const N coeff = 1) {

        const auto it = terms.find(mono);

        if (it == std::end(terms)) {
            // mono not in the map
            if (coeff != 0) {
                terms.emplace(mono, coeff);
            }
        } else {
            // mono in the map

            const auto sum = it->second + coeff;
            if (sum == 0) {
                terms.erase(it);
            } else {
                it->second = sum;
            }
        }
    }

    void sub(const Monomial<E, N>& mono, const N coeff = 1) {

        const auto it = terms.find(mono);

        if (it == std::end(terms)) {
            // mono not in the map
            if (coeff != 0) {
                terms.emplace(mono, -coeff);
            }
        } else {
            // mono in the map

            const auto diff = it->second - coeff;
            if (diff == 0) {
                terms.erase(it);
            } else {
                it->second = diff;
            }
        }
    }

    void scale(const N scale) {

        if (scale == 0) {
            terms.clear();
        } else {
            for (auto& kv : terms) {
                kv.second *= scale;
            }
        }
    }

    bool is_zero() const {
        return terms.empty();
    }

    size_t size() const {
        return terms.size();
    }

    N coeff(const Monomial<E, N>& mono) const {

        const auto it = terms.find(mono);

        if (it == std::end(terms)) {
            // default to 0 for things we can't find
            return 0;
        } else {
            return it->second;
        }
    }

    friend bool operator==(const Polynomial<E, N>& lhs, const Polynomial<E, N>& rhs) {
        return lhs.terms == rhs.terms;
    }

    friend bool operator<(const Polynomial<E, N>& lhs, const Polynomial<E, N>& rhs) {
        return lhs.terms < rhs.terms;
    }

    friend std::ostream& operator<<(std::ostream& os, const Polynomial<E, N>& poly) {

        // the sum of no elements is 0
        if (poly.is_zero()) {
            return os << '0';
        }

        // Set to false once we add the first coefficient
        bool front = true;

        for (const auto& kv : poly) {

            const auto& monom = kv.first;
            const auto coeff = kv.second;

            // add a positive sign if it is positive and not at the front
            if ((coeff > 0) && !front) {
                os << '+';
            }

            if (coeff == 1) {
                os << monom;
            } else if (coeff == -1) {
                os << '-' << monom;
            } else if (coeff != 0) {
                os << coeff;
                if (!monom.is_one()) {
                    os << '*' << monom;
                }
            } else {
                std::ostringstream err{};
                err << monom << " has zero coefficient in Polynomial";
                throw std::runtime_error(err.str());
            }

            front = false;
        }

        return os;
    }
};
}
