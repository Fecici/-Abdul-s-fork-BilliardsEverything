#pragma once

#include "../general.hpp"

namespace database {

// TODO maybe do things using istreams instead of strings
// That might make things a little more efficient

template <typename T>
T deserialize(const std::string& str);

template <>
InitialAngles deserialize(const std::string& str);

template <>
PointQ deserialize(const std::string& point_str);

template <>
std::vector<PointQ> deserialize(const std::string& polygon_str);

template <>
Equation<Sin> deserialize(const std::string& coeffs_str);

template <>
Equation<Cos> deserialize(const std::string& coeffs_str);

template <>
std::set<Equation<Sin>> deserialize(const std::string& equations_str);

template <>
std::set<Equation<Cos>> deserialize(const std::string& equations_str);
}
