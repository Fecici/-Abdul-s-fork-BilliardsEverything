#define NONIUS_RUNNER
#include "equations.hpp"
#include <nonius.h++>

#pragma clang diagnostic ignored "-Wglobal-constructors"

/*
NONIUS_BENCHMARK("test", [](nonius::chronometer meter) {
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 2, 1, 1, 12, 1, 2, 1, 9, 1, 1, 19, 2, 11, 1, 2, 1, 12, 1, 2, 1, 9, 1, 1, 19, 2, 13}, CodeType::OSNO};
    const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 14, 1, 1, 69, 2, 20, 2, 69, 1, 1, 14, 1, 1, 71, 2, 16, 1, 1, 71, 2, 16, 1, 1, 71, 3, 1, 39, 2, 49, 1, 2, 1, 65, 3, 1, 34, 1, 3, 65, 1, 2, 1, 49, 2, 39, 1, 3, 71}, CodeType::OSNO};
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 1, 1, 2, 2, 2, 1, 1, 3, 3, 2}, CodeType::OSNO};
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 20, 1, 1, 151, 3, 1, 107, 2, 68, 2, 109, 1, 3, 153}, CodeType::OSNO};
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 3, 3, 2, 3, 3, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 2, 2, 2, 2, 1, 1, 2}, CodeType::CS};
    //const std::vector<CodeNumber> code_numbers = {1, 1, 1, 1, 2, 3, 2}; // OSO
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 2, 2, 1, 1, 3, 3}, CodeType::OSNO};
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 1, 1, 4, 1, 3, 3, 1, 4}, CodeType::OSNO};
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 1, 1, 2, 1, 2, 1, 2, 1, 2, 1, 3, 2, 6, 2, 6, 2, 3, 1, 2, 1, 2, 1, 2, 1, 2, 1, 1, 1, 1, 2, 1, 2, 1, 2, 1, 3, 2, 7, 2, 7, 2, 3, 1, 2, 1, 2, 1, 2}, CodeType::CS};
    meter.measure([&] {
        return calculate_curves(input.first, input.second);
    });
});
*/

NONIUS_BENCHMARK("test", [](nonius::chronometer meter) {
    //const std::vector<CodeNumber> code_numbers = {1, 1, 14, 1, 1, 69, 2, 20, 2, 69, 1, 1, 14, 1, 1, 71, 2, 16, 1, 1, 71, 2, 16, 1, 1, 71, 3, 1, 39, 2, 49, 1, 2, 1, 65, 3, 1, 34, 1, 3, 65, 1, 2, 1, 49, 2, 39, 1, 3, 71};
    const std::vector<CodeNumber> code_numbers = {1, 2, 1, 23, 2, 14, 2, 26, 2, 10, 2, 29, 1, 2, 1, 23, 2, 15, 1, 3, 27, 1, 2, 1, 25, 2, 12, 2, 27, 1, 2, 1, 25, 2, 14, 2, 22, 2, 17, 1, 3, 29};
    const auto code_type = CodeType::OSNO;

    const auto code_angles = generate_code_angles(code_numbers);

    const auto code_angles_eta = to_abeta(code_angles);
    const auto code_angles_pi = to_abpi(code_angles);
    const auto code_angles_eta_phi = to_abetaphi(code_angles);

    const auto constraint = calculate_constraint(code_numbers, code_angles_eta);

    // double check that it is stable
    if (!constraint.is_zero()) {
        throw std::runtime_error("unstable code sequence passed to stable case");
    }

    if (code_type != CodeType::OSNO) {
        throw std::runtime_error("not osno");
    }

    const Unfolding unfold{code_numbers, code_angles};
    const auto shooting_vector = unfold.shooting_vector_general();

    meter.measure([&] {
        return unfold.generate_curves(shooting_vector.first, shooting_vector.second);
    });
});
