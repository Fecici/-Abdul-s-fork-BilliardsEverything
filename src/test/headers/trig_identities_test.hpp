#pragma once

#include <trig_identities.hpp>

BOOST_AUTO_TEST_CASE(test_simplify_sin_xypi) {

    const std::vector<std::pair<std::string, std::pair<Coeff64, std::string>>> in_out = {
        // no transformations
        {"x", {1, "x"}},
        // even-odd identities
        {"-x", {-1, "x"}},
        {"-x+y", {-1, "x-y"}},
        // periodicity identitios
        {"x+pi", {-1, "x"}},
        {"x+4pi", {1, "x"}},
        // all together
        {"-8x+3y+5pi", {1, "8x-3y"}},
        {"-4x-2y+2pi", {-1, "4x+2y"}},
    };

    for (const auto& p : in_out) {

        const auto in_expr = parse_lin_com_arr_xypi(p.first);
        const auto simplified_expr_pair = simplify_sin_xypi(in_expr);

        const auto out_expr = parse_lin_com_arr_xy(p.second.second);
        const auto expected_out = std::make_pair(p.second.first, Sin<LinComArrZ<XY>>{out_expr});

        BOOST_TEST(simplified_expr_pair == expected_out);
    }
}

BOOST_AUTO_TEST_CASE(test_simplify_cos_xypi) {

    const std::vector<std::pair<std::string, std::pair<Coeff64, std::string>>> in_out = {
        // no transformxtions
        {"x", {1, "x"}},
        // even-odd identities
        {"-x", {1, "x"}},
        {"-x+y", {1, "x-y"}},
        // periodicity identitios
        {"x+pi", {-1, "x"}},
        {"x+4pi", {1, "x"}},
        // all together
        {"-8x+3y+5pi", {-1, "8x-3y"}},
        {"-4x-2y+2pi", {1, "4x+2y"}},
    };

    for (const auto& p : in_out) {

        const auto in_expr = parse_lin_com_arr_xypi(p.first);
        const auto simplified_expr_pair = simplify_cos_xypi(in_expr);

        const auto out_expr = parse_lin_com_arr_xy(p.second.second);
        const auto expected_out = std::make_pair(p.second.first, Cos<LinComArrZ<XY>>{out_expr});

        BOOST_TEST(simplified_expr_pair == expected_out);
    }
}

BOOST_AUTO_TEST_CASE(test_simplify_sin_xyeta) {

    const std::vector<std::pair<std::string, std::pair<Coeff64, std::string>>> in_out = {
        {"x+eta", {1, "x"}},
        {"x-eta", {-1, "x"}},
        {"x+15eta", {-1, "x"}},
        {"x-15eta", {1, "x"}},
        {"-4x-2y+3eta", {-1, "4x+2y"}},
    };

    for (const auto& p : in_out) {

        const auto in_expr = parse_lin_com_arr_xyeta(p.first);
        const auto simplified_expr_pair = simplify_sin_xyeta(in_expr);

        const auto out_expr = parse_lin_com_arr_xy(p.second.second);
        const auto expected_out = std::make_pair(p.second.first, Cos<LinComArrZ<XY>>{out_expr});

        BOOST_TEST(simplified_expr_pair == expected_out);
    }
}

BOOST_AUTO_TEST_CASE(test_simplify_cos_xyeta) {

    const std::vector<std::pair<std::string, std::pair<Coeff64, std::string>>> in_out = {
        {"x+eta", {-1, "x"}},
        {"x-eta", {1, "x"}},
        {"x+15eta", {1, "x"}},
        {"x-15eta", {-1, "x"}},
        {"-4x-2y+3eta", {-1, "4x+2y"}},
    };

    for (const auto& p : in_out) {

        const auto in_expr = parse_lin_com_arr_xyeta(p.first);
        const auto simplified_expr_pair = simplify_cos_xyeta(in_expr);

        const auto out_expr = parse_lin_com_arr_xy(p.second.second);
        const auto expected_out = std::make_pair(p.second.first, Sin<LinComArrZ<XY>>{out_expr});

        BOOST_TEST(simplified_expr_pair == expected_out);
    }
}

#if 0
BOOST_AUTO_TEST_CASE(test_simplify_sin_pi2) {

    // Checked using sympy
    const std::vector<std::pair<Coeff64, Coeff64>> in_out = {
        {-4, 0},
        {-3, 1},
        {-2, 0},
        {-1, -1},
        {0, 0},
        {1, 1},
        {2, 0},
        {3, -1},
    };

    for (const auto& p : in_out) {
        BOOST_TEST(simplify_sin_pi2(p.first) == p.second);
    }
}

