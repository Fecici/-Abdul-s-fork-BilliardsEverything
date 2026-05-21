#pragma once

#include <ostream> // std::ostream

// This is essentially a `newtype` pattern.

template <typename T>
class Sin final {
  public:
    T arg;

    explicit Sin(const T& arg_)
        : arg{arg_} {}

    //std::string get_function() {
    //    return "sin";
    //}

    // Strictly speaking, these functions don't need to be friends, but I like keeping
    // them inside the class.
    friend bool operator==(const Sin<T>& lhs, const Sin<T>& rhs) {
        return lhs.arg == rhs.arg;
    }

    friend bool operator<(const Sin<T>& lhs, const Sin<T>& rhs) {
        return lhs.arg < rhs.arg;
    }

    friend std::ostream& operator<<(std::ostream& os, const Sin<T>& sin) {
        return os << "sin(" << sin.arg << ')';
    }
};

template <typename T>
class Cos final {
  public:
    T arg;

    explicit Cos(const T& arg_)
        : arg{arg_} {}

    //std::string get_function() {
    //    return "cos";
    //}

    friend bool operator==(const Cos<T>& lhs, const Cos<T>& rhs) {
        return lhs.arg == rhs.arg;
    }

    friend bool operator<(const Cos<T>& lhs, const Cos<T>& rhs) {
        return lhs.arg < rhs.arg;
    }

    friend std::ostream& operator<<(std::ostream& os, const Cos<T>& cos) {
        return os << "cos(" << cos.arg << ')';
    }
};
