#pragma once

#include <gradient.hpp>
#include <parse.hpp>

// TODO this stinks, find a better way
namespace boost {
namespace test_tools {
namespace tt_detail {
template <typename T, typename S>
struct print_log_value<std::pair<T, S>> {
    void operator()(std::ostream& os, const std::pair<T, S>& p) {
        os << '(' << p.first << ", " << p.second << ')';
    }
};
} // namespace tt_detail
} // namespace test_tools
} // namespace boost

#if 0
BOOST_AUTO_TEST_CASE(test_sin_gradient_pi2_pi2) {

    const std::vector<std::pair<std::string, std::pair<Coeff64, Coeff64>>> sines = {
        // sin(x+y)
        {"-sin(6x+5y)-sin(6x+7y)", {0, -6}},
        // sin(x+y)^2
        {"2sin(x-y)-sin(3x+y)-sin(4x+2y)+sin(x+3y)+2sin(2x)+sin(2y)", {-1, -1}},
        // sin(x+y)^3
        {"3sin(x-2y)+2sin(4x+y)-sin(6x+3y)+3sin(x+4y)-sin(2x+5y)-sin(3x+6y)-sin(3x)+2sin(3y)", {4, 1}},
    };

    for (const auto& p : sines) {
        const auto eq = parse_lin_com_map_sin_xy(p.first);

        BOOST_TEST(gradient_pi2_pi2(eq) == p.second);
    }
}

BOOST_AUTO_TEST_CASE(test_cos_gradient_pi2_pi2) {

    const std::vector<std::pair<std::string, std::pair<Coeff64, Coeff64>>> cosines = {
        // sin(x+y)
        {"cos(x-y)-cos(2x-y)-cos(3x+y)+cos(3y)", {-2, 0}},
        // sin(x+y)^2
        {"2cos(x-2y)-cos(2x-y)-cos(x+4y)-cos(2x+5y)-cos(3x)+2cos(3y)", {1, 1}},
        // sin(x+y)^3
        {"3cos(x-y)-3cos(3x+y)+4cos(4x+2y)-cos(x+3y)+cos(5x+3y)-cos(2x+4y)-cos(6x+4y)-6cos(2x)+4cos(2y)",
         {-4, -2}},
    };

    for (const auto& p : cosines) {
        const auto eq = parse_lin_com_map_cos_xy(p.first);

        BOOST_TEST(gradient_pi2_pi2(eq) == p.second);
    }
}
#endif

// Always do the simplest test case first, and then move up from there
BOOST_AUTO_TEST_CASE(test_gradient_bound) {

    const std::vector<std::pair<std::string, Coeff64>> input = {
        {"trig(0)", 0},
        {"trig(x)", 1},
        {"trig(y)", 1},
        {"-2trig(2x)", 4},
        {"2trig(3x-5y)-8trig(5x+6y)", 104},
    };

    for (const auto& p : input) {

        const auto string_sin = boost::replace_all_copy(p.first, "trig", "sin");
        const auto expr_sin = parse_lin_com_map_sin_xy(string_sin);

        const auto sin_bound = gradient_bounds(expr_sin);

        BOOST_TEST(sin_bound.first + sin_bound.second == p.second);

        const auto string_cos = boost::replace_all_copy(p.first, "trig", "cos");
        const auto expr_cos = parse_lin_com_map_cos_xy(string_cos);

        const auto cos_bound = gradient_bounds(expr_cos);

        BOOST_TEST(cos_bound.first + cos_bound.second == p.second);
    }
}
