#include "equations.hpp"

int main() {
    const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 2, 2, 1, 1, 3, 3}, CodeType::OSNO};
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 2, 1, 1, 12, 1, 2, 1, 9, 1, 1, 19, 2, 11, 1, 2, 1, 12, 1, 2, 1, 9, 1, 1, 19, 2, 13}, CodeType::OSNO};
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 15, 2, 23, 1, 2, 1, 29, 2, 10, 2, 26, 2, 14, 2, 23, 1, 2, 1, 29, 3, 1, 17, 2, 22, 2, 14, 2, 25, 1, 2, 1, 27, 2, 12, 2, 25, 1, 2, 1, 27, 3}, CodeType::OSNO};
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 2, 1, 119, 2, 58, 2, 122, 2, 56, 2, 123, 1, 2, 1, 152, 1, 2, 1, 124, 1, 2, 1, 151, 3, 1, 111, 2, 66, 2, 114, 2, 62, 2, 119, 1, 2, 1, 156, 1, 2, 1, 119, 2, 60, 2, 118, 2, 62, 2, 115, 1, 3, 156, 3, 1, 116, 1, 3, 157}, CodeType::OSNO};

    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 1}, CodeType::OSO};
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 1, 1, 2, 3, 2}, CodeType::OSO};
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 2, 1, 2, 1, 2, 2, 3, 4, 2}, CodeType::OSO};
    //const std::pair<std::vector<CodeNumber>, CodeType> input = {{1, 1, 1, 2, 2}, CodeType::OSO};

    const auto volatile x = calculate_stable(input.first, input.second);
}
