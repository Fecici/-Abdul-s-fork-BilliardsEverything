#pragma once

#include "database/serialize.hpp"
#include "general.hpp"
#include "refine.hpp"

std::vector<std::array<Interval, 3>> calculate_all_points(const IntervalPolygon& polygon);

std::vector<std::array<Interval, 3>> calculate_all_points(const IntervalLineSegment& line_seg);

template <size_t N>
class LessThan final {
  public:
    // return if first[N] < second[N]
    bool operator()(const std::array<Interval, 3>& first, const std::array<Interval, 3>& second) const {
        return compare_interval(std::get<N>(first), std::get<N>(second)) == Order::Less;
    }
};

template <size_t N>
Interval min(const std::vector<std::array<Interval, 3>>& points) {
    const auto iter = std::min_element(std::begin(points), std::end(points), LessThan<N>{});
    return std::get<N>(*iter);
}

template <size_t N>
Interval max(const std::vector<std::array<Interval, 3>>& points) {
    const auto iter = std::max_element(std::begin(points), std::end(points), LessThan<N>{});
    return std::get<N>(*iter);
}

std::array<XYZ, 3> permute_angles(const std::vector<std::array<Interval, 3>>& points);

std::vector<Vector2<Interval>> rearrange_points(const std::vector<std::array<Interval, 3>>& all_points, const std::array<XYZ, 3>& permutation);

std::array<XYZ, 3> invert_permutation(const std::array<XYZ, 3>& perm);

LinComArrZ<XYPi> rearrange_enum_com(const LinComArrZ<XY>& enum_com, const std::array<LinComArrZ<XYPi>, 3>& perm);

LinComArrZ<XYEta> rearrange_enum_com(const LinComArrZ<XYEta>& enum_com, const std::array<LinComArrZ<XYEta>, 3>& perm);

LinComMapZ<Sin<LinComArrZ<XY>>> rearrange_lin_com(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com, const std::array<LinComArrZ<XYPi>, 3>& perm);

LinComMapZ<Cos<LinComArrZ<XY>>> rearrange_lin_com(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com, const std::array<LinComArrZ<XYPi>, 3>& perm);

class RearrangeVariant final : public boost::static_visitor<std::string> {
  private:
    const std::array<LinComArrZ<XYEta>, 3>& inverse_perm_eta;
    const std::array<LinComArrZ<XYPi>, 3>& inverse_perm_pi;

  public:
    explicit RearrangeVariant(const std::array<LinComArrZ<XYEta>, 3>& inverse_perm_eta_, const std::array<LinComArrZ<XYPi>, 3>& inverse_perm_pi_)
        :

          inverse_perm_eta{inverse_perm_eta_},
          inverse_perm_pi{inverse_perm_pi_} {
    }

    std::string operator()(const EquationGradient<XY, LinComArrZ<XYEta>>&) const {
        // TODO deal with this case properly
        // Only do the error thing to make eta a used variable

        std::ostringstream err;
        err << std::get<0>(inverse_perm_eta);
        throw std::runtime_error("this isn't supposed to happen" + err.str());
        //std::ostringstream err;
        //err << std::get<0>(inverse_perm_eta);
        //throw std::runtime_error("this isn't supposed to happen" + err.str());

        return "";

        //const auto rearranged = rearrange_enum_com(eq_grad.equation, inverse_perm_eta).build();
        //std::cout << rearranged << std::endl; // IMPORTANT A June 16, 2016
        //std::ostringstream oss;
        //oss << rearranged;
        //return oss.str();
    }

    std::string operator()(const EquationGradient<XY, LinComMapZ<Sin<LinComArrZ<XY>>>>& eq_grad) const {
        const auto rearranged = rearrange_lin_com(eq_grad.equation, inverse_perm_pi);

        std::ostringstream oss{};
        oss << "sin ";
        database::serialize(oss, rearranged);
        return oss.str();
    }

    std::string operator()(const EquationGradient<XY, LinComMapZ<Cos<LinComArrZ<XY>>>>& eq_grad) const {
        const auto rearranged = rearrange_lin_com(eq_grad.equation, inverse_perm_pi);

        std::ostringstream oss{};
        oss << "cos ";
        database::serialize(oss, rearranged);
        return oss.str();
    }
};
