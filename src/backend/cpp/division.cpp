#include "division.hpp"
#include "math/monomial.hpp"
#include "math/polynomial.hpp"

// These typedefs are only used within this file
template <typename T>
using MonomialZ = math::Monomial<T, Coeff64>;
template <typename T>
using PolynomialZ = math::Polynomial<T, Coeff64>;

// BIG TODO there is a lot of ugly code duplication in this code right now. We need to clean it up
// and make it work nicer.

// I believe that the theory behind this is solid. In general, multivariate polynomial division
// is defined, but one will get different answers based on the monomial ordering (and maybe
// base on other things as well). However, if
// the factor evenly divides the polynomial, then all the answers are 0, so there is no worry.
// R is a UFD => R[X] is a UFD too. Hence Z[x, y] is a UFD

template <template <typename> class Trig>
std::pair<Coeff64, Coeff64> max_coeffs(const LinComMapZ<Trig<LinComArrZ<XY>>>& lin_com) {
    // We want the maximum absolute value,
    // so the minimum possible values are 0
    // Do these values make sense in the trivial case? I think so, because it means
    // we are just dividing by s^0*t^0 = 1
    Coeff64 x_max = 0;
    Coeff64 y_max = 0;
    for (const auto& kv : lin_com) {
        const auto& arg = kv.first.arg;

        const auto abs_x_coeff = math::abs(arg.coeff(XY::X));
        const auto abs_y_coeff = math::abs(arg.coeff(XY::Y));

        x_max = std::max(x_max, abs_x_coeff);
        y_max = std::max(y_max, abs_y_coeff);
    }

    return {x_max, y_max};
}

// constexpr if can help us get rid of this duplication in C++17
// The to_poly functions differ in one line
static std::pair<PolynomialZ<ST>, MonomialZ<ST>> to_poly(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com) {

    const auto xy_max = max_coeffs(lin_com);

    const auto x_max = xy_max.first;
    const auto y_max = xy_max.second;

    PolynomialZ<ST> poly{};
    for (const auto& kv : lin_com) {
        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff<XY::X>();
        const auto y_coeff = arg.coeff<XY::Y>();

        // a*sin(b*x + c*y) = a*s^b*t^c - a*s^{-b}*t^{-c}
        const MonomialZ<ST> first{x_max + x_coeff, y_max + y_coeff};
        const MonomialZ<ST> second{x_max - x_coeff, y_max - y_coeff};

        poly.add(first, kv.second);
        poly.sub(second, kv.second);
    }

    const MonomialZ<ST> denom{x_max, y_max};

    return {poly, denom};
}

static std::pair<PolynomialZ<ST>, MonomialZ<ST>> to_poly(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com) {

    const auto xy_max = max_coeffs(lin_com);

    const auto x_max = xy_max.first;
    const auto y_max = xy_max.second;

    PolynomialZ<ST> poly{};
    for (const auto& kv : lin_com) {
        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff<XY::X>();
        const auto y_coeff = arg.coeff<XY::Y>();

        // a*cos(b*x + c*y) = a*s^b*t^c + a*s^{-b}*t^{-c}
        const MonomialZ<ST> first{x_max + x_coeff, y_max + y_coeff};
        const MonomialZ<ST> second{x_max - x_coeff, y_max - y_coeff};

        poly.add(first, kv.second);
        poly.add(second, kv.second); // this line is the only difference
    }

    const MonomialZ<ST> denom{x_max, y_max};

    return {poly, denom};
}

template <ST Sym>
boost::optional<std::pair<MonomialZ<ST>, Coeff64>> first_term_divides(const PolynomialZ<ST>& poly) {
    for (const auto& kv : poly) {
        if (kv.first.exponent<Sym>() >= 2) {
            // Need to drop the `const` qualifier
            return std::make_pair(kv.first, kv.second);
        }
    }

    return boost::none;
}

template <ST Sym0, ST Sym1>
boost::optional<std::pair<MonomialZ<ST>, Coeff64>> first_term_divides(const PolynomialZ<ST>& poly) {
    for (const auto& kv : poly) {
        if (kv.first.exponent<Sym0>() >= 2 && kv.first.exponent<Sym1>() >= 2) {
            // Need to drop the `const` qualifier
            return std::make_pair(kv.first, kv.second);
        }
    }

    return boost::none;
}

