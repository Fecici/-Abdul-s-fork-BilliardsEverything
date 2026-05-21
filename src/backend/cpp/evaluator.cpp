#include "evaluator.hpp"

struct FreeCache {

    ~FreeCache() {
        mpfr_free_cache();
    }
};

// This will free the cache once each thread exits.
static const thread_local FreeCache free_cache{};

// The semantics of MPFR is that it calculates the correct answer to "infinite" precision,
// and then rounds as requested to the precise number of bits. In particular, rounding should
// always be within 1 ULP

// We want everything we add to the sum to be a small (most negative) as possible
// We want to have a lower bound, so we round down and stuff

template <template <typename> class Trig>
void eval_trig_helper(mpfr_t term, const Coeff64 coeff, const mpq_t frac,
                      const mpfr_t half_pi_d, const mpfr_t half_pi_u);

template <>
void eval_trig_helper<Sin>(mpfr_t term, const Coeff64 coeff, const mpq_t frac,
                           const mpfr_t half_pi_d, const mpfr_t half_pi_u) {

    // sin is increasing on [0, pi\2)

    if (coeff > 0) {

        // Round down the argument
        mpfr_mul_q(term, half_pi_d, frac, MPFR_RNDD);

        // MPFR is correctly rounded, so this should never be less than 0
        // In theory though, we could do a check, but it'll be fine
        // TODO if we didn't trust the 0.5 ULP of MPFR, we would have to do
        // a < 0 check here. For more general libraries that do not give us this
        // guarantee (but do give us the correctly rounded up or rounded down
        // guarantee), we would have to do this.

        // Round down the trig function
        mpfr_sin(term, term, MPFR_RNDD);

    } else if (coeff < 0) {

        // Round up the argument
        mpfr_mul_q(term, half_pi_u, frac, MPFR_RNDU);

        // It may be possible when doing the above multiplication that
        // the result is greater than half_pi_d. This is hopefully the
        // closest float to pi/2 (rounded down), so anything over will
        // be in the next quadrant. In fact, it could go so over into
        // the next quadrant that our lower bound is no longer true.
        // In this case, we simply bump the result up to 1, which is
        // sin(pi/2). This is a very obscure case, but it is possible.
        if (mpfr_greater_p(term, half_pi_d)) {
            // sin(pi/2) = 1
            mpfr_set_ui(term, 1, MPFR_RNDU);
        } else {
            // Else we can just find the sin like normal
            mpfr_sin(term, term, MPFR_RNDU);
        }

    } else {
        throw std::runtime_error("eval_trig<Sin>: zero coeff");
    }

    // Using FMA might be interesting in the future, but we'll keep
    // it simple for now.
    mpfr_mul_si(term, term, coeff, MPFR_RNDD);
}

template <>
void eval_trig_helper<Cos>(mpfr_t term, const Coeff64 coeff, const mpq_t frac,
                           const mpfr_t half_pi_d, const mpfr_t half_pi_u) {

    // cos is decreasing on [0, pi/2)

    if (coeff > 0) {

        // Round up the argument
        mpfr_mul_q(term, half_pi_u, frac, MPFR_RNDU);

        // Same as with the sin case
        if (mpfr_greater_p(term, half_pi_d)) {
            // cos(pi/2) = 0
            mpfr_set_zero(term, 0);
        } else {
            // Else we can just find the cos like normal
            mpfr_cos(term, term, MPFR_RNDD);
        }

    } else if (coeff < 0) {

        // Round down the argument
        mpfr_mul_q(term, half_pi_d, frac, MPFR_RNDD);

        // Again we rely that if half_pi_d and frac >= 0, then
        // their product will be >= 0.

        // Round up the trig function
        mpfr_cos(term, term, MPFR_RNDU);

    } else {
        throw std::runtime_error("eval_trig<Cos>: zero coeff");
    }

    // Using FMA might be interesting in the future, but we'll keep
    // it simple for now.
    mpfr_mul_si(term, term, coeff, MPFR_RNDD);
}

template <template <typename> class Trig>
void eval_trig(mpfr_t term, const Coeff64 coeff, const mpq_t frac, const unsigned long quad,
               const mpfr_t half_pi_d, const mpfr_t half_pi_u);

