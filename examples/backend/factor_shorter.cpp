#include "equations.hpp"
#include "parse.hpp"

int main() {

    // 2 4 4 6
    // This is zero, fudged 1e-15
    // (0, 0), ({0.713998,0.713998}, {0.428399,0.428399})
    const Interval a{Real{"8.574929257125441868932577761096445700766762257185121e-16"}, Real{"8.574929257125441868932577761096445700766762257185406e-16"}};
    const Interval b{Real{"5.144957554275265121359546656657867420460057354311068e-16"}, Real{"5.144957554275265121359546656657867420460057354311258e-16"}};

    const auto eq0 = parse_lin_com_sin_ab("2*sin(b)+sin(3*b)-sin(5*b)+sin(2*a-3*b)-sin(2*a+b)-sin(4*a-5*b)+sin(4*a-b)").build();
    // sin(a) sin(b) factored out
    const auto eq1 = parse_lin_com_sin_ab("-sin(a-4*b)-sin(3*a-4*b)-sin(3*a-2*b)+sin(a)").build();
    // cos(b) factored out
    const auto eq2 = parse_lin_com_sin_ab("-sin(a-3*b)-sin(3*a-3*b)+sin(a-b)").build();

    // sin(2b) = sin(b)cos(b) factored out
    //cos(4 a-3 b)-cos(2 a-b)+cos(b)-cos(3 b)
    
    // sin(a) factored out
    //cos(a-5 b)+cos(3 a-5 b)-cos(a-3 b)-cos(a-b)-cos(3 a-b)+cos(a+b))

    const auto r0 = evalf<Interval>(eq0, a, b);
    std::cout << r0 << std::endl;
    std::cout << boost::multiprecision::width(r0) << std::endl;

    const auto r1 = evalf<Interval>(eq1, a, b);
    std::cout << r1 << std::endl;
    std::cout << boost::multiprecision::width(r1) << std::endl;

    const auto r2 = evalf<Interval>(eq2, a, b);
    std::cout << r2 << std::endl;
    std::cout << boost::multiprecision::width(r2) << std::endl;

    // If you run this program, you can see that the shorter equations have the smaller interval widths.
    // So the shorter the equation, the more precise the result (makes sense).
    // Now, the equation is actually POS at this point, but the first one thinks it is ZERO.
    // This happens not because the extra lines are also close to that point, which messes it up.
    // It messes up because the equations are there, and they make it longer. That is why it
    // messes up.

}
