#include "shooting_vectors.hpp"
#include "trig_identities.hpp"

static LinComArrZ<XYPi> shooting_angle_odd(const CodeSequence& code_sequence, const std::vector<LinComArrZ<XYPi>>& code_angles) {

    const auto& code_numbers = code_sequence.numbers();

    LinComArrZ<XYPi> shooting_angle{0, 0, 1};

    for (size_t i = 0; i < code_numbers.size(); i += 1) {
        const auto number = code_numbers.at(i);
        const auto& angle = code_angles.at(i);

        // even indices are negative
        // odd indices are positive
        if ((i % 2) == 0) {
            shooting_angle.sub(number, angle);
        } else {
            shooting_angle.add(number, angle);
        }
    }

    // Once Z is substituted out (which we have done already), all the coefficients should be
    // even. TODO should we prove this is always the case, so we don't have to check?
    // In that case, it would be nice to make the code_numbers be in a sum type for each type
    // of code number
    for (auto& coeff : shooting_angle) {
        if (coeff % 2 != 0) {
            std::ostringstream err;
            err << code_numbers << ": odd coefficient in shooting_angle_odd";
            throw std::runtime_error(err.str());
        }

        coeff /= 2;
    }

    return shooting_angle;
}

static LinComArrZ<XYEta> shooting_angle_closed(const CodeSequence& code_sequence, const std::vector<LinComArrZ<XYEta>>& code_angles) {

    const auto closed_index = *code_sequence.closed_index();

    const auto& code_numbers = code_sequence.numbers();

    const auto closed_number = code_numbers.at(closed_index);

    const auto& closed_angle = code_angles.at(closed_index);

    // theta_{closed_index} = pi / 2 - closed_number / 2 * closed_angle
    const LinComArrZ<XYEta> perp_angle = [&]() {
        LinComArrZ<XYEta> builder{0, 0, 1};
        builder.sub(closed_number / 2, closed_angle); // closed_number is guaranteed to be even
        return builder;
    }();

    // now we derive another expression for the perp_angle and solve for the shooting angle
    LinComArrZ<XYEta> running_sum{};

    for (size_t i = 0; i < closed_index; i += 1) {
        running_sum.scale(-1);

        const auto number = code_numbers.at(i);
        const auto& angle = code_angles.at(i);

        running_sum.sub(number, angle);
    }

    LinComArrZ<XYEta> shooting_angle{};
    if (closed_index % 2 == 0) {
        // shooting_angle + running_sum = perp_angle
        // => shooting_angle = perp_angle - running_sum;
        shooting_angle.add(perp_angle);
        shooting_angle.sub(running_sum);
    } else {
        // pi - shooting_angle + running_sum = perp_angle
        // => shooting_angle = running_sum - perp_angle + pi
        shooting_angle.add(running_sum);
        shooting_angle.sub(perp_angle);
        shooting_angle.add(2, XYEta::Eta);
    }

    return shooting_angle;
}

std::pair<LinComMapZ<Cos<LinComArrZ<XY>>>, LinComMapZ<Sin<LinComArrZ<XY>>>> shooting_vector_open(const CodeSequence& code_sequence, const std::vector<LinComArrZ<XYPi>>& code_angles) {

    auto shooting_angle = shooting_angle_odd(code_sequence, code_angles);

    // make it polar
    shooting_angle.scale(-1);
    shooting_angle.add(XYPi::Pi);

    // cos is x coord, sin is y coord
    const auto cos = simplify_cos_xypi(shooting_angle);
    LinComMapZ<Cos<LinComArrZ<XY>>> cos_builder{};
    cos_builder.add(cos.first, cos.second);

    //std::cout << "I am sin_open!!!! " << cos << std::endl;

    const auto sin = simplify_sin_xypi(shooting_angle);
    LinComMapZ<Sin<LinComArrZ<XY>>> sin_builder{};
    sin_builder.add(sin.first, sin.second);

    //std::cout << "I am cos_open!!!! " << sin << std::endl;

    return {cos_builder, sin_builder};
}

std::pair<LinComMapZ<Sin<LinComArrZ<XY>>>, LinComMapZ<Cos<LinComArrZ<XY>>>> shooting_vector_closed(const CodeSequence& code_sequence, const std::vector<LinComArrZ<XYEta>>& code_angles) {

    auto shooting_angle = shooting_angle_closed(code_sequence, code_angles);

    // make it polar
    shooting_angle.scale(-1);
    shooting_angle.add(2, XYEta::Eta);

    // sin is x coord, cos is y coord in this case
    const auto sin = simplify_cos_xyeta(shooting_angle);

    //std::cout << "I am sin!!!! " << sin << std::endl;

    LinComMapZ<Sin<LinComArrZ<XY>>> sin_builder{};
    sin_builder.add(sin.first, sin.second);

    const auto cos = simplify_sin_xyeta(shooting_angle);
    LinComMapZ<Cos<LinComArrZ<XY>>> cos_builder{};
    cos_builder.add(cos.first, cos.second);

    //std::cout << "I am cos!!!! " << cos << std::endl;

    return {sin_builder, cos_builder};
}
