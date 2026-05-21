#pragma once
#include <vector>
#include <string>
#include <stdexcept>
#include <sstream>


enum class InvalidCodeSequence {
    EMPTY,
    NEGATIVE_OR_ZERO_NUMBERS,
    ILLEGAL_PATTERN,
    NONE
};

/*
Jul 31 2025 Marco Mai
transfer from Java
*/
inline std::string makeString(const std::vector<int>& codeNumbers, const std::string& sep = " ");

inline std::string errorMessage(const std::vector<int>& codeNumbers, InvalidCodeSequence errorCode);
