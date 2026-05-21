#pragma once

#include <parse.hpp>

BOOST_AUTO_TEST_CASE(test_parse_lin_com_arr_xy) {

    const std::vector<std::string> input = {
        "0",
        "x",
        "y",
        "x-y",
        "2x+3y",
        "2x-y",
        "x+y",
        "-x-y",
        "-x+y",
    };

    for (const auto& str : input) {
        const auto expr = parse_lin_com_arr_xy(str);
        std::ostringstream oss;
        oss << expr;
        const auto expr_str = oss.str();
        BOOST_TEST(expr_str == str);
    }
}

BOOST_AUTO_TEST_CASE(test_parse_lin_com_arr_xypi) {
    const std::vector<std::string> input = {
        "0",
        "x",
        "y",
        "pi",
        "x-y",
        "2x+3y",
        "2x-y",
        "x+y",
        "-x-y",
        "-x+y",
        "x+2pi",
    };

    for (const auto& str : input) {
        const auto expr = parse_lin_com_arr_xypi(str);

        // TODO make a to_string method?
        std::ostringstream oss;
        oss << expr;
        const auto expr_str = oss.str();

        BOOST_TEST(expr_str == str);
    }
}

BOOST_AUTO_TEST_CASE(test_parse_lin_com_arr_xyeta) {

    const std::vector<std::string> input = {
        "0",
        "x",
        "y",
        "eta",
        "x-y",
        "2x+3y",
        "2x-y",
        "x+y",
        "-x-y",
        "-x+y",
        "x+2eta",
        "3x",
        "x+3eta",
        "-y-2eta",
    };

    for (const auto& str : input) {
        const auto expr = parse_lin_com_arr_xyeta(str);
        std::ostringstream oss;
        oss << expr;
        const auto expr_str = oss.str();
        BOOST_TEST(expr_str == str);
    }
}

BOOST_AUTO_TEST_CASE(test_parse_lin_com_map_trig_xy) {

    // Make sure that the order of the expressions is the same as what will
    // be printed
    const std::vector<std::string> input = {
        "0",
        "trig(x)",
        "-trig(x)",
        "-8trig(y)+2trig(3x-y)",
        "-2trig(x-y)",
    };

    for (const auto& string_trig : input) {

        const auto string_sin = boost::replace_all_copy(string_trig, "trig", "sin");
        const auto expr_sin = parse_lin_com_map_sin_xy(string_sin);

        std::ostringstream oss_sin;
        oss_sin << expr_sin;
        const auto expr_sin_str = oss_sin.str();

        BOOST_TEST(expr_sin_str == string_sin);

        const auto string_cos = boost::replace_all_copy(string_trig, "trig", "cos");
        const auto expr_cos = parse_lin_com_map_cos_xy(string_cos);

        std::ostringstream oss_cos;
        oss_cos << expr_cos;
        const auto expr_cos_str = oss_cos.str();

        BOOST_TEST(expr_cos_str == string_cos);
    }
}