template <ST Sym>
boost::optional<PolynomialZ<ST>> divide_x2m1_full(PolynomialZ<ST> remainder) {

    PolynomialZ<ST> quotient{};

    while (!remainder.is_zero()) {

        // We need to find the first (starting at the beginning) monomial
        // that x^2 divides
        const auto opt = first_term_divides<Sym>(remainder);
        // Does not divide, so has non-zero remainder
        if (!opt) {
            return boost::none;
        }

        auto term = *opt;

        // Update remainder
        remainder.sub(term.first, term.second);
        term.first.divide(Sym, 2);
        quotient.add(term.first, term.second);
        remainder.add(term.first, term.second);
    }

    return quotient;
}

// divisor is x^2 - 1, where x = s, t, or s*t
// For multivariate polynomials, I think the results of Euclidean
// division may not be unique, not that it doesn't work in some cases.
// Now, if it divides evenly, of course the answer is unique
// Algorithm based on
// https://en.wikipedia.org/wiki/Polynomial_long_division#Pseudo-code
// with modifications from
// https://en.wikipedia.org/wiki/Gröbner_basis#Reduction
// to take into account multivariate polynomials
template <ST Sym>
boost::optional<PolynomialZ<ST>> divide_x2m1_partial(PolynomialZ<ST> remainder) {

    const auto orig_size = remainder.size();

    PolynomialZ<ST> quotient{};

    while (!remainder.is_zero()) {
        // Stop dividing once the quotient gets larger than
        // the original size
        // TODO find out how large we should allow the quotient
        // to become
        // It is possible that orig_size is an odd number, like 7,
        // and the quotient comes out to be something like 8, which
        // is acceptable
        // I believe that the quotient size always increases, and never
        // decreases as you continue to divide
        if (quotient.size() > orig_size + 2) {
            return boost::none;
        }

        // We need to find the first (starting at the beginning) monomial
        // that x^2 divides
        const auto opt = first_term_divides<Sym>(remainder);
        // Does not divide, so has non-zero remainder
        if (!opt) {
            return boost::none;
        }

        auto term = *opt;

        // Update remainder
        remainder.sub(term.first, term.second);
        term.first.divide(Sym, 2);
        quotient.add(term.first, term.second);
        remainder.add(term.first, term.second);
    }

    return quotient;
}

template <ST Sym0, ST Sym1>
boost::optional<PolynomialZ<ST>> divide_x2m1_full(PolynomialZ<ST> remainder) {

    PolynomialZ<ST> quotient{};

    while (!remainder.is_zero()) {

        const auto opt = first_term_divides<Sym0, Sym1>(remainder);
        // Does not divide, so has non-zero remainder
        if (!opt) {
            return boost::none;
        }

        auto term = *opt;

        remainder.sub(term.first, term.second);
        term.first.divide(Sym0, 2);
        term.first.divide(Sym1, 2);
        quotient.add(term.first, term.second);
        remainder.add(term.first, term.second);
    }

    return quotient;
}

template <ST Sym0, ST Sym1>
boost::optional<PolynomialZ<ST>> divide_x2m1_partial(PolynomialZ<ST> remainder) {

    const auto orig_size = remainder.size();

    PolynomialZ<ST> quotient{};

    while (!remainder.is_zero()) {

        if (quotient.size() > orig_size + 2) {
            return boost::none;
        }

        const auto opt = first_term_divides<Sym0, Sym1>(remainder);
        // Does not divide, so has non-zero remainder
        if (!opt) {
            return boost::none;
        }

        auto term = *opt;

        remainder.sub(term.first, term.second);
        term.first.divide(Sym0, 2);
        term.first.divide(Sym1, 2);
        quotient.add(term.first, term.second);
        remainder.add(term.first, term.second);
    }

    return quotient;
}

static LinComMapZ<Sin<LinComArrZ<XY>>> to_sin(const PolynomialZ<ST>& numer, const MonomialZ<ST>& denom) {

    // Assert that the numerator has even length
    const auto size = numer.size();
    if (size % 2 != 0) {
        throw std::runtime_error("to_sin: odd numerator size");
    }

    auto iter = numer.begin();
    auto riter = numer.rbegin();

    LinComMapZ<Sin<LinComArrZ<XY>>> builder{};

    for (size_t i = 0; i < size / 2; ++i) {

        const auto iter_coeff = iter->second;
        const auto riter_coeff = riter->second;

        if (iter_coeff != -riter_coeff) {
            throw std::runtime_error("to_sin: not negated coeffs");
        }

        auto iter_monom = iter->first;
        iter_monom.divide(denom);

        auto riter_monom = riter->first;
        riter_monom.divide(denom);

        // invert
        riter_monom.power(-1);

        if (iter_monom != riter_monom) {
            throw std::runtime_error("to_sin: unequal monomials");
        }

        // simplify_sin should be unnecessary here
        const auto s_exponent = iter_monom.exponent<ST::S>();
        const auto t_exponent = iter_monom.exponent<ST::T>();

        const LinComArrZ<XY> arg{s_exponent, t_exponent};

        const Sin<LinComArrZ<XY>> sin{arg};

        builder.add(iter_coeff, sin);

        ++iter;
        ++riter;
    }

    return builder;
}

