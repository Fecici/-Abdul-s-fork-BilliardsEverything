#pragma once

// TODO rename this to the math header, or something similar

#include <array>
#include <iostream>
#include <map>
#include <set>
#include <sstream>

#ifndef COMPUTE_CANADA
#include <eigen3/Eigen/Dense>
#endif

#include "basic.hpp"
#include "falgo.hpp"
#include "geometry/algorithms.hpp"
#include "math/lin_com_arr.hpp"
#include "math/lin_com_map.hpp"
#include "math/lin_com_vec.hpp"
#include "math/monomial.hpp"
#include "math/polynomial.hpp"
#include "math/symbols.hpp"
#include "math/trig.hpp"
#include "numbers.hpp"
#include "vertex_left_right.hpp"

// These are the only geometry types we use in the program, and we explicitly instantiate
// them in the cpp file. Using the extern suppresses instantiation here and in any file
// that includes this header.
using PointQ = geometry::Point<Rational>;
using OpenSegmentQ = geometry::Segment<Rational, geometry::Topology::Open>;
using OpenRectangleQ = geometry::Rectangle<Rational, geometry::Topology::Open>;
using OpenConvexPolygonQ = geometry::ConvexPolygon<Rational, geometry::Topology::Open>;
using ClosedSegmentQ = geometry::Segment<Rational, geometry::Topology::Closed>;
using ClosedRectangleQ = geometry::Rectangle<Rational, geometry::Topology::Closed>;
using ClosedConvexPolygonQ = geometry::ConvexPolygon<Rational, geometry::Topology::Closed>;

extern template class geometry::Point<Rational>;
extern template class geometry::Segment<Rational, geometry::Topology::Open>;
extern template class geometry::Rectangle<Rational, geometry::Topology::Open>;
extern template class geometry::ConvexPolygon<Rational, geometry::Topology::Open>;
extern template class geometry::Segment<Rational, geometry::Topology::Closed>;
extern template class geometry::Rectangle<Rational, geometry::Topology::Closed>;
extern template class geometry::ConvexPolygon<Rational, geometry::Topology::Closed>;

// PointQ is a rational multiple of eta (pi / 2)
// Vector2<Interval> is an interval radian value
// Vector2<Real> is a real radian value
// Vector2<Float> is a floating point radian value

template <typename T>
using LinComArrZ = math::LinComArr<T, Coeff64>;
extern template class math::LinComArr<XY, Coeff64>;
extern template class math::LinComArr<XYPi, Coeff64>;
extern template class math::LinComArr<XYEta, Coeff64>;

template <typename T>
using LinComArrQ = math::LinComArr<T, Rational>;
//extern template class math::LinComArr<XEta, Rational>;
//extern template class math::LinComArr<YEta, Rational>;

template <typename T>
using LinComMapZ = math::LinComMap<T, Coeff64>;
extern template class math::LinComMap<Sin<LinComArrZ<XY>>, Coeff64>;
extern template class math::LinComMap<Cos<LinComArrZ<XY>>, Coeff64>;

template <typename T>
using LinComMapQ = math::LinComMap<T, Rational>;
//extern template class math::LinComMap<Cos<LinComArrQ<XEta>>, Rational>;
//extern template class math::LinComMap<Sin<LinComArrQ<XEta>>, Rational>;
//extern template class math::LinComMap<Cos<LinComArrQ<YEta>>, Rational>;
//extern template class math::LinComMap<Sin<LinComArrQ<YEta>>, Rational>;

template <typename T>
using LinComVecZ = math::LinComVec<T, Coeff16>;
extern template class math::LinComVec<Sin<LinComArrZ<XY>>, Coeff16>;
extern template class math::LinComVec<Cos<LinComArrZ<XY>>, Coeff16>;

// We don't want these types to be vectorized, so don't align them.
// This can cause some nasty bugs and undefined behaviour.
// I'm not sure I want to stick with Eigen
#ifndef COMPUTE_CANADA
template <typename T>
using Vector2 = Eigen::Matrix<T, 2, 1, Eigen::DontAlign>;
template <typename T>
using Matrix2 = Eigen::Matrix<T, 2, 2, Eigen::DontAlign>;
#endif

//extern template class Eigen::Matrix<Real, 2, 1, Eigen::DontAlign>;
//extern template class Eigen::Matrix<Real, 2, 2, Eigen::DontAlign>;
//extern template class Eigen::Matrix<Interval, 2, 2, Eigen::DontAlign>;

// TODO maybe rename this to EqMap?
// Another idea is replacing the maps with unordered_maps for better performance?
template <template <typename> class Trig>
using Equation = LinComMapZ<Trig<LinComArrZ<XY>>>;

using Curves = std::pair<std::set<Equation<Sin>>, std::set<Equation<Cos>>>;
using CurvesLR = std::pair<std::map<Equation<Sin>, std::vector<LeftRight>>, std::map<Equation<Cos>, std::vector<LeftRight>>>;