template <>
void eval_trig<Sin>(mpfr_t term, const Coeff64 coeff, const mpq_t frac, const unsigned long quad,
                    const mpfr_t half_pi_d, const mpfr_t half_pi_u) {

    if (quad == 0) {
        // sin(x + 0) = sin(x)
        eval_trig_helper<Sin>(term, coeff, frac, half_pi_d, half_pi_u);

    } else if (quad == 1) {
        // sin(x + pi/2) = cos(x)
        eval_trig_helper<Cos>(term, coeff, frac, half_pi_d, half_pi_u);

    } else if (quad == 2) {
        // sin(x + pi) = -sin(x)
        eval_trig_helper<Sin>(term, -coeff, frac, half_pi_d, half_pi_u);

    } else if (quad == 3) {
        // sin(x + 3pi/2) = -cos(x)
        eval_trig_helper<Cos>(term, -coeff, frac, half_pi_d, half_pi_u);

    } else {
        std::ostringstream oss{};
        oss << "eval_trig<Sin>: unknown quadrant " << quad;
        throw std::runtime_error(oss.str());
    }
}

template <>
void eval_trig<Cos>(mpfr_t term, const Coeff64 coeff, const mpq_t frac, const unsigned long quad,
                    const mpfr_t half_pi_d, const mpfr_t half_pi_u) {

    if (quad == 0) {
        // cos(x + 0) = cos(x)
        eval_trig_helper<Cos>(term, coeff, frac, half_pi_d, half_pi_u);

    } else if (quad == 1) {
        // cos(x + pi/2) = -sin(x)
        eval_trig_helper<Sin>(term, -coeff, frac, half_pi_d, half_pi_u);

    } else if (quad == 2) {
        // cos(x + pi) = -cos(x)
        eval_trig_helper<Cos>(term, -coeff, frac, half_pi_d, half_pi_u);

    } else if (quad == 3) {
        // cos(x + 3pi/2) = sin(x)
        eval_trig_helper<Sin>(term, coeff, frac, half_pi_d, half_pi_u);

    } else {
        std::ostringstream oss{};
        oss << "eval_trig<Cos>: unknown quadrant " << quad;
        throw std::runtime_error(oss.str());
    }
}

Evaluator::Evaluator(const uint32_t prec) {

    mpz_init(quot);
    mpz_init(rem);

    mpq_init(xq);
    mpq_init(yq);
    mpq_init(argq);

    mpfr_init2(term, prec);
    mpfr_init2(sum, prec);

    // These are important constants, so they have twice the precision
    mpfr_init2(half_pi_d, 2 * prec);
    mpfr_init2(half_pi_u, 2 * prec);

    mpfr_const_pi(half_pi_d, MPFR_RNDD);
    mpfr_const_pi(half_pi_u, MPFR_RNDU);

    mpfr_div_ui(half_pi_d, half_pi_d, 2, MPFR_RNDD);
    mpfr_div_ui(half_pi_u, half_pi_u, 2, MPFR_RNDU);
}

Evaluator::~Evaluator() {
    mpz_clear(quot);
    mpz_clear(rem);

    mpq_clear(xq);
    mpq_clear(yq);
    mpq_clear(argq);

    mpfr_clear(term);
    mpfr_clear(sum);

    mpfr_clear(half_pi_d);
    mpfr_clear(half_pi_u);
}

