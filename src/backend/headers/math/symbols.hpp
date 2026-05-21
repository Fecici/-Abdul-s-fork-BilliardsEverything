#pragma once

#include <ostream>

// Struct to store the number of values in an enum.
template <typename EnumClass>
struct Enum final {};

// The values of these enum classes are going to be used as indices in a std::array,
// so they must all have underlying type size_t.

enum class XY : size_t {
    X,
    Y,
};

template <>
struct Enum<XY> final {
    static constexpr size_t size = 2;
};

std::ostream& operator<<(std::ostream& os, const XY sym);

enum class XYZ : size_t {
    X,
    Y,
    Z,
};

template <>
struct Enum<XYZ> final {
    static constexpr size_t size = 3;
};

std::ostream& operator<<(std::ostream& os, const XYZ sym);

enum class XYPi : size_t {
    X,
    Y,
    Pi,
};

template <>
struct Enum<XYPi> final {
    static constexpr size_t size = 3;
};

std::ostream& operator<<(std::ostream& os, const XYPi sym);

enum class XEta : size_t {
    X,
    Eta, // eta = pi/2
};

template <>
struct Enum<XEta> final {
    static constexpr size_t size = 2;
};

std::ostream& operator<<(std::ostream& os, const XEta sym);

enum class YEta : size_t {
    Y,
    Eta, // eta = pi/2
};

template <>
struct Enum<YEta> final {
    static constexpr size_t size = 2;
};

std::ostream& operator<<(std::ostream& os, const YEta sym);

enum class XYEta : size_t {
    X,
    Y,
    Eta, // eta = pi/2
};

template <>
struct Enum<XYEta> final {
    static constexpr size_t size = 3;
};

std::ostream& operator<<(std::ostream& os, const XYEta sym);

enum class XYEtaPhi : size_t {
    X,
    Y,
    Eta,
    Phi,
};

template <>
struct Enum<XYEtaPhi> final {
    static constexpr size_t size = 4;
};

std::ostream& operator<<(std::ostream& os, const XYEtaPhi sym);

enum class ST : size_t {
    S,
    T,
};

template <>
struct Enum<ST> final {
    static constexpr size_t size = 2;
};

std::ostream& operator<<(std::ostream& os, const ST sym);

XYZ other_angle(const XYZ angle1, const XYZ angle2);
