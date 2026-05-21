#include "conversion.hpp"

LinComArrZ<XYEta> xyz_to_xyeta(const XYZ symbol) {
    switch (symbol) {
    case XYZ::X:
        return LinComArrZ<XYEta>{1, 0, 0};
    case XYZ::Y:
        return LinComArrZ<XYEta>{0, 1, 0};
    case XYZ::Z:
        // z = -x - y + 2*eta
        return LinComArrZ<XYEta>{-1, -1, 2};
    }

    throw std::runtime_error(invalid_enum_value("XYZ", symbol));
}

LinComArrZ<XYPi> xyz_to_xypi(const XYZ symbol) {
    switch (symbol) {
    case XYZ::X:
        return LinComArrZ<XYPi>{1, 0, 0};
    case XYZ::Y:
        return LinComArrZ<XYPi>{0, 1, 0};
    case XYZ::Z:
        // z = -x - y + pi
        return LinComArrZ<XYPi>{-1, -1, 1};
    }

    throw std::runtime_error(invalid_enum_value("XYZ", symbol));
}

LinComArrZ<XYEtaPhi> xyz_to_xyetaphi(const XYZ symbol) {
    switch (symbol) {
    case XYZ::X:
        return LinComArrZ<XYEtaPhi>{1, 0, 0, 0};
    case XYZ::Y:
        return LinComArrZ<XYEtaPhi>{0, 1, 0, 0};
    case XYZ::Z:
        // z = -x - y + 2*eta
        return LinComArrZ<XYEtaPhi>{-1, -1, 2, 0};
    }

    throw std::runtime_error(invalid_enum_value("XYZ", symbol));
}
