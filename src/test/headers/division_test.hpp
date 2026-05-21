#pragma once

#include <division.hpp>
#include <parse.hpp>

BOOST_AUTO_TEST_CASE(test_divide_once_sin_sin) {

    const std::vector<std::array<std::string, 3>> inputs = {
        {{"sin(14x+y)+sin(22x-y)", "sin(18x)", "cos(4x-y)"}},
    };

    for (const auto& arr : inputs) {
        const auto dividend = parse_lin_com_map_sin_xy(std::get<0>(arr));
        const auto divisor = parse_lin_com_map_sin_xy(std::get<1>(arr));
        const auto quotient = parse_lin_com_map_cos_xy(std::get<2>(arr));

        BOOST_TEST(*divide_once(dividend, divisor) == quotient);
    }
}

BOOST_AUTO_TEST_CASE(test_divide_once_sin_cos) {

    const std::vector<std::array<std::string, 3>> inputs = {
        {{"sin(14x+y)+sin(22x-y)", "cos(4x-y)", "sin(18x)"}},
    };

    for (const auto& arr : inputs) {
        const auto dividend = parse_lin_com_map_sin_xy(std::get<0>(arr));
        const auto divisor = parse_lin_com_map_cos_xy(std::get<1>(arr));
        const auto quotient = parse_lin_com_map_sin_xy(std::get<2>(arr));

        BOOST_TEST(*divide_once(dividend, divisor) == quotient);
    }
}

BOOST_AUTO_TEST_CASE(test_divide_once_cos_sin) {

    const std::vector<std::array<std::string, 3>> inputs = {
        {{"-cos(x+2y)-cos(9x)+cos(11x+2y)-cos(25x-2y)+cos(27x)+cos(35x-2y)", "sin(18x)", "sin(7x-2y)-sin(9x)-sin(17x-2y)"}},
    };

    for (const auto& arr : inputs) {
        const auto dividend = parse_lin_com_map_cos_xy(std::get<0>(arr));
        const auto divisor = parse_lin_com_map_sin_xy(std::get<1>(arr));
        const auto quotient = parse_lin_com_map_sin_xy(std::get<2>(arr));

        BOOST_TEST(*divide_once(dividend, divisor) == quotient);
    }
}

BOOST_AUTO_TEST_CASE(test_divide_once_cos_cos) {

    const std::vector<std::array<std::string, 3>> inputs = {
        {{"cos(5x+4y)+cos(31x-8y)", "cos(13x-6y)", "cos(18x-2y)"}},
    };

    for (const auto& arr : inputs) {
        const auto dividend = parse_lin_com_map_cos_xy(std::get<0>(arr));
        const auto divisor = parse_lin_com_map_cos_xy(std::get<1>(arr));
        const auto quotient = parse_lin_com_map_cos_xy(std::get<2>(arr));

        BOOST_TEST(*divide_once(dividend, divisor) == quotient);
    }
}
