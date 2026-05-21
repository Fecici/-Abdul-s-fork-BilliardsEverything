#pragma once

#if 0
#include <shooting_angles.hpp>

BOOST_AUTO_TEST_CASE(test_calculate_closed_index) {

    const std::vector<std::pair<std::vector<CodeNumber>, size_t>> in_out = {
        // 4 CNS
        {{2, 2}, 0},
        // 6 CNS
        {{2, 4}, 0},
        {{1, 2, 1, 2}, 1},
        // 8 CNS
        {{2, 6}, 0},
        {{4, 4}, 0},
        {{1, 2, 1, 4}, 1},
        {{1, 1, 2, 1, 1, 2}, 2},
        // 10 CNS
        {{2, 8}, 0},
        {{4, 6}, 0},
        {{1, 2, 1, 6}, 1},
        {{2, 2, 2, 4}, 1},
        {{2, 3, 2, 3}, 0},
        {{1, 1, 2, 1, 1, 4}, 2},
        {{1, 4, 1, 4}, 1},
        {{1, 2, 1, 2, 2, 2}, 1},
        // 12 CS
        {{1, 1, 1, 1, 2, 1, 1, 1, 1, 2}, 4},
        // 12 CNS
        {{2, 10}, 0},
        {{4, 8}, 0},
        {{1, 2, 1, 8}, 1},
        {{2, 2, 2, 6}, 1},
        {{6, 6}, 0},
        {{2, 3, 4, 3}, 0},
        {{1, 1, 2, 1, 1, 6}, 2},
        {{1, 4, 1, 6}, 1},
        {{1, 2, 1, 3, 2, 3}, 1},
        {{1, 1, 4, 1, 1, 4}, 2},
        {{1, 1, 2, 1, 1, 2, 2, 2}, 2},
        {{1, 2, 1, 2, 1, 2, 1, 2}, 1}};

    for (const auto& p : in_out) {
        BOOST_TEST(calculate_closed_index(p.first) == p.second);
    }

    const std::vector<std::vector<CodeNumber>> odd_seq = {
        // 3 OSO
        {1, 1, 1},
        // 5 OSO
        {1, 1, 3},
        // 7 OSO
        {1, 1, 5},
        {1, 3, 3},
        // Now just random stuff
        {1},
        {1, 2, 3},
        {1, 2, 3, 4, 5},
    };

    for (const auto& code_numbers : odd_seq) {
        // BOOST_CHECK_EXCEPTION lets you check the exception message
        BOOST_CHECK_THROW(calculate_closed_index(code_numbers), std::runtime_error);
    }

    // sequences that are even but not closed
    const std::vector<std::vector<CodeNumber>> not_closed_seq = {
        // 10 ONS
        {1, 1, 2, 1, 3, 2},
        {1, 1, 1, 1, 1, 1, 2, 2},
        {1, 1, 1, 1, 3, 3},
        // 12 ONS
        {2, 2, 4, 4},
        {1, 1, 2, 3, 3, 2},
        {1, 1, 2, 3, 1, 4},
        {1, 1, 1, 1, 3, 5},
        // 14 OSNO
        {1, 1, 2, 2, 1, 1, 3, 3},
        // Random stuff
        {2, 1, 2, 3, 3, 3, 2, 1},
        {3, 1, 2, 3, 2, 3, 2, 1},
        {5, 1, 2, 3, 7, 3, 2, 1}};

    for (const auto& code_numbers : not_closed_seq) {
        BOOST_CHECK_THROW(calculate_closed_index(code_numbers), std::runtime_error);
    }
}
#endif
