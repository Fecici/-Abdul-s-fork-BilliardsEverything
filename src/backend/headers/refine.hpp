#pragma once

#include "evalf.hpp"
#include "general.hpp"
#include "gradient.hpp"
#include "intersection.hpp"
#include "linear_derivative.hpp"

#include <thread>

#include <tbb/parallel_for.h>
#include <tbb/concurrent_vector.h>

struct EquationPrinter final : public boost::static_visitor<std::string> {
    template <typename Symbols, typename S>
    std::string operator()(const EquationGradient<Symbols, S>& eq_grad) const {
        // TODO is there a way of overloading << for this case instead of returning a string
        std::ostringstream oss;
        oss << eq_grad.equation;
        return oss.str();
    }
};

// This is an MRR region boundary equation
using BoundaryEquation = boost::variant<EquationGradient<XY, LinComArrZ<XYEta>>, EquationGradient<XY, LinComMapZ<Sin<LinComArrZ<XY>>>>, EquationGradient<XY, LinComMapZ<Cos<LinComArrZ<XY>>>>>;

class IntervalLineSegment final {
  public:
    // equation_i intersects point_i with the constraint (so it's a "boundary equation")
    Vector2<Interval> point0;
    BoundaryEquation equation0;
    Vector2<Interval> point1;
    BoundaryEquation equation1;

    explicit IntervalLineSegment(const Vector2<Interval>& point0_, const BoundaryEquation& equation0_, const Vector2<Interval>& point1_, const BoundaryEquation& equation1_)
        : point0{point0_},
          equation0{equation0_},
          point1{point1_},
          equation1{equation1_} {
    }
};

// The point starts first, and the equation goes from that point to the point of
// the next pair
class IntervalPair final {
  public:
    Vector2<Interval> point;
    BoundaryEquation equation;

    explicit IntervalPair(const Vector2<Interval>& point_, const BoundaryEquation& equation_)
        : point{point_},
          equation{equation_} {
    }
};

// Strictly speaking this isn't a polygon, but close enough. It's a "generalized" polygon
using IntervalPolygon = std::vector<IntervalPair>;

void print_region(const IntervalPolygon& region);

void print_region(const IntervalLineSegment& line_seg);

template <typename T>
boost::optional<IntervalLineSegment> refine_line_segment(const IntervalLineSegment& line_segment, const T& curve, const EquationGradient<XY, LinComArrZ<XYEta>>& constraint);

extern template boost::optional<IntervalLineSegment> refine_line_segment<Equation<Sin>>(const IntervalLineSegment& line_segment, const Equation<Sin>& curve, const EquationGradient<XY, LinComArrZ<XYEta>>& constraint);
extern template boost::optional<IntervalLineSegment> refine_line_segment<Equation<Cos>>(const IntervalLineSegment& line_segment, const Equation<Cos>& curve, const EquationGradient<XY, LinComArrZ<XYEta>>& constraint);

template <typename T>
boost::optional<IntervalPolygon> refine_polygon(const IntervalPolygon& polygon, const T& curve);

extern template boost::optional<IntervalPolygon> refine_polygon<Equation<Sin>>(const IntervalPolygon& polygon, const Equation<Sin>& curve);
extern template boost::optional<IntervalPolygon> refine_polygon<Equation<Cos>>(const IntervalPolygon& polygon, const Equation<Cos>& curve);