static LinComMapZ<Cos<LinComArrZ<XY>>> to_cos(const PolynomialZ<ST>& numer, const MonomialZ<ST>& denom) {

    // The numerator can have even or odd length (odd lenghth when there is a constant)
    const auto size = numer.size();

    auto iter = numer.begin();
    auto riter = numer.rbegin();

    LinComMapZ<Cos<LinComArrZ<XY>>> builder{};

    for (size_t i = 0; i < size / 2; ++i) {

        const auto iter_coeff = iter->second;
        const auto riter_coeff = riter->second;

        if (iter_coeff != riter_coeff) {
            throw std::runtime_error("to_cos: unequal coeffs");
        }

        auto iter_monom = iter->first;
        iter_monom.divide(denom);

        auto riter_monom = riter->first;
        riter_monom.divide(denom);

        // invert
        riter_monom.power(-1);

        if (iter_monom != riter_monom) {
            throw std::runtime_error("to_cos: unequal monomials");
        }

        // simplify_cos should be unnecessary here
        const auto s_exponent = iter_monom.exponent<ST::S>();
        const auto t_exponent = iter_monom.exponent<ST::T>();

        const LinComArrZ<XY> arg{s_exponent, t_exponent};

        const Cos<LinComArrZ<XY>> cos{arg};

        builder.add(iter_coeff, cos);

        ++iter;
        ++riter;
    }

    if (size % 2 != 0) {
        // If the size is odd, then there is a constant term in the middle
        // iter and riter should both now point to the same member

        const auto& iter_monom = iter->first;

        // iter_monom / denom = 1 => iter_monom = denom

        if (iter_monom != denom) {
            throw std::runtime_error("to_cos: middle monomial is not 1");
        }

        const auto iter_coeff = iter->second;

        // Note that we need to divide by 2 here, since all of the other cosine
        // terms have that division

        // cos(0) = 1
        const Cos<LinComArrZ<XY>> cos_zero{LinComArrZ<XY>{}};

        if (iter_coeff % 2 == 0) {
            builder.add(iter_coeff / 2, cos_zero);
        } else {
            // odd coeff, so multiply the rest by 2 and then add
            builder.scale(2);
            builder.add(iter_coeff, cos_zero);
        }
    }

    return builder;
}

// When we take this example, we can divide by s multiple times with the quotient having the
// *same* length as it had before. So watch out for that.
// 3*sin(2*b)+sin(4*b)-sin(6*b)+sin(2*a-6*b)+sin(2*a-2*b)+sin(2*a)-3*sin(2*a+2*b)-sin(2*a+4*b)+sin(2*a+6*b)-sin(4*a-6*b)-sin(4*a-4*b)+sin(4*a)+sin(4*a+2*b)+sin(6*a-4*b)-sin(6*a)

// 2 2 2 4 2 4
// cos(0)-cos(2*b)-cos(2*a-6*b)+cos(2*a), has length 7. This has one less because cos(0) only gives 1 term
// sin(b)-sin(2*a-5*b)-sin(2*a-3*b)-sin(2*a-b), has length 8
// So the rule should be roundup to next even for both, then check <=

