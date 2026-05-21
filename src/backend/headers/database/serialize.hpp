#pragma once

#include "../code_sequence.hpp"
#include "../general.hpp"
#include "../evaluator.hpp"

namespace database {

void serialize(std::ostream& os, const CodeType& code_type);

void serialize(std::ostream& os, const CodeSequence& code_sequence);

void serialize(std::ostream& os, const InitialAngles& initial_angles);

void serialize(std::ostream& os, const PointQ& point);

void serialize(std::ostream& os, const std::vector<PointQ>& points);

template <template <typename> class Trig>
void serialize(std::ostream& os, const LinComMapZ<Trig<LinComArrZ<XY>>>& equation);

extern template void serialize(std::ostream& os, const LinComMapZ<Sin<LinComArrZ<XY>>>& equation);
extern template void serialize(std::ostream& os, const LinComMapZ<Cos<LinComArrZ<XY>>>& equation);

template <template <typename> class Trig>
void serialize(std::ostream& os, const std::set<LinComMapZ<Trig<LinComArrZ<XY>>>>& eqs);

extern template void serialize(std::ostream& os, const std::set<LinComMapZ<Sin<LinComArrZ<XY>>>>& eqs);
extern template void serialize(std::ostream& os, const std::set<LinComMapZ<Cos<LinComArrZ<XY>>>>& eqs);

template <typename T>
std::string serialize(const T& t) {
    std::ostringstream oss{};
    serialize(oss, t);
    return oss.str();
}
}

inline std::string table_name(const CodeSequence& code_sequence) {
    return database::serialize(code_sequence.type());
}
