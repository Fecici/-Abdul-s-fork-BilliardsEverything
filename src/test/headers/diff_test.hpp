#pragma once

#include <diff.hpp>
#include <parse.hpp>

BOOST_AUTO_TEST_CASE(test_diff_sin) {

    // f, df/dx, df/dy
    const std::vector<std::tuple<std::string, std::string, std::string>> input_output = {
        std::make_tuple("sin(x)", "cos(x)", "0"),
        std::make_tuple("sin(3x+4y)", "3cos(3x+4y)", "4cos(3x+4y)"),
        std::make_tuple("-2sin(-4x+y)", "8cos(-4x+y)", "-2cos(-4x+y)"),
        std::make_tuple("sin(x+8y)-sin(4x+2y)", "cos(x+8y)-4cos(4x+2y)", "8cos(x+8y)-2cos(4x+2y)"),
    };

    for (const auto& tup : input_output) {
        const auto in_expr = parse_lin_com_map_sin_xy(std::get<0>(tup));
        const auto in_expr_dx = diff<XY::X>(in_expr);
        const auto in_expr_dy = diff<XY::Y>(in_expr);

        const auto out_expr_dx = parse_lin_com_map_cos_xy(std::get<1>(tup));
        const auto out_expr_dy = parse_lin_com_map_cos_xy(std::get<2>(tup));

        BOOST_TEST(in_expr_dx == out_expr_dx);
        BOOST_TEST(in_expr_dy == out_expr_dy);
    }
}

BOOST_AUTO_TEST_CASE(test_diff_cos) {

    // f, df/dx, df/dy
    const std::vector<std::tuple<std::string, std::string, std::string>> input_output = {
        std::make_tuple("cos(x)", "-sin(x)", "0"),
        std::make_tuple("cos(3x+4y)", "-3sin(3x+4y)", "-4sin(3x+4y)"),
        std::make_tuple("-2cos(-4x+y)", "-8sin(-4x+y)", "2sin(-4x+y)"),
        std::make_tuple("cos(x+8y)-cos(4x+2y)", "-sin(x+8y)+4sin(4x+2y)", "-8sin(x+8y)+2sin(4x+2y)"),
    };

    for (const auto& tup : input_output) {
        const auto in_expr = parse_lin_com_map_cos_xy(std::get<0>(tup));
        const auto in_expr_dx = diff<XY::X>(in_expr);
        const auto in_expr_dy = diff<XY::Y>(in_expr);

        const auto out_expr_dx = parse_lin_com_map_sin_xy(std::get<1>(tup));
        const auto out_expr_dy = parse_lin_com_map_sin_xy(std::get<2>(tup));

        BOOST_TEST(in_expr_dx == out_expr_dx);
        BOOST_TEST(in_expr_dy == out_expr_dy);
    }
}
