#pragma once

#include "symbols.hpp"

namespace math {

// E is an enum of the variables for this monomial
// N is the type of their exponents
template <typename E, typename N>
class Monomial final {
  private:
    // The underlying type of the enum class must be size_t, because that is the type
    // used for indexing the array
    static constexpr bool proper_type = std::is_same<std::underlying_type_t<E>, size_t>::value;
    static_assert(proper_type, "enum class does not have size_t underlying type");

    std::array<N, Enum<E>::size> exponents;

  public:
    using value_type = typename decltype(exponents)::value_type;
    using iterator = typename decltype(exponents)::iterator;
    using const_iterator = typename decltype(exponents)::const_iterator;

    // This assumes the enum is labelled 0, 1, 2, 3, etc.
    // Unfortunately, I don't think there is a way to enforce that in the type system

    // A monomial default constructs to 1
    constexpr explicit Monomial() {
        exponents.fill(0);
    }

    // You can also initialize all of the coefficients. We use a variadic constructor
    // here because it is cleaner than a std::initializer_list (and also works better
    // with initializing a std::array)
    template <typename... T>
    constexpr explicit Monomial(const T&... exps)
        : exponents{{exps...}} {
        constexpr bool proper_size = sizeof...(exps) == Enum<E>::size;
        static_assert(proper_size, "Too few initializers in Monomial");
    }

    iterator begin() {
        return std::begin(exponents);
    }

    iterator end() {
        return std::end(exponents);
    }

    const_iterator begin() const {
        return std::begin(exponents);
    }

    const_iterator end() const {
        return std::end(exponents);
    }

    void multiply(const E expr, const N exponent = 1) {
        // Without some extra support from the compiler, I don't think we
        // could remove this bounds check (I mean the compiler could already
        // be doing it behind the scenes, but we have no way to enforce it)
        const auto index = static_cast<size_t>(expr);
        exponents.at(index) += exponent;
    }

    void divide(const E expr, const N exponent = 1) {
        const auto index = static_cast<size_t>(expr);
        exponents.at(index) -= exponent;
    }

    void multiply(const Monomial<E, N>& other) {
        for (size_t i = 0; i < exponents.size(); ++i) {
            exponents.at(i) += other.exponents.at(i);
        }
    }

    void divide(const Monomial<E, N>& other) {
        for (size_t i = 0; i < exponents.size(); ++i) {
            exponents.at(i) -= other.exponents.at(i);
        }
    }

    void power(const N power) {
        for (auto& exponent : exponents) {
            exponent *= power;
        }
    }

    // exponents.at(i) <= other.exponents.at(i) for each i to divide it
    bool divides(const Monomial<E, N>& other) const {
        for (size_t i = 0; i < exponents.size(); ++i) {
            if (exponents.at(i) > other.exponents.at(i)) {
                return false;
            }
        }

        return true;
    }

    template <E Expr>
    N exponent() const {
        constexpr auto index = static_cast<size_t>(Expr);
        return std::get<index>(exponents);
    }

    N exponent(const E expr) const {
        const auto index = static_cast<size_t>(expr);
        return exponents.at(index);
    }

    bool is_one() const {

        for (const auto exponent : exponents) {
            if (exponent != 0) {
                return false;
            }
        }

        return true;
    }

    friend bool operator==(const Monomial<E, N>& lhs, const Monomial<E, N>& rhs) {
        return lhs.exponents == rhs.exponents;
    }

    friend bool operator!=(const Monomial<E, N>& lhs, const Monomial<E, N>& rhs) {
        return !operator==(lhs, rhs);
    }

    friend bool operator<(const Monomial<E, N>& lhs, const Monomial<E, N>& rhs) {
        return lhs.exponents < rhs.exponents;
    }

    friend std::ostream& operator<<(std::ostream& os, const Monomial<E, N>& monom) {

        if (monom.is_one()) {
            return os << '1';
        }

        bool front = true;
        for (size_t i = 0; i < monom.exponents.size(); ++i) {

            const E expr = static_cast<E>(i);
            const N exponent = monom.exponents.at(i);

            if (exponent == 0) {
                continue;
            }

            if (!front) {
                os << '*';
            }

            if (exponent == 1) {
                os << expr;
            } else {
                os << expr << '^' << exponent;
            }

            front = false;
        }

        return os;
    }

    friend Monomial<E, N> operator*(Monomial<E, N> lhs, const Monomial<E, N>& rhs) {
        lhs.multiply(rhs);
        return lhs;
    }

    friend Monomial<E, N> operator/(Monomial<E, N> lhs, const Monomial<E, N>& rhs) {
        lhs.divide(rhs);
        return lhs;
    }
};
}
