#include "database/serialize.hpp"

void database::serialize(std::ostream& os, const CodeType& code_type) {
    switch (code_type) {
    case CodeType::OSO:
        os << "oso";
        return;
    case CodeType::OSNO:
        os << "osno";
        return;
    case CodeType::ONS:
        os << "ons";
        return;
    case CodeType::CS:
        os << "cs";
        return;
    case CodeType::CNS:
        os << "cns";
        return;
    }

    throw std::runtime_error(invalid_enum_value("CodeType", code_type));
}

void database::serialize(std::ostream& os, const CodeSequence& code_sequence) {
    os << code_sequence;
}

void database::serialize(std::ostream& os, const InitialAngles& initial_angles) {
    os << initial_angles.first << initial_angles.second;
}

void database::serialize(std::ostream& os, const PointQ& point) {
    os << point.x << ' ' << point.y;
}

void database::serialize(std::ostream& os, const std::vector<PointQ>& points) {

    bool first = true;
    for (const auto& point : points) {

        if (!first) {
            os << '\n';
        }

        serialize(os, point);

        first = false;
    }
}

template <template <typename> class Trig>
void database::serialize(std::ostream& os, const LinComMapZ<Trig<LinComArrZ<XY>>>& equation) {

    bool first = true;
    for (const auto& kv : equation) {
        // order is trig_coeff, x_coeff, y_coeff
        const auto trig_coeff = kv.second;
        const auto x_coeff = kv.first.arg.coeff(XY::X);
        const auto y_coeff = kv.first.arg.coeff(XY::Y);

        if (!first) {
            os << ' ';
        }

        os << trig_coeff << ' ' << x_coeff << ' ' << y_coeff;

        first = false;
    }
}

template void database::serialize(std::ostream& os, const LinComMapZ<Sin<LinComArrZ<XY>>>& equation);
template void database::serialize(std::ostream& os, const LinComMapZ<Cos<LinComArrZ<XY>>>& equation);

template <template <typename> class Trig>
void database::serialize(std::ostream& os, const std::set<LinComMapZ<Trig<LinComArrZ<XY>>>>& eqs) {

    bool first = true;
    for (const auto& eq : eqs) {

        if (!first) {
            os << '\n';
        }

        serialize(os, eq);

        first = false;
    }
}

template void database::serialize(std::ostream& os, const std::set<LinComMapZ<Sin<LinComArrZ<XY>>>>& eqs);
template void database::serialize(std::ostream& os, const std::set<LinComMapZ<Cos<LinComArrZ<XY>>>>& eqs);

