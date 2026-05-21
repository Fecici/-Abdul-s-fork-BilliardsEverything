#pragma once

#include "diff.hpp"
#include "evalf.hpp"

inline bool test_delta(const Vector2<Real>& dx, ParamType<Real> eps) {
    return (abs(dx[0]) < eps) && (abs(dx[1]) < eps);
}

// What happens if there is no root in the given region, as is the case for the
// following example? Then it looks like it converges to one of the endpoints,
// chasing a root that lies outside the region. Doing a sign check at the end of
// the thing will then catch this, because you aren't at a root. I think it is always
// best to do one final sign check. This ensures we have actually converged to a root,
// and that the fudging of the root we have is enough to cover the actual root.
/*
const auto eq0 = parse_enum_com_abeta("-5*a-b+6*eta").build();
const auto eq1 = parse_lin_com_cos_ab("cos(0)-cos(2*a+2*b)-cos(2*a+4*b)+cos(6*a+4*b)").build();

const EquationGradient<EnumComZ<XYEta>> eq_grad0{eq0};
const EquationGradient<LinComZ<Cos<EnumComZ<XY>>>> eq_grad1{eq1};

//const Vector2<Real> a{1.72788, 0.785398};
//const Vector2<Real> b{1.5708, 1.5708};

const Vector2<Real> a{1.5910, 1.3980};
const Vector2<Real> b{1.6342, 1.1453};
*/

// WARNING: all instances of this class must be temporaries
template <typename Symbols, typename Eq0, typename Eq1, typename Func>
class Newton final {

  private:
    const EquationGradient<Symbols, Eq0>& eq0;
    const EquationGradient<Symbols, Eq1>& eq1;

    // Function that tells you if the x_trial is inside the solution
    // region or not.
    const Func& inside;

    // Determines if every step must reduce the norm
    const bool reduce_norm;

    // This is a reimplimentation of GSL's gnewton multi-dimensional root solver to
    // use MPFR (GSL is hardcoded to work with doubles). For now, this solver seems
    // to work correctly. If in some cases it doesn't work, we could switch to
    // the hybridsj version in GSL (o2scl has an implementation of that algorithm in C++).
  public:
    // We are using Eigen for linear algebra, because it can be used with custom numeric type,
    // not just floats and doubles (as opposed to something like armadillo or blaze)
    // Reduce the norm by default
    explicit Newton(const EquationGradient<Symbols, Eq0>& eq0_, const EquationGradient<Symbols, Eq1>& eq1_, const Func& inside_, const bool reduce_norm_ = true)
        : eq0{eq0_},
          eq1{eq1_},
          inside{inside_},
          reduce_norm{reduce_norm_} {
    }

    Vector2<Real> f(const Vector2<Real>& x) const {
        Vector2<Real> y;
        y(0) = evalf<Real>(eq0.equation, x[0], x[1]);
        y(1) = evalf<Real>(eq1.equation, x[0], x[1]);

        return y;
    }

    Matrix2<Real> jac(const Vector2<Real>& x) const {

        Matrix2<Real> j;
        j(0, 0) = evalf<Real>(eq0.diff0, x[0], x[1]);
        j(0, 1) = evalf<Real>(eq0.diff1, x[0], x[1]);
        j(1, 0) = evalf<Real>(eq1.diff0, x[0], x[1]);
        j(1, 1) = evalf<Real>(eq1.diff1, x[0], x[1]);

        return j;
    }

    // The initial approximation x
    Vector2<Real> solve(Vector2<Real> x) const {

        // Once each component of dx is less than eps,
        // we return the approximation.
        // Can't be 1e-45 for 1 1 48 1 1 150 1 2 1 55 1 1 156 1 1 52 1 1 154 1 1 52 1 1 156 1 1 55 1 2 1 150
        // (At least at 50 digits of precision)
        const Real eps{"1e-40"};

        constexpr uint64_t max_iters = 100;

        // x -> x + dx
        Vector2<Real> dx;

        // TODO check if we can reduce temporaries, and if so will it
        // give us a performance boost.
        uint64_t iters = 0;
        bool within_eps = false;
        while (!within_eps) {

            gnewton_iterate(x, dx);
            //std::cout << x << std::endl;
            //std::cout << dx << std::endl;
            //std::cout << std::endl;

            if (test_delta(dx, eps)) {
                within_eps = true;
            }

            iters += 1;
            if (iters > max_iters && !within_eps) {
                throw std::runtime_error("maximum iterations exceeded in newton's method");
            }
        }

        /*
        std::cout << eq0.equation << '\n'
                  << eq1.equation << '\n'
                  << a << '\n'
                  << b << '\n'
                  << x << '\n'
                  << std::endl;;

        std::cout << x[0].str() << ", " << x[1].str() << std::endl;
        */

        return x;
    }

    void gnewton_iterate(Vector2<Real>& x, Vector2<Real>& dx) const {

        // y = f(x)
        auto y = f(x);

        // norm = |f(x)|
        const Real norm = y.norm();

        const auto j = jac(x);

        //std::cout << "x: " << x << std::endl;
        //std::cout << "f(x): " << y << std::endl;
        //std::cout << "j(x): " << j << std::endl;

        // For a simple 2x2 system, it may seem like overkill to use an LU solver,
        // especially when there are more straightforward methods like Cramer's rule.
        // However, this is one of the many cases where clean mathematical theory
        // breaks down in practice. For even 2x2 systems of floating point numbers,
        // Cramer's rule is numerically unstable (see Accuracy and Stability of
        // Numerical Algorithms, Second Edition, page ___). A full pivot LU solver
        // is stable and should be used instead. Note that this is only true for floating point
        // numbers. For exact numbers, like integers and rationals, Cramer's rule
        // is of course fine. It is only when rounding is added to the mix that it
        // breaks down.
        dx = j.fullPivLu().solve(-y);

        Real t = 1;

        Vector2<Real> x_trial = x + t * dx;

        // halve t until x_trial is inside the interval
        // TODO should just move t back until point_sign_line(x_trial) == 0,
        // and then let the normal gnewton take over
        // Also, only one of these will ever be negative
        // It is rather straigtforward to do this
        // But how much should we fall back?
        //
        // Sometimes dx points in the direction of the root we want, but it is way to big
        // and oversteps that root, and then finds itself close to another root that it
        // converges to. This keeps dx always within the region where we expect the
        // root to be.
        while (!inside(x_trial)) {
            t /= 2;
            x_trial = x + t * dx;
        }

        const Real epsilon = std::numeric_limits<Real>::epsilon();

        // Alright, so we are inside the line, now let's check that the residual is reduced
        // If reduce_norm is false, we don't reduce the norm
        bool repeat = reduce_norm;
        while (repeat) {
            x_trial = x + t * dx;

            y = f(x_trial);

            // trial_norm = |f(x_trial)|
            const Real trial_norm = y.norm();

            // Check if the step goes uphill (increases the norm).
            // If so, scale the step down and try again
            if (trial_norm > norm && t > epsilon) {

                const Real r = trial_norm / norm;
                const Real u = (sqrt(1.0 + 6.0 * r) - 1.0) / (3.0 * r);

                t *= u;
            } else {
                repeat = false;
            }
        }

        // Update x and dx so we can see their new values outside the function
        dx *= t;
        x = x_trial;
    }
};
