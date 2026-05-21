#pragma once

#include "database/viewer.hpp"

boost::optional<Stable> calculate_stable(const CodeSequence& code_sequence, const CodeType code_type);

boost::optional<Unstable> calculate_unstable(const CodeSequence& code_sequence, const CodeType code_type);

boost::optional<Stable> calculate_stable(const CodeSequence& code_sequence, const CodeType code_type, const std::vector<LeftRight>& left_rights);

boost::optional<Unstable> calculate_unstable(const CodeSequence& code_sequence, const CodeType code_type, const std::vector<LeftRight>& left_rights);
