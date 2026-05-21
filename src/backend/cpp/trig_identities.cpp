#include "trig_identities.hpp"

// NOTE: We can eliminate the scaling by one half when applying the trig identities, because we can
// factor
// them out nicely each time from the final equation, and then divide and get rid of them. Uber
// awesome.
// Sooooo, we can use a long in this case. Heurestics show that usually we need to divide by 2 each
// time
// when reducing the numerator. That is totally acceptable.

// In general, there are two types of identities we can apply:
// - sign identities. These are sin(-x) = -sin(x) and cos(-x) = cos(x)
// - shift identities. These are sin(x + n*eta) = +/- sin/cos(x)
// When applying these, first apply the shift, then apply the sign to what is left

// take out a minus sign if necessary
// take out a minus sign if necessary
// return 0 if arg is 0
std::pair<Coeff64, Sin<LinComArrZ<XY>>> simplify_sin_xy(const LinComArrZ<XY>& arg) {

    const auto x_coeff = arg.coeff<XY::X>();
    const auto y_coeff = arg.coeff<XY::Y>();

    const auto leading_coeff = (x_coeff != 0) ? x_coeff : y_coeff;

    const auto sign = signum(leading_coeff);

    auto new_arg = arg;
    new_arg.scale(sign);

    const Sin<LinComArrZ<XY>> sin{new_arg};

    // sin(-x) = -sin(x)
    return {sign, sin};
}

// Take out a minus sign
std::pair<Coeff64, Cos<LinComArrZ<XY>>> simplify_cos_xy(const LinComArrZ<XY>& arg) {

    const auto x_coeff = arg.coeff<XY::X>();
    const auto y_coeff = arg.coeff<XY::Y>();

    const auto leading_coeff = (x_coeff != 0) ? x_coeff : y_coeff;

    const auto sign = signum(leading_coeff);

    auto new_arg = arg;
    new_arg.scale(sign);

    // cos(-x) = cos(x)
    const Cos<LinComArrZ<XY>> cos{new_arg};

    // It strictly speaking isn't necessary to return the 1, but it keeps the interface
    // consistent. The compiler will also likely apply constant propagation to optimize
    // it out anyway.
    return {1, cos};
}

std::pair<Coeff64, Sin<LinComArrZ<XY>>> simplify_sin_xypi(const LinComArrZ<XYPi>& arg) {

    const auto x_coeff = arg.coeff<XYPi::X>();
    const auto y_coeff = arg.coeff<XYPi::Y>();
    const auto pi_coeff = arg.coeff<XYPi::Pi>();

    const auto coeff = (x_coeff != 0) ? x_coeff : y_coeff;

    const auto var_sign = signum(coeff);
    const auto pi_sign = (pi_coeff % 2 == 0) ? 1 : -1;

    LinComArrZ<XY> new_arg{var_sign * x_coeff, var_sign * y_coeff};

    // sin(-x) = -sin(x)
    const auto sin_coeff = var_sign * pi_sign;
    const Sin<LinComArrZ<XY>> sin{new_arg};

    return {sin_coeff, sin};
}

std::pair<Coeff64, Cos<LinComArrZ<XY>>> simplify_cos_xypi(const LinComArrZ<XYPi>& arg) {

    const auto x_coeff = arg.coeff<XYPi::X>();
    const auto y_coeff = arg.coeff<XYPi::Y>();
    const auto pi_coeff = arg.coeff<XYPi::Pi>();

    const auto coeff = (x_coeff != 0) ? x_coeff : y_coeff;

    const auto var_sign = (coeff >= 0) ? 1 : -1;
    const auto pi_sign = (pi_coeff % 2 == 0) ? 1 : -1;

    LinComArrZ<XY> new_arg{var_sign * x_coeff, var_sign * y_coeff};

    // cos(-x) = cos(x)
    const auto cos_coeff = pi_sign;
    const Cos<LinComArrZ<XY>> cos{new_arg};

    return {cos_coeff, cos};
}

// sin(a + n*pi/2) = +/- cos(a), where n is odd
std::pair<Coeff64, Cos<LinComArrZ<XY>>> simplify_sin_xyeta(const LinComArrZ<XYEta>& arg) {

    const auto x_coeff = arg.coeff<XYEta::X>();
    const auto y_coeff = arg.coeff<XYEta::Y>();
    const auto eta_coeff = arg.coeff<XYEta::Eta>();

    // We need to ensure n is odd
    if (eta_coeff % 2 == 0) {
        std::ostringstream oss{};
        oss << "simplify_sin_xyeta: even eta coeff " << arg;
        throw std::runtime_error(oss.str());
    }

    const auto sign = ((eta_coeff - 1) / 2) % 2 == 0 ? 1 : -1;

    LinComArrZ<XY> new_arg{x_coeff, y_coeff};

    const auto cos = simplify_cos_xy(new_arg);

    return {sign * cos.first, cos.second};
}

