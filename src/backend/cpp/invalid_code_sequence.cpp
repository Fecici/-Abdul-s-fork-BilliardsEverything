#include "invalid_code_sequence.hpp"



/*
Jul 31 2025 Marco Mai
transfer from Java
*/
inline std::string makeString(const std::vector<int>& codeNumbers, const std::string& sep ) {
    std::ostringstream oss;
    for (size_t i = 0; i < codeNumbers.size(); ++i) {
        if (i > 0) oss << sep;
        oss << codeNumbers[i];
    }
    return oss.str();
}

inline std::string errorMessage(const std::vector<int>& codeNumbers, InvalidCodeSequence errorCode) {
    std::string str = makeString(codeNumbers);

    switch (errorCode) {
        case InvalidCodeSequence::EMPTY:
            return "Code sequence is empty: " + str;
        case InvalidCodeSequence::NEGATIVE_OR_ZERO_NUMBERS:
            return "All code numbers must be greater than 0: " + str;
        case InvalidCodeSequence::ILLEGAL_PATTERN:
            return "Code sequence is illegal: " + str;
        default:
            throw std::runtime_error("Unknown InvalidCodeSequence");
    }
}
