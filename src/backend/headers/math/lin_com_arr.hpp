#pragma once

#include <array>       // std::array
#include <ostream>     // std::ostream
#include <type_traits> // type trait stuff

#include "arith.hpp" // gcd
#include "symbols.hpp"

namespace math {

// This assumes the enum is labelled 0, 1, 2, 3, etc.
// Unfortunately, I don't think there is a way to enforce that in the type system
template <typename E, typename N>
class LinComArr final {
  private:
    // The underlying type of the enum class must be size_t, because that is the type
    // used for indexing the array
    static constexpr bool proper_type = std::is_same<std::underlying_type_t<E>, size_t>::value;
    static_assert(proper_type, "enum class does not have size_t underlying type");

    std::array<N, Enum<E>::size> coeffs;

  public:
    using value_type = typename decltype(coeffs)::value_type;
    using iterator = typename decltype(coeffs)::iterator;
    using const_iterator = typename decltype(coeffs)::const_iterator;

    // A LinComArr default constructs to 0.
    // We can't merely default initialize the array, because not all number types will default initialize to 0.
    constexpr explicit LinComArr() {
        coeffs.fill(0);
    }

    // You can also initialize all of the coefficients. We use a variadic constructor
    // here because it is cleaner than a std::initializer_list (and also works better
    // with initializing a std::array)
    template <typename... T>
    constexpr explicit LinComArr(const T&... coeffs_)
        : coeffs{{coeffs_...}} {
        constexpr bool proper_size = sizeof...(coeffs_) == Enum<E>::size;
        static_assert(proper_size, "Too few initializers in LinComArr");
    }

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

    void add(const E expr) {
        add(1, expr);
    }

    void sub(const E expr) {
        sub(1, expr);
    }

    void add(const N coeff, const E expr) {
        // Without some extra support from the compiler, I don't think we
        // could remove this bounds check (I mean the compiler could already
        // be doing it behind the scenes, but we have no way to enforce it)
        const auto index = static_cast<size_t>(expr);
        coeffs.at(index) += coeff;
    }

    void sub(const N coeff, const E expr) {
        const auto index = static_cast<size_t>(expr);
        coeffs.at(index) -= coeff;
    }

    void add(const LinComArr<E, N>& other) {
        add(1, other);
    }

    void sub(const LinComArr<E, N>& other) {
        sub(1, other);
    }

    void add(const N scale, const LinComArr<E, N>& other) {

        for (size_t i = 0; i < coeffs.size(); ++i) {
            // The compiler should optimize out these bound checks
            coeffs.at(i) += scale * other.coeffs.at(i);
        }
    }

    void sub(const N scale, const LinComArr<E, N>& other) {

        for (size_t i = 0; i < coeffs.size(); ++i) {
            coeffs.at(i) -= scale * other.coeffs.at(i);
        }
    }

    static LinComArr<E, N> add(LinComArr<E, N> lhs, const LinComArr<E, N>& rhs) {
        lhs.add(rhs);
        return lhs;
    }

    static LinComArr<E, N> sub(LinComArr<E, N> lhs, const LinComArr<E, N>& rhs) {
        lhs.sub(rhs);
        return lhs;
    }

    void scale(const N scale) {
        for (auto& coeff : coeffs) {
            coeff *= scale;
        }
    }

    // Using terminology for polynomials over Z as inspiration,
    // we define the following terms for an LinComArr a.
    // unit(a) = sign(first non zero coefficient), or 0 if all coefficients are zero
    // content(a) = gcd(all coeffs), where the gcd is > 0

    N unit() const {

        for (const auto& coeff : coeffs) {
            // Find the first non-zero coefficient
            if (coeff < 0) {
                return -1;
            } else if (coeff > 0) {
                return 1;
            }
            // else the coefficient is 0, so we just
            // continue on to the next one
        }

        // Everything is zero, so just return 0
        return 0;
    }

    // Perhaps rename this to normalize_unit or something
    void divide_unit() {

        // If the unit is 0, then all coefficients are zero, so scaling by 0 won't do anything
        scale(unit());
    }

    N content() const {

        // Perfect example of a foldl right here
        N g = 0;
        for (const auto& coeff : coeffs) {
            // The gcd is always positive
            g = gcd(g, coeff);
        }

        return g;
    }

    // This doesn't change the sign of any of the coefficients
    // since the content is always > 0
    void divide_content() {

        const auto g = content();

        // Don't divide if the gcd is zero
        // (which means the LinComArr is 0)
        if (g != 0) {
            for (auto& coeff : coeffs) {
                coeff /= g;
            }
        }
    }

    N coeff(const E expr) const {
        const auto index = static_cast<size_t>(expr);
        return coeffs.at(index);
    }

    template <E Expr>
    N coeff() const {
        constexpr auto index = static_cast<size_t>(Expr);
        return std::get<index>(coeffs);
    }

    // A LinComArr is zero iff each coefficient is zero
    bool is_zero() const {
        for (const auto& coeff : coeffs) {
            if (coeff != 0) {
                return false;
            }
        }

        return true;
    }

    friend bool operator==(const LinComArr<E, N>& lhs, const LinComArr<E, N>& rhs) {
        return lhs.coeffs == rhs.coeffs;
    }

    friend bool operator<(const LinComArr<E, N>& lhs, const LinComArr<E, N>& rhs) {
        return lhs.coeffs < rhs.coeffs;
    }

    friend std::ostream& operator<<(std::ostream& os, const LinComArr<E, N>& lin_com) {

        if (lin_com.is_zero()) {
            return os << '0';
        }

        // set to false once we add the first coefficient
        bool front = true;

        for (size_t i = 0; i < lin_com.coeffs.size(); ++i) {

            const auto expr = static_cast<E>(i);
            const auto coeff = lin_com.coeffs.at(i);

            // add a positive sign if it is positive and not at the front
            if ((coeff > 0) && !front) {
                os << '+';
            }

            if (coeff == -1) {
                os << '-';
            } else if ((coeff != 0) && (coeff != 1)) {
                os << coeff;
            }

            // if coeff == 0, we ignore it
            // otherwise, we << it and set front to false
            if (coeff != 0) {
                os << expr;
                front = false;
            }
        }

        return os;
    }
};
}