template <template <typename> class Trig>
bool Evaluator::is_positive(const Equation<Trig>& eq, const Coeff64 bx, const Coeff64 by,
                            const PointQ& center, const Rational& rx, const Rational& ry) {

    mpfr_clear_flags();

    mpfr_set_zero(sum, 0);

    for (const auto& kv : eq) {

        // t_coeff * trig(x_coeff * center.x * pi/2 + y_coeff * center.y * pi/2)

        const auto x_coeff = kv.first.arg.coeff(XY::X);
        const auto y_coeff = kv.first.arg.coeff(XY::Y);
        const auto t_coeff = kv.second;

        mpq_set_si(xq, x_coeff, 1);
        mpq_set_si(yq, y_coeff, 1);

        mpq_mul(xq, xq, center.x.backend().data());
        mpq_mul(yq, yq, center.y.backend().data());

        mpq_add(argq, xq, yq);

        mpz_fdiv_qr(quot, rem, mpq_numref(argq), mpq_denref(argq));

        // Reuse xq, yq, argq

        mpq_set_z(xq, rem);
        mpq_set_z(yq, mpq_denref(argq));

        // This is the remainder fraction. 0 <= argq < 1
        mpq_div(argq, xq, yq);

        // The return value of this function is the remainder
        const auto quad = mpz_fdiv_ui(quot, 4);

        // We can ignore the quotient part of the above remainder, since
        // sin(x + 2pi) = sin(x)
        // cos(x + 2pi) = cos(x)

        eval_trig<Trig>(term, t_coeff, argq, quad, half_pi_d, half_pi_u);

        mpfr_add(sum, sum, term, MPFR_RNDD);
    }

    // Reuse xq, yq, argq
    mpq_set_si(xq, bx, 1);
    mpq_set_si(yq, by, 1);

    mpq_mul(xq, xq, rx.backend().data());
    mpq_mul(yq, yq, ry.backend().data());

    mpq_add(argq, xq, yq);

    // Reuse term
    mpfr_mul_q(term, half_pi_u, argq, MPFR_RNDU);

    const bool is_pos = mpfr_greater_p(sum, term);

    // Now we check for errors.
    // We don't check the INEXACT flag, because that one will be set when evaluating the trig functions
#if 0
    const auto mask = MPFR_FLAGS_UNDERFLOW | MPFR_FLAGS_OVERFLOW | MPFR_FLAGS_NAN | MPFR_FLAGS_ERANGE;

    const auto flags = mpfr_flags_test(mask);

    if (flags != 0) {

        std::ostringstream oss{};
        oss << "error: flags raised in calculation of " << eq
            << " at point " << center
            << " with radius " << radius << '\n';

        if (flags & MPFR_FLAGS_UNDERFLOW) {
            oss << "underflow\n";
        }

        if (flags & MPFR_FLAGS_OVERFLOW) {
            oss << "overflow\n";
        }

        //if (flags & MPFR_FLAGS_DIVBY0) {
            //oss << "divby0\n";
        //}

        if (flags & MPFR_FLAGS_NAN) {
            oss << "nan\n";
        }

        if (flags & MPFR_FLAGS_ERANGE) {
            oss << "erange\n";
        }

        throw std::runtime_error(oss.str());
    }
#endif

    const bool err = mpfr_underflow_p() || mpfr_overflow_p() || mpfr_nanflag_p() || mpfr_erangeflag_p();

    if (err) {

        std::ostringstream oss{};
        oss << "error: flags raised in calculation of " << eq
            << " at point " << center
            << " with radius " << rx << " " << ry << '\n';

        if (mpfr_underflow_p()) {
            oss << "underflow\n";
        }

        if (mpfr_overflow_p()) {
            oss << "overflow\n";
        }

        if (mpfr_nanflag_p()) {
            oss << "nan\n";
        }

        if (mpfr_erangeflag_p()) {
            oss << "erange\n";
        }

        throw std::runtime_error(oss.str());
    }

    return is_pos;
}

template bool Evaluator::is_positive(const Equation<Sin>& eq, const Coeff64 bx, const Coeff64 by,
                                     const PointQ& center, const Rational& rx, const Rational& ry);
template bool Evaluator::is_positive(const Equation<Cos>& eq, const Coeff64 bx, const Coeff64 by,
                                     const PointQ& center, const Rational& rx, const Rational& ry);