// cos(a + n*pi/2) = +/- sin(a), where n is odd
std::pair<Coeff64, Sin<LinComArrZ<XY>>> simplify_cos_xyeta(const LinComArrZ<XYEta>& arg) {

    const auto x_coeff = arg.coeff<XYEta::X>();
    const auto y_coeff = arg.coeff<XYEta::Y>();
    const auto eta_coeff = arg.coeff<XYEta::Eta>();

    // We need to ensure n is odd
    if (eta_coeff % 2 == 0) {
        std::ostringstream oss{};
        oss << "simplify_cos_xyeta: even eta coeff " << arg;
        throw std::runtime_error(oss.str());
    }

    const auto sign = ((eta_coeff - 1) / 2) % 2 == 0 ? -1 : 1;

    LinComArrZ<XY> new_arg{x_coeff, y_coeff};

    const auto sin = simplify_sin_xy(new_arg);

    return {sign * sin.first, sin.second};
}

// cos(a) * sin(b) = 1/2 sin(a + b) - 1/2 sin(a - b)
LinComMapZ<Sin<LinComArrZ<XY>>> multiply_lin_com(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos, const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_sin) {

    LinComMapZ<Sin<LinComArrZ<XY>>> result{};

    for (const auto& cos_key : lin_com_cos) {
        const auto& cos_arg = cos_key.first.arg;
        const auto cos_coeff = cos_key.second;

        for (const auto& sin_key : lin_com_sin) {
            const auto& sin_arg = sin_key.first.arg;
            const auto sin_coeff = sin_key.second;

            // This must distribute over the sum
            const auto prod_coeff = cos_coeff * sin_coeff;

            const auto sum = LinComArrZ<XY>::add(cos_arg, sin_arg);

            // We still need to do a simplify here, since we might need
            // to pull a minus sign out front.
            const auto sin1 = simplify_sin_xy(sum);

            result.add(prod_coeff * sin1.first, sin1.second);

            const auto diff = LinComArrZ<XY>::sub(cos_arg, sin_arg);

            const auto sin2 = simplify_sin_xy(diff);

            result.sub(prod_coeff * sin2.first, sin2.second);
        }
    }

    return result;
}
// cos(a) * sin(b) = 1/2 sin(a + b) - 1/2 sin(a - b)
LinComMapZ<Sin<LinComArrZ<XY>>> multiply_lin_com(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_sin, const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos) {

    LinComMapZ<Sin<LinComArrZ<XY>>> result{};

    for (const auto& cos_key : lin_com_cos) {
        const auto& cos_arg = cos_key.first.arg;
        const auto cos_coeff = cos_key.second;

        for (const auto& sin_key : lin_com_sin) {
            const auto& sin_arg = sin_key.first.arg;
            const auto sin_coeff = sin_key.second;

            // This must distribute over the sum
            const auto prod_coeff = cos_coeff * sin_coeff;

            const auto sum = LinComArrZ<XY>::add(cos_arg, sin_arg);

            // We still need to do a simplify here, since we might need
            // to pull a minus sign out front.
            const auto sin1 = simplify_sin_xy(sum);

            result.add(prod_coeff * sin1.first, sin1.second);

            const auto diff = LinComArrZ<XY>::sub(cos_arg, sin_arg);

            const auto sin2 = simplify_sin_xy(diff);

            result.sub(prod_coeff * sin2.first, sin2.second);
        }
    }

    return result;
}