// We have the more general problem that sometimes there are other straight lines in a curve other than the ones
// we have here. Factoring those out can also shorten the equations. But to do that, we would need to know
// - how to factor them (need some complete factorization algorithm)
// - how to use them to chop down the region.
template <typename T>
void divide_out_lines_general_lr(const LinComMapZ<T>& lin_com, CurvesLR& curves, const LeftRight& left_right, bool one_over_i) {
    //std::cout << lin_com << std::endl;

    // We want this to be mutable, since we update it as we go along
    PolynomialZ<ST> numer{};
    MonomialZ<ST> denom{};

    std::tie(numer, denom) = to_poly(lin_com);

    Coeff64 sign = 1;

    boost::optional<PolynomialZ<ST>> quotient;

    //std::cout << numer.size() << std::endl;

    while (quotient = divide_x2m1_partial<ST::S>(numer), quotient) {
        //if (quotient) {
        //std::cout << "s " << quotient->size() << std::endl;
        //}

        numer = *quotient; // TODO see if this moves or copies
        denom.divide(ST::S);

        // If we have a 1/i, we factor it out, so it becomes false
        // If we don't have a 1/i, then we need to make another one
        // by 1 = 1/i * -1/i, so that gives us another 1/i for later,
        // so it becomes true (and we get an extra -1)
        if (!one_over_i) {
            sign *= -1;
        }

        one_over_i = !one_over_i;
    }
    while (quotient = divide_x2m1_partial<ST::T>(numer), quotient) {
        //if (quotient) {
        //std::cout << "t " << quotient->size() << std::endl;
        //}

        numer = *quotient; // TODO see if this moves or copies
        denom.divide(ST::T);

        if (!one_over_i) {
            sign *= -1;
        }

        one_over_i = !one_over_i;
    }

    while (quotient = divide_x2m1_partial<ST::S, ST::T>(numer), quotient) {
        //if (quotient) {
        //std::cout << "st " << quotient->size() << std::endl;
        //}

        numer = *quotient; // TODO see if this moves or copies
        denom.divide(ST::S);
        denom.divide(ST::T);

        if (!one_over_i) {
            sign *= -1;
        }

        one_over_i = !one_over_i;
    }

    numer.scale(sign);

    if (one_over_i) {
        // sin
        //std::cout << to_sin(numer, denom) << std::endl;
        curves.first[to_sin(numer, denom)].push_back(left_right);
    } else {
        // cos
        //std::cout << to_cos(numer, denom) << std::endl;
        curves.second[to_cos(numer, denom)].push_back(left_right);
    }
}
// one_over_i = true for sin
// one_over_i = false for cos

void divide_out_lines_lr(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com, CurvesLR& curves, const LeftRight& left_right) {
    // There is nothing to divide out if the lin_com is zero
    if (lin_com.is_zero()) {
        curves.first[lin_com].push_back(left_right);
    } else if (lin_com.size() == 1) {
        // lin_com == constant * sin(n*a + m*b)
        // We are dividing out the equations sin(a), sin(b), sin(a + b)
        // If, however, we come across one of those, we will keep it, since
        // eg. 2 2 needs one of those for a boundary MRR to replace one of the LinComArr's
        const auto& arg = lin_com.begin()->first.arg;
        if (arg == LinComArrZ<XY>{1, 0} || arg == LinComArrZ<XY>{0, 1} || arg == LinComArrZ<XY>{1, 1}) {
            curves.first[lin_com].push_back(left_right);
        } else {
            divide_out_lines_general_lr(lin_com, curves, left_right, true);
        }
    } else {
        divide_out_lines_general_lr(lin_com, curves, left_right, true);
    }
}

void divide_out_lines_lr(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com, CurvesLR& curves, const LeftRight& left_right) {
    // Trying to divide lines out of 0 will lead to an infinite loop.
    if (lin_com.is_zero()) {
        curves.second[lin_com].push_back(left_right);
    } else {
        divide_out_lines_general_lr(lin_com, curves, left_right, false);
    }
}

// -----------------------------------------------------------------
// Divide out lines for the prover
// -----------------------------------------------------------------
// TODO see if there is a way of getting rid of the code duplication

// TODO it turns out the order you divide out the sinx, siny, and sinz has an effect
// on the final length of the equation. We should see if we can optimize it so we
// always divide in such a way such that the equations are shortest

// one_over_i = true for sin
// one_over_i = false for cos
class Division final {
  private:
    PolynomialZ<ST> numer;
    MonomialZ<ST> denom;
    Coeff64 sign;
    bool one_over_i;
    boost::optional<PolynomialZ<ST>> quotient;

    void divide_sinx() {

        while (quotient = divide_x2m1_partial<ST::S>(numer), quotient) {

            numer = *quotient;
            denom.divide(ST::S);

            // If we have a 1/i, we factor it out, so it becomes false
            // If we don't have a 1/i, then we need to make another one
            // by 1 = 1/i * -1/i, so that gives us another 1/i for later,
            // so it becomes true (and we get an extra -1)
            if (!one_over_i) {
                sign *= -1;
            }

            one_over_i = !one_over_i;
        }
    }

