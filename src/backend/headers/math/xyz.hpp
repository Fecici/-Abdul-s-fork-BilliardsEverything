#pragma once
#include <stdexcept>
#include <string>
#include "math/symbols.hpp"

inline XYZ otherAngle(XYZ angle1, XYZ angle2) {
    if ((angle1 == XYZ::X && angle2 == XYZ::Y) || (angle1 == XYZ::Y && angle2 == XYZ::X)) {
        return XYZ::Z;
    }
    if ((angle1 == XYZ::X && angle2 == XYZ::Z) || (angle1 == XYZ::Z && angle2 == XYZ::X)) {
        return XYZ::Y;
    }
    if ((angle1 == XYZ::Y && angle2 == XYZ::Z) || (angle1 == XYZ::Z && angle2 == XYZ::Y)) {
        return XYZ::X;
    }
    throw std::runtime_error("invalid angles " + std::to_string(static_cast<int>(angle1)) + " " + std::to_string(static_cast<int>(angle2)));
}