// sin(a) * sin(b) = 1/2 cos(a - b) - 1/2 cos(a + b)
LinComMapZ<Cos<LinComArrZ<XY>>> multiply_lin_com(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_sin1, const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_sin2) {

    LinComMapZ<Cos<LinComArrZ<XY>>> result{};

    for (const auto& sin1_key : lin_com_sin1) {
        const auto& sin1_arg = sin1_key.first.arg;
        const auto sin1_coeff = sin1_key.second;

        for (const auto& sin2_key : lin_com_sin2) {
            const auto& sin2_arg = sin2_key.first.arg;
            const auto sin2_coeff = sin2_key.second;

            // This must distribute over the sum
            const auto prod_coeff = sin1_coeff * sin2_coeff;

            const auto diff = LinComArrZ<XY>::sub(sin1_arg, sin2_arg);

            // We still need to do a simplify here, since we might need
            // to pull a minus sign out front.
            const auto cos1 = simplify_cos_xy(diff);

            result.add(prod_coeff * cos1.first, cos1.second);

            const auto sum = LinComArrZ<XY>::add(sin1_arg, sin2_arg);

            const auto cos2 = simplify_cos_xy(sum);

            result.sub(prod_coeff * cos2.first, cos2.second);
        }
    }

    return result;
}
/*
//for cos
LinComMapZ<Cos<LinComArrZ<XY>>> add_lin_com(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos1, const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos2) {

    LinComMapZ<Cos<LinComArrZ<XY>>> result{};

    for (const auto& cos1_key : lin_com_cos1) {
        const auto& cos1_arg = cos1_key.first.arg;
        const auto cos1_coeff = cos1_key.second;

        for (const auto& cos2_key : lin_com_cos2) {
            const auto& cos2_arg = cos2_key.first.arg;
            const auto cos2_coeff = cos2_key.second;
            const auto prod_coeff =  cos1_coeff+cos2_coeff;

            const auto cos1 = simplify_cos_xy(cos1_arg);
            const auto cos2 = simplify_cos_xy(cos2_arg);
            if(cos1.second==cos2.second)
            {
               result.add(prod_coeff, cos1.second);
            }
            else{
                result.add(cos1_coeff, cos1.second );
                result.add(cos2_coeff, cos2.second);
            }

        }
    }

    return result;
}*/

/*LinComMapZ<Sin<LinComArrZ<XY>>> add_lin_com(LinComMapZ<Sin<LinComArrZ<XY>>> lin_com_cos1, const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_cos2)
{
        lin_com_cos1.add(lin_com_cos2);
        return lin_com_cos1;
}
LinComMapZ<Cos<LinComArrZ<XY>>> add_lin_com(LinComMapZ<Cos<LinComArrZ<XY>>> lin_com_cos1, const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos2)
{
        lin_com_cos1.add(lin_com_cos2);
        return lin_com_cos1 ;
}*/
LinComMapZ<Cos<LinComArrZ<XY>>> get_final_result_formula(const LinComMapZ<Cos<LinComArrZ<XY>>> lin_com, const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com2, const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com3)
{
        LinComMapZ<Cos<LinComArrZ<XY>>> result{};
        result.sub(lin_com);
        result.add(lin_com2);
        result.add(lin_com2);
        result.sub(lin_com3);
        return result;
}

LinComMapZ<Sin<LinComArrZ<XY>>> get_final_result_formula(const LinComMapZ<Sin<LinComArrZ<XY>>> lin_com, const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com2, const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com3)
{
        LinComMapZ<Sin<LinComArrZ<XY>>> result{};
        result.sub(lin_com);
        result.add(lin_com2);
        result.add(lin_com2);
        result.sub(lin_com3);
        return result;
}

//for sin
/*
LinComMapZ<Sin<LinComArrZ<XY>>> add_lin_com(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_cos1, const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com_cos2) {
    LinComMapZ<Sin<LinComArrZ<XY>>> result{};

    for (const auto& cos1_key : lin_com_cos1) {
        const auto& cos1_arg = cos1_key.first.arg;
        const auto cos1_coeff = cos1_key.second;

        for (const auto& cos2_key : lin_com_cos2) {
            const auto& cos2_arg = cos2_key.first.arg;
            const auto cos2_coeff = cos2_key.second;
            const auto prod_coeff =  cos1_coeff+cos2_coeff;
            const auto cos1 = simplify_sin_xy(cos1_arg);
            const auto cos2 = simplify_sin_xy(cos2_arg);
            if(cos1.second==cos2.second)
            {
               result.add(prod_coeff, cos1.second);
            }
            else{
                result.add(cos1_coeff, cos1.second );
                result.add(cos2_coeff, cos2.second);
            }
        }
    }
    return result;
}*/
// cos(a) * cos(b) = 1/2 cos(a - b) + 1/2 cos(a + b)
LinComMapZ<Cos<LinComArrZ<XY>>> multiply_lin_com(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos1, const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com_cos2) {

    LinComMapZ<Cos<LinComArrZ<XY>>> result{};

    for (const auto& cos1_key : lin_com_cos1) {
        const auto& cos1_arg = cos1_key.first.arg;
        const auto cos1_coeff = cos1_key.second;

        for (const auto& cos2_key : lin_com_cos2) {
            const auto& cos2_arg = cos2_key.first.arg;
            const auto cos2_coeff = cos2_key.second;

            // This must distribute over the sum
            const auto prod_coeff = cos1_coeff * cos2_coeff;

            const auto diff = LinComArrZ<XY>::sub(cos1_arg, cos2_arg);

            // We still need to do a simplify here, since we might need
            // to pull a minus sign out front.
            const auto cos1 = simplify_cos_xy(diff);

            result.add(prod_coeff * cos1.first, cos1.second);

            const auto sum = LinComArrZ<XY>::add(cos1_arg, cos2_arg);

            const auto cos2 = simplify_cos_xy(sum);

            result.add(prod_coeff * cos2.first, cos2.second);
        }
    }

    return result;
}

