#pragma once

#include <ostream>   // std::ostream
#include <stdexcept> // std::runtime_error

#include "types.hpp"

namespace geometry {

// A proper bounded interval
template <typename N, Topology Top>
class Interval final {

  private:
    // for open intervals, min and max are not actually correct. Hence
    // we use lower and upper instead (whic are lower and upper endpoints)
    N lower_;
    N upper_;

    void check() const {
        // Degeneracies are not allowed, so lower_ < upper_

        if (lower_ >= upper_) {
            throw std::runtime_error("degenerate Interval: lower >= upper");
        }
    }

  public:
    Interval(const N& lower, const N& upper, const Check ch = Check::Checked)
        : lower_{lower}, upper_{upper} {

        if (ch == Check::Checked) {
            check();
        }
    }

    const N& lower() const {
        return lower_;
    }

    const N& upper() const {
        return upper_;
    }

    N length() const {
        return upper_ - lower_;
    }

    N center() const {
        return (lower_ + upper_) / 2;
    }

    friend bool operator==(const Interval<N, Top>& lhs, const Interval<N, Top>& rhs) {
        return std::tie(lhs.lower_, lhs.upper_) == std::tie(rhs.lower_, rhs.upper_);
    }

    friend bool operator!=(const Interval<N, Top>& lhs, const Interval<N, Top>& rhs) {
        return !(lhs == rhs);
    }

    friend bool operator<(const Interval<N, Top>& lhs, const Interval<N, Top>& rhs) {
        return std::tie(lhs.lower_, lhs.upper_) < std::tie(rhs.lower_, rhs.upper_);
    }

    friend bool operator>(const Interval<N, Top>& lhs, const Interval<N, Top>& rhs) {
        return rhs < lhs;
    }

    friend bool operator<=(const Interval<N, Top>& lhs, const Interval<N, Top>& rhs) {
        return !(lhs > rhs);
    }

    friend bool operator>=(const Interval<N, Top>& lhs, const Interval<N, Top>& rhs) {
        return rhs <= lhs;
    }
};

template <typename N>
inline std::ostream& operator<<(std::ostream& os, const Interval<N, Topology::Closed>& interval) {
    return os << '[' << interval.lower() << ", " << interval.upper() << ']';
}

template <typename N>
inline std::ostream& operator<<(std::ostream& os, const Interval<N, Topology::Open>& interval) {
    return os << ']' << interval.lower() << ", " << interval.upper() << '[';
}
}
