#pragma once

#include <bounding_region.hpp>
#include <conversion.hpp>
#include <equations.hpp>
#include <parse.hpp>

#if 0
BOOST_AUTO_TEST_CASE(test_line_sign_at_point) {

    const std::vector<std::tuple<std::string, PointQ, Sign>> in_out = {
        // f(x, y) = string, evaluate it at Vector2<Rational>, and get sign of value
        std::make_tuple("x", PointQ{0, 0}, Sign::ZERO),
        std::make_tuple("x", PointQ{-1, 2}, Sign::NEG),
        std::make_tuple("x", PointQ{3, 5}, Sign::POS),
        std::make_tuple("y", PointQ{0, 0}, Sign::ZERO),
        std::make_tuple("y", PointQ{2, -1}, Sign::NEG),
        std::make_tuple("y", PointQ{5, 3}, Sign::POS),
        std::make_tuple("x-y", PointQ{1, 1}, Sign::ZERO),
        std::make_tuple("x-y", PointQ{-1, 2}, Sign::NEG),
        std::make_tuple("x-y", PointQ{2, -1}, Sign::POS),
        std::make_tuple("2x-3y+eta", PointQ{{5, 2}, 2}, Sign::ZERO),
        std::make_tuple("2x-3y+eta", PointQ{1, 2}, Sign::NEG),
        std::make_tuple("2x-3y+eta", PointQ{2, 0}, Sign::POS),
    };

    for (const auto& tup : in_out) {

        const auto& eq_str = std::get<0>(tup);
        const auto eq = parse_lin_com_arr_xyeta(eq_str);

        const auto& point = std::get<1>(tup);
        const auto sign = line_sign_at_point(eq, point);

        const auto expected_sign = std::get<2>(tup);

        BOOST_TEST(sign == expected_sign);
    }
}
#endif

#if 0
BOOST_AUTO_TEST_CASE(test_intersection) {

    const std::vector<std::tuple<std::string, std::string, PointQ>> in_out = {
        std::make_tuple("x", "y", PointQ{{0}, {0}}),
        std::make_tuple("3x-4y+5eta", "2x+5y-4eta", PointQ{{-9, 23}, {22, 23}}),
    };

    for (const auto& tup : in_out) {
        const auto eq0 = parse_lin_com_arr_xyeta(std::get<0>(tup));
        const auto eq1 = parse_lin_com_arr_xyeta(std::get<1>(tup));

        const auto inter = intersection(eq0, eq1);

        const auto& actual_inter = std::get<2>(tup);

        BOOST_TEST(inter == actual_inter);
    }
}
#endif

BOOST_AUTO_TEST_CASE(test_calculate_bounding_polygon) {

    // NOTE: I haven't checked these by hand, they are based on the Java program, which I
    // think is correct, but haven't checked
    // The ordering of the points is now slightly different than the Java program, but I
    // believe that is because the LinComArrZ<XYEta>s are now ordered differently (previously
    // on the strings, now just the natural ordering)
    const std::vector<std::pair<std::vector<CodeNumber>, std::vector<PointQ>>> non_empty = {
        {{1, 1, 1}, {{1, 0}, {1, 1}, {0, 1}}},
        {{1, 1, 3}, {{1, {1, 2}}, {1, 1}, {{1, 2}, 1}}},
        {{1, 1, 5}, {{1, {2, 3}}, {1, 1}, {{2, 3}, 1}}},
        {{1, 3, 3}, {{{5, 4}, {1, 4}}, {{4, 3}, {1, 3}}, {{5, 4}, {1, 2}}, {{1}, {1, 2}}}},
        {{1, 1, 7}, {{{1}, {3, 4}}, {{1}, {1}}, {{3, 4}, {1}}}},
        {{1, 3, 5}, {{{4, 3}, {1, 3}}, {{7, 5}, {2, 5}}, {{4, 3}, {1, 2}}, {{7, 6}, {1, 2}}}},
        {{1, 1, 2, 2, 3}, {{{7, 6}, {1, 2}}, {{1}, {2, 3}}, {{3, 4}, {3, 4}}, {{5, 7}, {5, 7}}, {{1}, {1, 2}}}},
        {{1, 1, 1, 1, 2, 1, 2}, {{{1}, {0}}, {{1}, {1}}, {{4, 5}, {3, 5}}}},
    };

    for (const auto& p : non_empty) {

        const auto code_sequence = CodeSequence{p.first};

        const auto code_numbers = code_sequence.numbers();
        const auto code_angles = code_sequence.angles(XYZ::X, XYZ::Y);

        const auto polygon = calculate_bounding_polygon(code_numbers, code_angles);

        const auto get_points = [](const auto& rat_pair) {
            return rat_pair.point;
        };

        const auto points = falgo::transform(*polygon, get_points);

        BOOST_TEST(points == p.second);
    }
}

BOOST_AUTO_TEST_CASE(test_calculate_bounding_line_segment) {

    // Same here
    const std::vector<std::pair<std::vector<CodeNumber>, std::vector<PointQ>>> non_empty = {
        {{2, 2}, {{{0}, {0}}, {{1}, {1}}}},
        {{2, 4}, {{{0}, {0}}, {{1}, {1, 2}}}},
        {{1, 2, 1, 2}, {{{1}, {0}}, {{1}, {1}}}},
        {{2, 6}, {{{0}, {0}}, {{1}, {1, 3}}}},
        {{4, 4}, {{{0}, {0}}, {{1, 2}, {1, 2}}}},
        {{1, 2, 1, 4}, {{{5, 4}, {1, 4}}, {{1}, {1}}}},
        {{1, 1, 2, 1, 1, 2}, {{{2, 3}, {2, 3}}, {{1}, {1}}}},
    };

    for (const auto& p : non_empty) {

        const auto code_sequence = CodeSequence{p.first};

        const auto code_numbers = code_sequence.numbers();
        const auto code_angles = code_sequence.angles(XYZ::X, XYZ::Y);

        const auto constraint = code_sequence.constraint(XYZ::X, XYZ::Y);

        const auto line_segment = calculate_bounding_line_segment(code_numbers, code_angles, constraint);

        // Also, store the points and equations in an array so we can map over it
        const std::vector<PointQ> points{line_segment->point0, line_segment->point1};

        BOOST_TEST(points == p.second);
    }
}