// sin(coeff * pi/2)
static Coeff64 simplify_sin_pi2(ParamType<Coeff64> coeff) {

    if (coeff % 2 == 0) {
        // coeff = 2*n,
        return 0;
    } else {
        // coeff = 2*n + 1
        const auto n = (coeff - 1) / 2;
        if (n % 2 == 0) {
            return 1;
        } else {
            return -1;
        }
    }
}

// cos(coeff * pi/2)
static Coeff64 simplify_cos_pi2(ParamType<Coeff64> coeff) {

    if (coeff % 2 == 0) {
        // coeff = 2*n
        const auto n = coeff / 2;
        if (n % 2 == 0) {
            return 1;
        } else {
            return -1;
        }
    } else {
        // coeff = 2*n + 1
        return 0;
    }
}

// a = 0, b = 0
Coeff64 simplify_lin_com_zero_zero(const LinComMapZ<Sin<LinComArrZ<XY>>>&) {
    // sin(0) = 0
    return 0;
}

Coeff64 simplify_lin_com_zero_zero(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com) {
    // cos(0) = 1

    Coeff64 sum = 0;

    for (const auto& kv : lin_com) {
        sum += kv.second;
    }

    return sum;
}

// a = 0, b = pi/2
Coeff64 simplify_lin_com_zero_pi2(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com) {

    Coeff64 sum = 0;

    for (const auto& kv : lin_com) {

        const auto b_coeff = kv.first.arg.coeff<XY::Y>();
        // kv.second * sin(a_coeff*0 + b_coeff*pi/2)

        sum += kv.second * simplify_sin_pi2(b_coeff);
    }

    return sum;
}

// a = 0, b = pi/2
Coeff64 simplify_lin_com_zero_pi2(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com) {

    Coeff64 sum = 0;

    for (const auto& kv : lin_com) {

        const auto b_coeff = kv.first.arg.coeff<XY::Y>();
        // kv.second * cos(a_coeff*0 + b_coeff*pi/2)

        sum += kv.second * simplify_cos_pi2(b_coeff);
    }

    return sum;
}

// a = pi/2, b = 0
Coeff64 simplify_lin_com_pi2_zero(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com) {

    Coeff64 sum = 0;

    for (const auto& kv : lin_com) {

        const auto a_coeff = kv.first.arg.coeff<XY::X>();
        // kv.second * sin(a_coeff*pi/2 + b_coeff*0)

        sum += kv.second * simplify_sin_pi2(a_coeff);
    }

    return sum;
}

// a = pi/2, b = 0
Coeff64 simplify_lin_com_pi2_zero(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com) {

    Coeff64 sum = 0;

    for (const auto& kv : lin_com) {

        const auto a_coeff = kv.first.arg.coeff<XY::X>();
        // kv.second * cos(a_coeff*pi/2 + b_coeff*0)

        sum += kv.second * simplify_cos_pi2(a_coeff);
    }

    return sum;
}

// a = pi/2, b = pi/2
Coeff64 simplify_lin_com_pi2_pi2(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com) {

    Coeff64 sum = 0;

    for (const auto& kv : lin_com) {

        const auto a_coeff = kv.first.arg.coeff<XY::X>();
        const auto b_coeff = kv.first.arg.coeff<XY::Y>();
        // kv.second * sin(a_coeff*pi/2 + b_coeff*pi/2)

        sum += kv.second * simplify_sin_pi2(a_coeff + b_coeff);
    }

    return sum;
}

// a = pi/2, b = pi/2
Coeff64 simplify_lin_com_pi2_pi2(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com) {

    Coeff64 sum = 0;

    for (const auto& kv : lin_com) {

        const auto a_coeff = kv.first.arg.coeff<XY::X>();
        const auto b_coeff = kv.first.arg.coeff<XY::Y>();
        // kv.second * cos(a_coeff*pi/2 + b_coeff*pi/2)

        sum += kv.second * simplify_cos_pi2(a_coeff + b_coeff);
    }

    return sum;
}
