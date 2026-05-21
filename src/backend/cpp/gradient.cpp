#include "gradient.hpp"
#include "evalf.hpp"
#include "trig_identities.hpp"

// So far, when gradf(a, b) = 0, we do a full factor, and
// grad f_factored(a, b) /= 0. So other lines in there (ones other
// than sin(x), sin(y), or sin(x + y)) don't show up. Alright, so
// that means we're pretty safe.

// So the premise is this: we have a function f at a point (a, b)
// and f(a, b) = 0 and grad f(a, b) = 0. What does this mean? Well,
// it could mean that f has a local min, max, or saddle point at
// (a, b), and is also shifted down. Or, it could mean there is
// an intersection of a two curves there. How do we determine which
// one? Well, we could check the Hessian, and if det H(a, b) = 0,
// then it is semidefinite. This doesn't guarantee that we have
// an intersection, but det H(a, b) != 0 would indicate we don't.

// (0, 0)
// could get all three lines for this case
static bool is_zero_zero(const Vector2<Interval>& point) {

    const auto coord0_sign = sign(point[0]);
    const auto coord1_sign = sign(point[1]);

    return coord0_sign == Sign::ZERO && coord1_sign == Sign::ZERO;
}

// (0, pi/2)
static bool is_zero_pi2(const Vector2<Interval>& point) {

    const auto coord0_sign = sign(point[0]);

    const Interval coord1_diff = point[1] - boost::math::constants::half_pi<Interval>();
    const auto coord1_sign = sign(coord1_diff);

    return coord0_sign == Sign::ZERO && coord1_sign == Sign::ZERO;
}

// (pi/2, 0)
static bool is_pi2_zero(const Vector2<Interval>& point) {

    const Interval coord0_diff = point[0] - boost::math::constants::half_pi<Interval>();
    const auto coord0_sign = sign(coord0_diff);

    const auto coord1_sign = sign(point[1]);

    return coord0_sign == Sign::ZERO && coord1_sign == Sign::ZERO;
}

// (pi/2, pi/2)
static bool is_pi2_pi2(const Vector2<Interval>& point) {

    const Interval coord0_diff = point[0] - boost::math::constants::half_pi<Interval>();
    const auto coord0_sign = sign(coord0_diff);

    const Interval coord1_diff = point[1] - boost::math::constants::half_pi<Interval>();
    const auto coord1_sign = sign(coord1_diff);

    return coord0_sign == Sign::ZERO && coord1_sign == Sign::ZERO;
}

constexpr uint32_t max_exp = 10;

template <template <typename> class Trig>
std::pair<Coeff64, Coeff64> gradient_zero_pi2(const LinComMapZ<Trig<LinComArrZ<XY>>>& f) {
    // (0, pi/2)
    // f = sin(a)^n * g

    // TODO double check the Hessian?

    // (n + 1)!
    Coeff64 factorial = 1;

    // Same type as f
    LinComMapZ<Trig<LinComArrZ<XY>>> diff_a_same{};
    LinComMapZ<Trig<LinComArrZ<XY>>> diff_b_same{};

    // Different type than f
    auto diff_a_different = diff<XY::X>(f);
    auto diff_b_different = diff<XY::Y>(f);

    for (uint32_t n = 1; n < max_exp; ++n) {

        // Update the variables
        factorial *= (n + 1);

        Coeff64 eval_a;
        Coeff64 eval_b;
        if (n % 2 == 0) {
            diff_a_different = diff<XY::X>(diff_a_same);
            diff_b_different = diff<XY::X>(diff_b_same);

            eval_a = simplify_lin_com_pi2_pi2(diff_a_different);
            eval_b = simplify_lin_com_pi2_pi2(diff_b_different);
        } else {
            diff_a_same = diff<XY::X>(diff_a_different);
            diff_b_same = diff<XY::X>(diff_b_different);

            eval_a = simplify_lin_com_pi2_pi2(diff_a_same);
            eval_b = simplify_lin_com_pi2_pi2(diff_b_same);
        }

        if (eval_a != 0 || eval_b != 0) {
            // TODO double check this divides properly
            auto g_a = eval_a / factorial;
            auto g_b = eval_b * (n + 1) / factorial;

            // Divide out the factors of two
            for (uint32_t i = 0; i < n; ++i) {
                g_a /= 2;
                g_b /= 2;
            }

            return {g_a, g_b};
        }
    }

    std::ostringstream err;
    err << "zero gradient after 10 divisions for " << '\n'
        << f << '\n'
        << " at (0, pi/2)";

    throw std::runtime_error(err.str());
}

