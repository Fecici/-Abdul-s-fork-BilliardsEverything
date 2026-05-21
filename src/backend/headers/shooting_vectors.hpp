#pragma once

#include "code_sequence.hpp"
#include "general.hpp"

std::pair<Equation<Cos>, Equation<Sin>> shooting_vector_open(const CodeSequence& code_sequence, const std::vector<LinComArrZ<XYPi>>& code_angles);

std::pair<Equation<Sin>, Equation<Cos>> shooting_vector_closed(const CodeSequence& code_sequence, const std::vector<LinComArrZ<XYEta>>& code_angles);
