#include "general.hpp"

template class geometry::Point<Rational>;
template class geometry::Segment<Rational, geometry::Topology::Open>;
template class geometry::Rectangle<Rational, geometry::Topology::Open>;
template class geometry::ConvexPolygon<Rational, geometry::Topology::Open>;
template class geometry::Segment<Rational, geometry::Topology::Closed>;
template class geometry::Rectangle<Rational, geometry::Topology::Closed>;
template class geometry::ConvexPolygon<Rational, geometry::Topology::Closed>;

template class math::LinComArr<XY, Coeff64>;
template class math::LinComArr<XYPi, Coeff64>;
template class math::LinComArr<XYEta, Coeff64>;

template class math::LinComMap<Sin<LinComArrZ<XY>>, Coeff64>;
template class math::LinComMap<Cos<LinComArrZ<XY>>, Coeff64>;

template class math::LinComVec<Sin<LinComArrZ<XY>>, Coeff16>;
template class math::LinComVec<Cos<LinComArrZ<XY>>, Coeff16>;

// Parse a signed 64 bit integer integer
static int64_t parse_int(const char* start, const char* end) {

    // optional +/- at the start, followed by digits

    if (start == end) {
        throw std::runtime_error("parse_int: cannot parse empty string");
    }

    int64_t sign = 1; // Positive by default, unless overriden by a sign
    if (*start == '+') {
        ++start;
    } else if (*start == '-') {
        sign = -1;
        ++start;
    }

    if (start == end) {
        throw std::runtime_error("parse_int: cannot just have a sign");
    }

    int64_t number = 0;
    while (start != end) {

        const char ch = *start;

        // The C standard guarantees that '0' .. '9' are contiguous
        //https://stackoverflow.com/questions/628761/convert-a-character-digit-to-the-corresponding-integer-in-c
        if ((ch < '0') || (ch > '9')) {
            throw std::runtime_error("parse_int: not a character");
        }

        const int64_t digit = ch - '0';

        // But, we need to do an overflow check
        if (number > std::numeric_limits<int64_t>::max() / 10) {
            throw std::runtime_error("parse_int: integer overflow");
        }

        number *= 10;

        // Now check if number + digit > max
        if (number > std::numeric_limits<int64_t>::max() - digit) {
            throw std::runtime_error("parse_int: integer overflow");
        }

        number += digit;

        ++start;
    }

    // Sadly, this function is unable to parse INT_MIN, because that overflows. Oh well

    // This will never underflow, since number is two's complement
    return sign * number;
}

Rational decimal_to_rational(std::string str) {

    // It must be a decimal float, with an optional +/- at the front,
    // and an optional decimal point.

    // Now we find the index of the decimal point
    size_t index = 0;
    bool found = false;

    for (size_t i = 0; i < str.size(); ++i) {
        if (str.at(i) == '.') {
            found = true;
            index = i;
            break;
        }
    }

    if (found) {

        // Number of characters after the decimal point
        // Note that this works fine if there is nothing before the decimal point or nothing after
        const size_t den_pow = str.size() - index - 1;

        // The denominator is 10^den_pow;
        Integer denom = 1;
        for (size_t i = 0; i < den_pow; ++i) {
            denom *= 10;
        }

        // Erase the decimal point
        str.erase(index, 1);

        const auto numer = parse_int(str.data(), str.data() + str.size());

        return Rational{numer} / denom;

    } else {
        // There is no decimal point, so it's an integer
        // Just parse it, and that's the numerator

        const auto numer = parse_int(str.data(), str.data() + str.size());

        return Rational{numer};
    }
}

// These are the gradient bounds used when coloring the regions
// Here would be the perfect place to use unsigned ints, but we don't have overflow checking for
// them sadly
template <template <typename> class Trig>
std::pair<Coeff64, Coeff64> gradient_bounds(const LinComMapZ<Trig<LinComArrZ<XY>>>& eq) {
    Coeff64 sum_x = 0;
    Coeff64 sum_y = 0;

    for (const auto& kv : eq) {

        const auto coeff = kv.second;

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff(XY::X);
        const auto y_coeff = arg.coeff(XY::Y);

        sum_x += math::abs(coeff) * math::abs(x_coeff);
        sum_y += math::abs(coeff) * math::abs(y_coeff);
    }

    return {sum_x, sum_y};
}

template std::pair<Coeff64, Coeff64> gradient_bounds(const LinComMapZ<Sin<LinComArrZ<XY>>>& eq);
template std::pair<Coeff64, Coeff64> gradient_bounds(const LinComMapZ<Cos<LinComArrZ<XY>>>& eq);

// Split the segment in half
std::array<ClosedSegmentQ, 2> subdivide(const ClosedSegmentQ& seg) {

    const auto midpoint = seg.midpoint();

    const ClosedSegmentQ first{seg.start(), midpoint};
    const ClosedSegmentQ second{midpoint, seg.end()};

    // This is first, second
    return {{first, second}};
}
