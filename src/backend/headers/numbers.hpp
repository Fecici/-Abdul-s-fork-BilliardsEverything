#pragma once

#include <boost/cstdfloat.hpp>
#include <boost/multiprecision/gmp.hpp>
#ifndef COMPUTE_CANADA
#include <boost/multiprecision/mpfi.hpp>
#endif
#include <boost/multiprecision/mpfr.hpp>

// This is almost certainly just a typedef of double, but it is good to be safe.
// Fixed width floats may be part of a future C++ standard, in which case
// you should use that instead of the boost version
using boost::float64_t;

// All numbers passed to functions should be passed as a ParamType
// Because we might change a number later, and then that would
// necessitate changing to (pass by reference vs value)

// This type should never be larger than the Coeff64 type.
// This way, we can use it safely as a coefficient.
// We already use 32 bit ints in Java, so this shouldn't be an issue
using CodeNumber = int32_t;

// It's important to have typedefs for specific uses. This way you know exactly
// what is changing if you alter the typedef. It keeps things separate.
// There would be some cases where it would be nice to use an unsigned integer
// (such as when doing the gradient bounds), but we can't because those are unchecked
using Coeff16 = int16_t; // would be nice to use as coefficients
using Coeff32 = int32_t; // used for coefficients in the equations
using Coeff64 = int64_t; // used for sums of those coefficients

using Float = float64_t; // floating point type to use in evaluations

using Integer = boost::multiprecision::mpz_int;

using Rational = boost::multiprecision::mpq_rational;

using Real = boost::multiprecision::mpfr_float_50;

#ifndef COMPUTE_CANADA
using Interval = boost::multiprecision::mpfi_float_50;
#endif

// NOTE: Because boost uses the number backend to optimize mathematical expressions,
// you must explicitly write the type of number you expect. Eg
// const auto x = interval1 * interval2;    // wrong!
// const Interval x = interval1 * interval2; // right!

// For exact numbers, such as integers and rationals, you can do all the algebraic
// manipulations you want, and the end result will be the same. For floating point
// numbers this is *not* the case. Performing algebraic manipulations on an
// expression can change the resulting number due to rounding effects. Often,
// the simplest algebraic expression for a problem is not very accurate when
// evaluated using floating point numbers. Instead, a more involved and not
// simplified expression will give you a more accurate result.
// In short, for these algorithms just do what the experts do. There are many
// tricky and unintuitive things you need to watch out for.
