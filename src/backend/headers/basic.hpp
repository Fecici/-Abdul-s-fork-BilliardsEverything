#pragma once

// General stuff that should be in stdlib but isn't
#include <array>
#include <fstream>
#include <map>
#include <ostream>
#include <set>
#include <sstream>
#include <vector>

#include <boost/algorithm/string.hpp>
#include <boost/call_traits.hpp>
#include <boost/optional.hpp>
#include <boost/variant.hpp>

// const T for primitive types, const T& for classes
template <typename T>
using ParamType = typename boost::call_traits<T>::param_type;

// Unfortunately, we need this, since it is possible to cast an
// integer to an enum value outside the class
// Switches are ok, if you are returning from each branch. If
// you are not, use an if/else.
template <typename T>
std::string invalid_enum_value(const std::string& enum_name, const T value) {
    std::ostringstream err{};
    err << "unknown " << enum_name << " value " << static_cast<size_t>(value);
    return err.str();
}

// Return -1 if lhs < rhs
// Return 0 if lhs == rhs
// Return 1 if lhs > rhs
template <typename T>
int compare(const T lhs, const T rhs) {
    return (lhs > rhs) - (lhs < rhs);
}

template <typename T, typename S>
std::ostream& operator<<(std::ostream& os, const std::pair<T, S>& p) {
    return os << '(' << p.first << ", " << p.second << ')';
}

// Why does this not already exist?
template <typename Cont>
std::ostream& print_container(std::ostream& os, const Cont& cont) {

    os << '[';

    bool first = true;
    for (const auto& elem : cont) {
        if (!first) {
            os << ", ";
        }

        first = false;

        os << elem;
    }

    os << ']';
    return os;
}

template <typename T, size_t N>
std::ostream& operator<<(std::ostream& os, const std::array<T, N>& arr) {
    return print_container(os, arr);
}

template <typename T>
std::ostream& operator<<(std::ostream& os, const std::vector<T>& vec) {
    return print_container(os, vec);
}

template <typename T>
std::ostream& operator<<(std::ostream& os, const std::set<T>& set) {
    return print_container(os, set);
}

template <typename T, typename S>
std::ostream& operator<<(std::ostream& os, const std::map<T, S>& map) {
    return print_container(os, map);
}

std::vector<std::string> split(const std::string& str, const std::string& delims);

std::ifstream open_file_read(const std::string& path);

std::ofstream open_file_write(const std::string& path);

std::string read_file(const std::string& path);