// TODO put the following class somewhere better
struct InitialAngles final {
    XYZ first;
    XYZ second;

    explicit InitialAngles(const XYZ first_, const XYZ second_)
        : first{first_}, second{second_} {}

    friend std::ostream& operator<<(std::ostream& os, const InitialAngles& initial_angles) {
        return os << initial_angles.first << initial_angles.second;
    }

    friend bool operator==(const InitialAngles& lhs, const InitialAngles& rhs) {
        return std::tie(lhs.first, lhs.second) == std::tie(rhs.first, rhs.second);
    }

    friend bool operator<(const InitialAngles& lhs, const InitialAngles& rhs) {
        return std::tie(lhs.first, lhs.second) < std::tie(rhs.first, rhs.second);
    }
};

// TODO rename these to Neg, Zero, and Pos
enum class Sign : uint64_t {
    NEG,
    ZERO,
    POS,
};

inline std::ostream& operator<<(std::ostream& os, const Sign sign) {
    switch (sign) {
    case Sign::NEG:
        return os << "neg";
    case Sign::ZERO:
        return os << "zero";
    case Sign::POS:
        return os << "pos";
    }

    throw std::runtime_error(invalid_enum_value("Sign", sign));
}

#ifndef COMPUTE_CANADA
template <uint32_t Precision>
Sign sign(const boost::multiprecision::number<boost::multiprecision::mpfi_float_backend<Precision>>& interval) {

    if (boost::multiprecision::lower(interval) > 0) {
        return Sign::POS;
    }

    if (boost::multiprecision::upper(interval) < 0) {
        return Sign::NEG;
    }

    // The problem is we could get a interval that is actually positive or
    // negative, but because we haven't calculated it to enough precision
    // it comes out as straddling 0. How can we guard against cases like
    // this?

    // Arbitrarily the precision of numbers using
    // Interval::default_precision won't help you if you want to
    // change the interval, because the interval widths of the inputs
    // will remain the same. We now know the interval widths to
    // higher precision, but they are just as wide as before.
    // You have to go back and recalculate the whole thing
    // at higher precision to make the interval widths of the
    // provided points smaller, and then that would do it.
    // I don't think this is necessary now. I think we'll just
    // pick a fixed precision, say 50, and increase that if we
    // need to.

    // This prevents numbers like NaN and +/-Inf from getting through
    if (boost::multiprecision::zero_in(interval)) {
        return Sign::ZERO;
    }

    std::ostringstream err;
    err << "unable to determine sign for " << interval;

    throw std::runtime_error(err.str());
}

enum class Order {
    Less,
    Equal,
    Greater,
};

// Impose a total order on Intervals. Need this for sorting
inline Order compare_interval(const Interval& a, const Interval& b) {

    if (boost::multiprecision::upper(a) < boost::multiprecision::lower(b)) {
        // a < b
        return Order::Less;
    }

    if (boost::multiprecision::lower(a) > boost::multiprecision::upper(b)) {
        // a > b
        return Order::Greater;
    }

    if (boost::multiprecision::overlap(a, b)) {
        // a == b
        return Order::Equal;
    }

    throw std::runtime_error("unorderable intervals");
}
#endif

// Convert a floating point number in decimal string form to a rational
// We don't convert to a float first, because that may introduce rounding
// problems in the decimal -> binary conversion. It must be exact.
Rational decimal_to_rational(std::string str);

// convert in the normal n/d format
Rational parse_rational(const std::string& str);

template <template <typename> class Trig>
std::pair<Coeff64, Coeff64> gradient_bounds(const Equation<Trig>& eq);

extern template std::pair<Coeff64, Coeff64> gradient_bounds(const Equation<Sin>& eq);
extern template std::pair<Coeff64, Coeff64> gradient_bounds(const Equation<Cos>& eq);

std::array<ClosedSegmentQ, 2> subdivide(const ClosedSegmentQ& seg);

// +-----+-----+
// |     |     |
// |     |     |
// +-----+-----+
// |     |     |
// |     |     |
// +-----+-----+
// Split the rect into four quarters
template <geometry::Topology Top>
std::array<geometry::Rectangle<Rational, Top>, 4> subdivide(const geometry::Rectangle<Rational, Top>& rect) {

    const auto center = rect.center();

    const geometry::Rectangle<Rational, Top> upper_left{{rect.interval_x().lower(), center.x},
                                                        {center.y, rect.interval_y().upper()}};

    const geometry::Rectangle<Rational, Top> upper_right{{center.x, rect.interval_x().upper()},
                                                         {center.y, rect.interval_y().upper()}};

    const geometry::Rectangle<Rational, Top> lower_left{{rect.interval_x().lower(), center.x},
                                                        {rect.interval_y().lower(), center.y}};

    const geometry::Rectangle<Rational, Top> lower_right{{center.x, rect.interval_x().upper()},
                                                         {rect.interval_y().lower(), center.y}};

    // This is UL, UR, LL, LR
    return {{upper_left, upper_right, lower_left, lower_right}};
}
