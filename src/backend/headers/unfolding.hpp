#pragma once

#include "general.hpp"

#include <boost/asio/thread_pool.hpp>
#include <boost/asio/post.hpp>
#include <thread>
#include <vector>
#include <algorithm>

struct Edge final {
    XYZ edge_type;
    LinComArrZ<XYPi> polar_angle;

    explicit Edge(const XYZ edge_type_, const LinComArrZ<XYPi>& polar_angle_)
        : edge_type{edge_type_}, polar_angle{polar_angle_} {}

    friend bool operator==(const Edge& lhs, const Edge& rhs) {
        return std::tie(lhs.edge_type, lhs.polar_angle) == std::tie(rhs.edge_type, rhs.polar_angle);
    }

    friend bool operator<(const Edge& lhs, const Edge& rhs) {
        return std::tie(lhs.edge_type, lhs.polar_angle) < std::tie(rhs.edge_type, rhs.polar_angle);
    }

    friend std::ostream& operator<<(std::ostream& os, const Edge& edge) {
        return os << '(' << edge.edge_type << ", " << edge.polar_angle << ')';
    }


};

class Unfolding final {
  private:
    std::map<std::pair<Vertex, Vertex>, Edge> edges;
    std::vector<Vertex> left_vertices;
    std::vector<Vertex> right_vertices;

    // given an unfolding and a path from one point to another, this gives the vector going from the first point to the second.
    // The first coordinate is x, the second y
    std::pair<Equation<Sin>, Equation<Cos>> path_vector(const std::vector<Vertex>& path) const;

  public:
    explicit Unfolding(const std::vector<CodeNumber>& tmp_code_numbers, const std::vector<XYZ>& tmp_code_angles);

    // shooting vector in the general case
    // we use this for OSNO, since we can't calculate the vector
    // straight up for that one
    std::pair<Equation<Sin>, Equation<Cos>> shooting_vector_general() const;

    std::set<std::pair<Equation<Sin>, Equation<Cos>>> get_all_vectors() const;

    template <template <typename> class T, template <typename> class S>
    Curves generate_curves(const Equation<T>& shooting_vector_x, const Equation<S>& shooting_vector_y, const InitialAngles& initial_angles) const;

    template <template <typename> class T, template <typename> class S>
    Curves generate_curves(const Equation<T>& shooting_vector_x, const Equation<S>& shooting_vector_y, const InitialAngles& initial_angles, const PointQ& center, const Rational& rx, const Rational& ry) const;

    template <template <typename> class T, template <typename> class S>
    CurvesLR generate_curves_lr(const Equation<T>& shooting_vector_x, const Equation<S>& shooting_vector_y) const;

    template <template <typename> class T, template <typename> class S>
    CurvesLR generate_curves_lr(const Equation<T>& shooting_vector_x, const Equation<S>& shooting_vector_y, const std::vector<LeftRight>& left_rights) const;

};

extern template Curves Unfolding::generate_curves(const Equation<Sin>& shooting_vector_x, const Equation<Cos>& shooting_vector_y, const InitialAngles& initial_angles) const;
extern template Curves Unfolding::generate_curves(const Equation<Cos>& shooting_vector_x, const Equation<Sin>& shooting_vector_y, const InitialAngles& initial_angles) const;

extern template Curves Unfolding::generate_curves(const Equation<Sin>& shooting_vector_x, const Equation<Cos>& shooting_vector_y, const InitialAngles& initial_angles, const PointQ& center, const Rational& rx, const Rational& ry) const;
extern template Curves Unfolding::generate_curves(const Equation<Cos>& shooting_vector_x, const Equation<Sin>& shooting_vector_y, const InitialAngles& initial_angles, const PointQ& center, const Rational& rx, const Rational& ry) const;

extern template CurvesLR Unfolding::generate_curves_lr(const Equation<Sin>& shooting_vector_x, const Equation<Cos>& shooting_vector_y) const;
extern template CurvesLR Unfolding::generate_curves_lr(const Equation<Cos>& shooting_vector_x, const Equation<Sin>& shooting_vector_y) const;

extern template CurvesLR Unfolding::generate_curves_lr(const Equation<Sin>& shooting_vector_x, const Equation<Cos>& shooting_vector_y, const std::vector<LeftRight>& left_rights) const;
extern template CurvesLR Unfolding::generate_curves_lr(const Equation<Cos>& shooting_vector_x, const Equation<Sin>& shooting_vector_y, const std::vector<LeftRight>& left_rights) const;
