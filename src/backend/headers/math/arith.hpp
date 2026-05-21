#pragma once

namespace math {
// The standard library already provides a std::abs function. However, that function does not perform
// error checking and will overflow if the integer type is not large enough to contain the return value.
// For example, std::abs(INT_MIN) will overflow and trigger undefined behaviour. To avoid this, we provide
// our own abs() function, which, unlike the standard library, will be compiled to trap integer overflow.
template <typename T>
T abs(const T val) {
    if (val < 0) {
        return -val;
    } else {
        return val;
    }
}

// C++17 will introduce a std::gcd function. However, we again cannot use it, since it also has
// undefined behaviour in several cases (eg, std::gcd(INT_MIN, INT_MIN)). As above, we use our
// own version.
template <typename T>
T gcd(T a, T b) {
    a = abs(a);
    b = abs(b);

    while (b != 0) {
        const auto t = b;
        b = a % b;
        a = t;
    }

    return a;
}
}
