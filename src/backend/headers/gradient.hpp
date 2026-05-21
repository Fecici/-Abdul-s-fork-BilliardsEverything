#pragma once

#include "diff.hpp"
#include "general.hpp"

Vector2<Interval> gradient(const EquationGradient<XY, LinComArrZ<XYEta>>& eq_grad, const Vector2<Interval>& point);

template <template <typename> class Trig>
Vector2<Interval> gradient(const EquationGradient<XY, Equation<Trig>>& eq_grad, const Vector2<Interval>& point);

extern template Vector2<Interval> gradient(const EquationGradient<XY, Equation<Sin>>& eq_grad, const Vector2<Interval>& point);
extern template Vector2<Interval> gradient(const EquationGradient<XY, Equation<Cos>>& eq_grad, const Vector2<Interval>& point);

// WARNING: all instances of this class should be created as temporaries
class GradientVariant final : public boost::static_visitor<Vector2<Interval>> {
  private:
    const Vector2<Interval>& point;

  public:
    explicit GradientVariant(const Vector2<Interval>& point_)
        : point{point_} {}

    template <typename S>
    Vector2<Interval> operator()(const EquationGradient<XY, S>& eq_grad) const {
        return gradient(eq_grad, point);
    }
};