template <template <typename> class Trig>
std::pair<Coeff64, Coeff64> gradient_pi2_zero(const LinComMapZ<Trig<LinComArrZ<XY>>>& f) {
    // (pi/2, zero)
    // f = sin(b)^n * g

    // TODO double check the Hessian?

    // (n + 1)!
    Coeff64 factorial = 1;

    // Same type as f
    LinComMapZ<Trig<LinComArrZ<XY>>> diff_a_same{};
    LinComMapZ<Trig<LinComArrZ<XY>>> diff_b_same{};

    // Different type than f
    auto diff_a_different = diff<XY::X>(f);
    auto diff_b_different = diff<XY::Y>(f);

    for (uint32_t n = 1; n < max_exp; ++n) {

        // Update the variables
        factorial *= (n + 1);

        Coeff64 eval_a;
        Coeff64 eval_b;
        if (n % 2 == 0) {
            diff_a_different = diff<XY::Y>(diff_a_same);
            diff_b_different = diff<XY::Y>(diff_b_same);

            eval_a = simplify_lin_com_pi2_pi2(diff_a_different);
            eval_b = simplify_lin_com_pi2_pi2(diff_b_different);
        } else {
            diff_a_same = diff<XY::Y>(diff_a_different);
            diff_b_same = diff<XY::Y>(diff_b_different);

            eval_a = simplify_lin_com_pi2_pi2(diff_a_same);
            eval_b = simplify_lin_com_pi2_pi2(diff_b_same);
        }

        if (eval_a != 0 || eval_b != 0) {
            // TODO double check this divides properly
            auto g_a = eval_a * (n + 1) / factorial;
            auto g_b = eval_b / factorial;

            // Divide out the factors of two
            for (uint32_t i = 0; i < n; ++i) {
                g_a /= 2;
                g_b /= 2;
            }

            return {g_a, g_b};
        }
    }

    std::ostringstream err;
    err << "zero gradient after 10 divisions for " << '\n'
        << f << '\n'
        << " at (pi/2, 0)";

    throw std::runtime_error(err.str());
}

template <template <typename> class Trig>
std::pair<Coeff64, Coeff64> gradient_pi2_pi2(const LinComMapZ<Trig<LinComArrZ<XY>>>& f) {
    // (pi/2, pi/2)
    // f = sin(a + b)^n * g

    // TODO double check the Hessian?

    Coeff64 sign = 1;
    Coeff64 factorial = 1;

    // Same type as f
    LinComMapZ<Trig<LinComArrZ<XY>>> diff_a_same{};
    LinComMapZ<Trig<LinComArrZ<XY>>> diff_b_same{};

    // Different type than f
    auto diff_a_different = diff<XY::X>(f);
    auto diff_b_different = diff<XY::Y>(f);

    for (uint32_t n = 1; n < max_exp; ++n) {

        // Update the variables
        sign *= -1;
        factorial *= (n + 1);

        Coeff64 eval_a;
        Coeff64 eval_b;
        if (n % 2 == 0) {
            diff_a_different = diff<XY::X>(diff_a_same);
            diff_b_different = diff<XY::Y>(diff_b_same);

            eval_a = simplify_lin_com_pi2_pi2(diff_a_different);
            eval_b = simplify_lin_com_pi2_pi2(diff_b_different);
        } else {
            diff_a_same = diff<XY::X>(diff_a_different);
            diff_b_same = diff<XY::Y>(diff_b_different);

            eval_a = simplify_lin_com_pi2_pi2(diff_a_same);
            eval_b = simplify_lin_com_pi2_pi2(diff_b_same);
        }

        if (eval_a != 0 || eval_b != 0) {
            // TODO double check this divides properly
            auto g_a = sign * eval_a / factorial;
            auto g_b = sign * eval_b / factorial;

            // Divide out the factors of two
            for (uint32_t i = 0; i < n; ++i) {
                g_a /= 2;
                g_b /= 2;
            }

            return {g_a, g_b};
        }
    }

    std::ostringstream err;
    err << "zero gradient after 10 divisions for " << '\n'
        << f << '\n'
        << " at (pi/2, pi/2)";

    throw std::runtime_error(err.str());
}

template <template <typename> class Trig>
std::pair<Coeff64, Coeff64> zero_gradient(const LinComMapZ<Trig<LinComArrZ<XY>>>& lin_com, const Vector2<Interval>& point) {

    if (is_zero_zero(point)) {

    } else if (is_zero_pi2(point)) {
        return gradient_zero_pi2(lin_com);
    } else if (is_pi2_zero(point)) {
        return gradient_pi2_zero(lin_com);
    } else if (is_pi2_pi2(point)) {
        return gradient_pi2_pi2(lin_com);
    }

    // throw exception
    std::ostringstream err{};
    err << "undealt with case in zero_gradient:\n"
        << lin_com << '\n'
        << point;
    throw std::runtime_error(err.str());
}

Vector2<Interval> gradient(const EquationGradient<XY, LinComArrZ<XYEta>>& eq_grad, const Vector2<Interval>& point) {
    const Interval x = evalf<Interval>(eq_grad.diff0, point[0], point[1]);
    const Interval y = evalf<Interval>(eq_grad.diff1, point[0], point[1]);

    const auto sign_x = sign(x);
    const auto sign_y = sign(y);

    if (sign_x != Sign::ZERO || sign_y != Sign::ZERO) {
        return {x, y};
    }

    // This should always have a nonzero gradient
    std::ostringstream err;
    err << "zero gradient for " << eq_grad.equation << " at " << point;
    throw std::runtime_error(err.str());
}

template <template <typename> class Trig>
Vector2<Interval> gradient(const EquationGradient<XY, Equation<Trig>>& eq_grad, const Vector2<Interval>& point) {

    const Interval x = evalf<Interval>(eq_grad.diff0, point[0], point[1]);
    const Interval y = evalf<Interval>(eq_grad.diff1, point[0], point[1]);

    const auto sign_x = sign(x);
    const auto sign_y = sign(y);

    if (sign_x != Sign::ZERO || sign_y != Sign::ZERO) {
        return {x, y};
    }

    // Zero gradient
    // TODO make sure we don't repeat calculating derivatives
    const auto grad = zero_gradient(eq_grad.equation, point);

    return {grad.first, grad.second};
}

template Vector2<Interval> gradient(const EquationGradient<XY, Equation<Sin>>& eq_grad, const Vector2<Interval>& point);
template Vector2<Interval> gradient(const EquationGradient<XY, Equation<Cos>>& eq_grad, const Vector2<Interval>& point);