    void divide_siny() {

        while (quotient = divide_x2m1_partial<ST::T>(numer), quotient) {

            numer = *quotient;
            denom.divide(ST::T);

            if (!one_over_i) {
                sign *= -1;
            }

            one_over_i = !one_over_i;
        }
    }

    void divide_sinz() {

        while (quotient = divide_x2m1_partial<ST::S, ST::T>(numer), quotient) {

            numer = *quotient;
            denom.divide(ST::S);
            denom.divide(ST::T);

            if (!one_over_i) {
                sign *= -1;
            }

            one_over_i = !one_over_i;
        }
    }

    void divide_angle(const XYZ angle) {
        if (angle == XYZ::X) {
            divide_sinx();
        } else if (angle == XYZ::Y) {
            divide_siny();
        } else if (angle == XYZ::Z) {
            divide_sinz();
        } else {
            throw std::runtime_error(invalid_enum_value("XYZ", angle));
        }
    }

  public:
    explicit Division(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com)
        : numer{}, denom{}, sign{1}, one_over_i{true}, quotient{boost::none} {

        std::tie(numer, denom) = to_poly(lin_com);
    }

    explicit Division(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com)
        : numer{}, denom{}, sign{1}, one_over_i{false}, quotient{boost::none} {

        std::tie(numer, denom) = to_poly(lin_com);
    }

    void divide(Inserter& insert, const XYZ first, const XYZ second) {

        const auto third = other_angle(first, second);

        divide_angle(first);
        divide_angle(second);
        divide_angle(third);

        numer.scale(sign);

        if (one_over_i) {
            // sin
            insert.insert(to_sin(numer, denom));
            //std::cout << "factored eq: " << to_sin(numer, denom) << std::endl;
        } else {
            // cos
            insert.insert(to_cos(numer, denom));
            //std::cout << "factored eq: " << to_cos(numer, denom) << std::endl;
        }
    }

    void divide(Curves& curves, const XYZ first, const XYZ second) {

        const auto third = other_angle(first, second);

        divide_angle(first);
        divide_angle(second);
        divide_angle(third);

        numer.scale(sign);

        if (one_over_i) {
            // sin
            curves.first.insert(to_sin(numer, denom));
            //std::cout << "factored eq: " << to_sin(numer, denom) << std::endl;
        } else {
            // cos
            curves.second.insert(to_cos(numer, denom));
            //std::cout << "factored eq: " << to_cos(numer, denom) << std::endl;
        }
    }
};

void divide_out_lines(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com, Curves& curves, const XYZ first, const XYZ second) {
    // There is nothing to divide out if the lin_com is zero.
    // Also, trying to divide lines out of 0 will lead to an infinite loop.
    if (lin_com.is_zero()) {
        curves.first.insert(lin_com);
    } else {
        Division division{lin_com};
        division.divide(curves, first, second);
    }
}

void divide_out_lines(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com, Curves& curves, const XYZ first, const XYZ second) {
    if (lin_com.is_zero()) {
        curves.second.insert(lin_com);
    } else {
        Division division{lin_com};
        division.divide(curves, first, second);
    }
}

void divide_out_lines(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com, const XYZ first, const XYZ second, Inserter& insert) {
    // There is nothing to divide out if the lin_com is zero.
    // Also, trying to divide lines out of 0 will lead to an infinite loop.
    if (lin_com.is_zero()) {
        insert.curves.first.insert(lin_com);
    } else {
        Division division{lin_com};
        division.divide(insert, first, second);
    }
}

void divide_out_lines(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com, const XYZ first, const XYZ second, Inserter& insert) {
    if (lin_com.is_zero()) {
        insert.curves.second.insert(lin_com);
    } else {
        Division division{lin_com};
        division.divide(insert, first, second);
    }
}

// the first term of the poly that is divisible by the mono
// We assume we are working with integers here
static boost::optional<std::pair<MonomialZ<ST>, Coeff64>> term_reducible_by(const PolynomialZ<ST>& poly, const std::pair<MonomialZ<ST>, Coeff64>& leading_term) {

    for (const auto& term : poly) {
        if (leading_term.first.divides(term.first) && (term.second % leading_term.second == 0)) {
            // The monomial divides the term, and the coefficient divides the other one
            // We can't merely return kv, because we need to drop the `const` qualifier
            return std::make_pair(term.first, term.second);
        }
    }

    return boost::none;
}