BOOST_AUTO_TEST_CASE(test_simplify_cos_pi2) {

    // Checked using sympy
    const std::vector<std::pair<Coeff64, Coeff64>> in_out = {
        {-4, 1},
        {-3, 0},
        {-2, -1},
        {-1, 0},
        {0, 1},
        {1, 0},
        {2, -1},
        {3, 0},
        {4, 1},
    };

    for (const auto& p : in_out) {
        BOOST_TEST(simplify_cos_pi2(p.first) == p.second);
    }
}
#endif

BOOST_AUTO_TEST_CASE(test_simplify_lin_com_sin) {

    // Checked using sympy
    // zero_zero, zero_pi2, pi2_zero, pi2_pi2
    const std::vector<std::pair<std::string, std::array<Coeff64, 4>>> sines = {
        {"sin(y)", {{0, 1, 0, 1}}},
        {"sin(2x+y)", {{0, 1, 0, -1}}},
        {"sin(y)+2sin(2x+y)", {{0, 3, 0, -1}}},
        {"sin(x-y)+sin(x+y)+sin(3x+y)", {{0, 1, 1, 0}}},
        {"sin(x)-sin(5x)-sin(x+2y)-sin(3x+2y)", {{0, 0, 0, 0}}},
        {"sin(2x)-sin(6x)-sin(4x-2y)-sin(4x+2y)", {{0, 0, 0, 0}}},
        {"sin(x)-sin(x+2y)+sin(3x+2y)+sin(5x+2y)-sin(5x+4y)", {{0, 0, -1, 1}}},
        {"-sin(2x+4y)-2sin(4x+4y)+sin(4x+6y)-sin(6x+4y)+sin(6x+6y)", {{0, 0, 0, 0}}},
    };

    for (const auto& p : sines) {

        const auto eq = parse_lin_com_map_sin_xy(p.first);
        const auto& result = p.second;

        BOOST_TEST(simplify_lin_com_zero_zero(eq) == std::get<0>(result));

        BOOST_TEST(simplify_lin_com_zero_pi2(eq) == std::get<1>(result));

        BOOST_TEST(simplify_lin_com_pi2_zero(eq) == std::get<2>(result));

        BOOST_TEST(simplify_lin_com_pi2_pi2(eq) == std::get<3>(result));
    }
}

BOOST_AUTO_TEST_CASE(test_simplify_lin_com_cos) {

    // Checked using sympy
    // zero_zero, zero_pi2, pi2_zero, pi2_pi2
    const std::vector<std::pair<std::string, std::array<Coeff64, 4>>> cosines = {
        {"-cos(x+y)", {{-1, 0, 0, 1}}},
        {"-cos(2x+y)", {{-1, 0, 1, 0}}},
        {"-cos(2x)+cos(2y)+cos(2x-2y)", {{1, -3, 1, 1}}},
        {"-cos(x)-cos(3x)-cos(x-2y)-cos(x+2y)", {{-4, 0, 0, 0}}},
        {"-2cos(2x)+cos(2y)+cos(2x-2y)+cos(2x+2y)", {{1, -5, 1, 3}}},
        {"-cos(x-3y)-cos(x-y)-cos(x+y)-cos(3x-y)-cos(3x+y)", {{-5, 0, 0, 1}}},
        {"cos(2x)-cos(2y)+cos(2x+2y)+cos(2x+4y)-cos(4x+6y)+cos(6x+4y)+cos(0)", {{3, 5, -5, 1}}},
        {"cos(x)+cos(3x)-cos(5x)-2cos(3x+2y)+cos(3x+4y)+2cos(5x+2y)-cos(5x+4y)", {{1, 1, 0, 0}}},
    };

    for (const auto& p : cosines) {

        const auto eq = parse_lin_com_map_cos_xy(p.first);
        const auto& result = p.second;

        BOOST_TEST(simplify_lin_com_zero_zero(eq) == std::get<0>(result));

        BOOST_TEST(simplify_lin_com_zero_pi2(eq) == std::get<1>(result));

        BOOST_TEST(simplify_lin_com_pi2_zero(eq) == std::get<2>(result));

        BOOST_TEST(simplify_lin_com_pi2_pi2(eq) == std::get<3>(result));
    }
}