template <template <typename> class Trig>
bool Evaluator::is_positive(const EqVec<Trig>& eq, const Coeff64 bound, const PointQ& center, const Rational& radius) {

    mpfr_clear_flags();

    mpfr_set_zero(sum, 0);

    for (const auto& kv : eq) {

        // t_coeff * trig(x_coeff * center.x * pi/2 + y_coeff * center.y * pi/2)

        const auto x_coeff = kv.first.arg.coeff(XY::X);
        const auto y_coeff = kv.first.arg.coeff(XY::Y);
        const auto t_coeff = kv.second;

        mpq_set_si(xq, x_coeff, 1);
        mpq_set_si(yq, y_coeff, 1);

        mpq_mul(xq, xq, center.x.backend().data());
        mpq_mul(yq, yq, center.y.backend().data());

        mpq_add(argq, xq, yq);

        mpz_fdiv_qr(quot, rem, mpq_numref(argq), mpq_denref(argq));

        // Reuse xq, yq, argq

        mpq_set_z(xq, rem);
        mpq_set_z(yq, mpq_denref(argq));

        // This is the remainder fraction. 0 <= argq < 1
        mpq_div(argq, xq, yq);

        // The return value of this function is the remainder
        const auto quad = mpz_fdiv_ui(quot, 4);

        // We can ignore the quotient part of the above remainder, since
        // sin(x + 2pi) = sin(x)
        // cos(x + 2pi) = cos(x)

        // We want everything we add to the sum to be a small (most negative) as possible
        // We want to have a lower bound, so we round down and stuff

        eval_trig<Trig>(term, t_coeff, argq, quad, half_pi_d, half_pi_u);

        mpfr_add(sum, sum, term, MPFR_RNDD);
    }

    // Reuse argq
    mpq_set_si(argq, bound, 1);
    mpq_mul(argq, argq, radius.backend().data());

    // Reuse term
    mpfr_mul_q(term, half_pi_u, argq, MPFR_RNDU);

    const bool is_pos = mpfr_greater_p(sum, term);
    /*if(!is_pos && radius == Rational{0.125} && (center == PointQ{Rational{0.125}, Rational{0.625}} || center == PointQ{Rational{0.125}, Rational{0.875}})) {
        mpfr_printf ("--------------------\n");
        mpfr_printf ("Sum is %.60Rf\n", sum);
        mpfr_printf ("term is %.60Rf\n", term);
        mpfr_printf ("--------------------\n");
    }*/
    // Now we check for errors.
    // We don't check the INEXACT flag, because that one will be set when evaluating the trig functions
#if 0
    const auto mask = MPFR_FLAGS_UNDERFLOW | MPFR_FLAGS_OVERFLOW | MPFR_FLAGS_NAN | MPFR_FLAGS_ERANGE;

    const auto flags = mpfr_flags_test(mask);

    if (flags != 0) {

        std::ostringstream oss{};
        oss << "error: flags raised in calculation of " << eq
            << " at point " << center
            << " with radius " << radius << '\n';

        if (flags & MPFR_FLAGS_UNDERFLOW) {
            oss << "underflow\n";
        }

        if (flags & MPFR_FLAGS_OVERFLOW) {
            oss << "overflow\n";
        }

        //if (flags & MPFR_FLAGS_DIVBY0) {
            //oss << "divby0\n";
        //}

        if (flags & MPFR_FLAGS_NAN) {
            oss << "nan\n";
        }

        if (flags & MPFR_FLAGS_ERANGE) {
            oss << "erange\n";
        }

        throw std::runtime_error(oss.str());
    }
#endif

    const bool err = mpfr_underflow_p() || mpfr_overflow_p() || mpfr_nanflag_p() || mpfr_erangeflag_p();

    if (err) {

        std::ostringstream oss{};
        oss << "error: flags raised in calculation of " << eq
            << " at point " << center
            << " with radius " << radius << '\n';

        if (mpfr_underflow_p()) {
            oss << "underflow\n";
        }

        if (mpfr_overflow_p()) {
            oss << "overflow\n";
        }

        if (mpfr_nanflag_p()) {
            oss << "nan\n";
        }

        if (mpfr_erangeflag_p()) {
            oss << "erange\n";
        }

        throw std::runtime_error(oss.str());
    }

    return is_pos;
}

template bool Evaluator::is_positive(const EqVec<Sin>& eq, const Coeff64 bound,
                                     const PointQ& center, const Rational& radius);
template bool Evaluator::is_positive(const EqVec<Cos>& eq, const Coeff64 bound,
                                     const PointQ& center, const Rational& radius);