// Perform one division
static boost::optional<PolynomialZ<ST>> poly_divide(PolynomialZ<ST> remainder, const PolynomialZ<ST>& divisor) {

    //std::cout << "divisor " << divisor << std::endl;

    const auto leading_term = *divisor.leading_term(); // this fails if the divisor is zero

    PolynomialZ<ST> quotient{};

    // we should perhaps check if the remainder is already zero, and then just return
    //
    //std::cout << "quotient " << quotient << std::endl;
    //std::cout << "remainder " << remainder << std::endl;

    while (!remainder.is_zero()) {

        const auto opt = term_reducible_by(remainder, leading_term);

        // No remaining terms are divisible by the divisor, so there is a nonzero
        // remainder. Hence, it does not divide through evenly
        if (!opt) {
            return boost::none;
        }

        const auto mono = opt->first / leading_term.first;
        const auto coeff = opt->second / leading_term.second;

        // q += div
        quotient.add(mono, coeff);

        // r -= div * divisor
        for (const auto& t : divisor) {
            remainder.sub(t.first * mono, t.second * coeff);
        }

        //std::cout << "quotient " << quotient << std::endl;
        //std::cout << "remainder " << remainder << std::endl;
    }

    return quotient;
}

template <template <typename> class T, template <typename> class S>
boost::optional<std::pair<PolynomialZ<ST>, MonomialZ<ST>>>
divide_generic(const LinComMapZ<T<LinComArrZ<XY>>>& dividend, const LinComMapZ<S<LinComArrZ<XY>>>& divisor) {

    PolynomialZ<ST> dividend_numer{};
    MonomialZ<ST> dividend_denom{};

    std::tie(dividend_numer, dividend_denom) = to_poly(dividend);

    PolynomialZ<ST> divisor_numer{};
    MonomialZ<ST> divisor_denom{};

    std::tie(divisor_numer, divisor_denom) = to_poly(divisor);

    const auto opt = poly_divide(dividend_numer, divisor_numer);

    if (!opt) {
        // Does not divide, so don't worry about it
        return boost::none;
    }

    const auto quotient_numer = *opt;

    const auto quotient_denom = dividend_denom / divisor_denom;

    const auto try_again = poly_divide(quotient_numer, divisor_numer);
    if (try_again) {
        throw std::runtime_error("factor divides twice!");
    }

    return std::make_pair(quotient_numer, quotient_denom);
}

boost::optional<LinComMapZ<Cos<LinComArrZ<XY>>>>
divide_once(const LinComMapZ<Sin<LinComArrZ<XY>>>& eq, const LinComMapZ<Sin<LinComArrZ<XY>>>& factor) {

    const auto result = divide_generic(eq, factor);

    if (result) {
        return to_cos(result->first, result->second);
    } else {
        return boost::none;
    }
}

boost::optional<LinComMapZ<Sin<LinComArrZ<XY>>>>
divide_once(const LinComMapZ<Sin<LinComArrZ<XY>>>& eq, const LinComMapZ<Cos<LinComArrZ<XY>>>& factor) {

    const auto result = divide_generic(eq, factor);

    if (result) {
        return to_sin(result->first, result->second);
    } else {
        return boost::none;
    }
}

boost::optional<LinComMapZ<Sin<LinComArrZ<XY>>>>
divide_once(const LinComMapZ<Cos<LinComArrZ<XY>>>& eq, const LinComMapZ<Sin<LinComArrZ<XY>>>& factor) {

    const auto result = divide_generic(eq, factor);

    if (result) {
        // Need to scale by -1 in this case to make sure
        auto numer = result->first;
        numer.scale(-1);
        return to_sin(numer, result->second);
    } else {
        return boost::none;
    }
}

boost::optional<LinComMapZ<Cos<LinComArrZ<XY>>>>
divide_once(const LinComMapZ<Cos<LinComArrZ<XY>>>& eq, const LinComMapZ<Cos<LinComArrZ<XY>>>& factor) {

    const auto result = divide_generic(eq, factor);

    if (result) {
        return to_cos(result->first, result->second);
    } else {
        return boost::none;
    }
}

// sin(b*x + c*y) = s^b*t^c - s^{-b}*t^{-c}
// cos(b*x + c*y) = s^b*t^c + s^{-b}*t^{-c}

// cos(x) = (exp(ix) + exp(-ix)) / 2
// sin(x) = (exp(ix) - exp(-ix)) / (2i)
