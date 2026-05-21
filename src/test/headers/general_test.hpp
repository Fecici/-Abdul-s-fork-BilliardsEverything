#pragma once

#include <general.hpp>

BOOST_AUTO_TEST_CASE(test_other_angle) {

    const std::vector<std::tuple<XYZ, XYZ, XYZ>> valid_angles = {
        // the first two angles are the input, and the
        // last is the output
        // these are all possible valid inputs
        // in a nicer language, we would just pattern match. Oh well
        std::make_tuple(XYZ::X, XYZ::Y, XYZ::Z),
        std::make_tuple(XYZ::Y, XYZ::X, XYZ::Z),

        std::make_tuple(XYZ::X, XYZ::Z, XYZ::Y),
        std::make_tuple(XYZ::Z, XYZ::X, XYZ::Y),

        std::make_tuple(XYZ::Y, XYZ::Z, XYZ::X),
        std::make_tuple(XYZ::Z, XYZ::Y, XYZ::X),
    };

    for (const auto& tup : valid_angles) {

        const auto zero = std::get<0>(tup);
        const auto one = std::get<1>(tup);
        const auto two = std::get<2>(tup);

        const auto other = other_angle(zero, one);

        BOOST_TEST(other == two);
    }

    // These angles should throw an exception
    const std::vector<std::pair<XYZ, XYZ>> invalid_angles = {
        {XYZ::X, XYZ::X},
        {XYZ::Y, XYZ::Y},
        {XYZ::Z, XYZ::Z},
    };

    for (const auto& p : invalid_angles) {
        const auto zero = p.first;
        const auto one = p.second;

        BOOST_CHECK_THROW(other_angle(zero, one), std::runtime_error);
    }
}
