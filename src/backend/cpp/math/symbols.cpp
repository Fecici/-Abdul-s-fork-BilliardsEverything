// TODO it would be nice to pull out the invalid_enum_value so we don't have
// to include everything else
#include "math/symbols.hpp"
#include "basic.hpp"

std::ostream& operator<<(std::ostream& os, const XY sym) {
    switch (sym) {
    case XY::X:
        return os << 'x';
    case XY::Y:
        return os << 'y';
    }

    throw std::runtime_error(invalid_enum_value("XY", sym));
}

std::ostream& operator<<(std::ostream& os, const XYZ sym) {
    switch (sym) {
    case XYZ::X:
        return os << 'x';
    case XYZ::Y:
        return os << 'y';
    case XYZ::Z:
        return os << 'z';
    }

    throw std::runtime_error(invalid_enum_value("XYZ", sym));
}

std::ostream& operator<<(std::ostream& os, const XYPi sym) {
    switch (sym) {
    case XYPi::X:
        return os << 'x';
    case XYPi::Y:
        return os << 'y';
    case XYPi::Pi:
        return os << "pi";
    }

    throw std::runtime_error(invalid_enum_value("XYPi", sym));
}

std::ostream& operator<<(std::ostream& os, const XEta sym) {
    switch (sym) {
    case XEta::X:
        return os << 'x';
    case XEta::Eta:
        return os << "eta";
    }

    throw std::runtime_error(invalid_enum_value("XEta", sym));
}

std::ostream& operator<<(std::ostream& os, const YEta sym) {
    switch (sym) {
    case YEta::Y:
        return os << 'y';
    case YEta::Eta:
        return os << "eta";
    }

    throw std::runtime_error(invalid_enum_value("YEta", sym));
}

std::ostream& operator<<(std::ostream& os, const XYEta sym) {
    switch (sym) {
    case XYEta::X:
        return os << 'x';
    case XYEta::Y:
        return os << 'y';
    case XYEta::Eta:
        return os << "eta";
    }

    throw std::runtime_error(invalid_enum_value("XYEta", sym));
}

std::ostream& operator<<(std::ostream& os, const XYEtaPhi sym) {
    switch (sym) {
    case XYEtaPhi::X:
        return os << 'x';
    case XYEtaPhi::Y:
        return os << 'y';
    case XYEtaPhi::Eta:
        return os << "eta";
    case XYEtaPhi::Phi:
        return os << "phi";
    }

    throw std::runtime_error(invalid_enum_value("XYEtaPhi", sym));
}

std::ostream& operator<<(std::ostream& os, const ST sym) {
    switch (sym) {
    case ST::S:
        return os << 's';
    case ST::T:
        return os << 't';
    }

    throw std::runtime_error(invalid_enum_value("ST", sym));
}

// No pattern matching. Oh well.
XYZ other_angle(const XYZ angle1, const XYZ angle2) {

    const auto x = XYZ::X;
    const auto y = XYZ::Y;
    const auto z = XYZ::Z;

    if ((angle1 == x && angle2 == y) || (angle1 == y && angle2 == x)) {
        return z;
    }

    if ((angle1 == x && angle2 == z) || (angle1 == z && angle2 == x)) {
        return y;
    }

    if ((angle1 == y && angle2 == z) || (angle1 == z && angle2 == y)) {
        return x;
    }

    std::ostringstream err{};
    err << "invalid angles in other_angle: "
        << "angle1 = " << static_cast<size_t>(angle1) << ", "
        << "angle2 = " << static_cast<size_t>(angle2);

    throw std::runtime_error(err.str());
}
