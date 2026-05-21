#pragma once

#include "division.hpp"

// We have the more general problem that sometimes there are other straight lines in a curve other than the ones
// we have here. Factoring those out can also shorten the equations. But to do that, we would need to know
// - how to factor them (need some complete factorization algorithm)
// - how to use them to chop down the region.
template <typename T>
std::string factor_partial(const LinComMapZ<T>& lin_com, bool one_over_i) {

    if (lin_com.is_zero()) {
        return "0";
    }

    // We want this to be mutable, since we update it as we go along
    PolynomialZ<ST> numer{};
    MonomialZ<ST> denom{};

    std::tie(numer, denom) = to_poly(lin_com);

    Coeff64 sign = 1;

    boost::optional<PolynomialZ<ST>> quotient;

    uint32_t sin_a_factors = 0;

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
        sin_a_factors += 1;
    }

    uint32_t sin_b_factors = 0;

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
        sin_b_factors += 1;
    }

    uint32_t sin_ab_factors = 0;

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
        sin_ab_factors += 1;
    }

    numer.scale(sign);

    std::ostringstream oss;
    if (sin_a_factors == 0) {

    } else if (sin_a_factors == 1) {
        oss << "sin(x)*";
    } else {
        oss << "sin(x)^" << sin_a_factors << "*";
    }

    if (sin_b_factors == 0) {

    } else if (sin_b_factors == 1) {
        oss << "sin(y)*";
    } else {
        oss << "sin(y)^" << sin_b_factors << "*";
    }

    if (sin_ab_factors == 0) {

    } else if (sin_ab_factors == 1) {
        oss << "sin(x+y)*";
    } else {
        oss << "sin(x+y)^" << sin_ab_factors << "*";
    }

    oss << '(';

    if (one_over_i) {
        // sin
        const auto sin = to_sin(numer, denom);
        oss << sin;
    } else {
        // cos
        const auto cos = to_cos(numer, denom);
        oss << cos;
    }
    oss << ')';

    return oss.str();
}
// one_over_i = true for sin
// one_over_i = false for cos

std::string factor_partial(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com) {
    return factor_partial(lin_com, true);
}

std::string factor_partial(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com) {
    return factor_partial(lin_com, false);
}

template <typename T>
std::string factor_full(const LinComMapZ<T>& lin_com, bool one_over_i) {

    if (lin_com.is_zero()) {
        return "0";
    }

    // We want this to be mutable, since we update it as we go along
    PolynomialZ<ST> numer{};
    MonomialZ<ST> denom{};

    std::tie(numer, denom) = to_poly(lin_com);

    Coeff64 sign = 1;

    boost::optional<PolynomialZ<ST>> quotient;

    uint32_t sin_a_factors = 0;

    while (quotient = divide_x2m1_full<ST::S>(numer), quotient) {
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
        sin_a_factors += 1;
    }

    uint32_t sin_b_factors = 0;

    while (quotient = divide_x2m1_full<ST::T>(numer), quotient) {
        //if (quotient) {
        //std::cout << "t " << quotient->size() << std::endl;
        //}

        numer = *quotient; // TODO see if this moves or copies
        denom.divide(ST::T);

        if (!one_over_i) {
            sign *= -1;
        }

        one_over_i = !one_over_i;
        sin_b_factors += 1;
    }

    uint32_t sin_ab_factors = 0;

    while (quotient = divide_x2m1_full<ST::S, ST::T>(numer), quotient) {
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
        sin_ab_factors += 1;
    }

    numer.scale(sign);

    std::ostringstream oss;
    if (sin_a_factors == 0) {

    } else if (sin_a_factors == 1) {
        oss << "sin(x)*";
    } else {
        oss << "sin(x)^" << sin_a_factors << "*";
    }

    if (sin_b_factors == 0) {

    } else if (sin_b_factors == 1) {
        oss << "sin(y)*";
    } else {
        oss << "sin(y)^" << sin_b_factors << "*";
    }

    if (sin_ab_factors == 0) {

    } else if (sin_ab_factors == 1) {
        oss << "sin(x+y)*";
    } else {
        oss << "sin(x+y)^" << sin_ab_factors << "*";
    }

    oss << '(';

    if (one_over_i) {
        // sin
        const auto sin = to_sin(numer, denom);
        oss << sin;
    } else {
        // cos
        const auto cos = to_cos(numer, denom);
        oss << cos;
    }
    oss << ')';

    return oss.str();
}

// one_over_i = true for sin
// one_over_i = false for cos
std::string factor_full(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com) {
    return factor_full(lin_com, true);
}

std::string factor_full(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com) {
    return factor_full(lin_com, false);
}
