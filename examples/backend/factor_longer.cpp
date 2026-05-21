
#include "equations.hpp"
#include "parse.hpp"

int main() {

    // 1 3 31 1 3 44 3 1 31 3 1 18
    const auto eq0 = parse_lin_com_sin_ab("sin(9*a+b)-sin(9*a+3*b)-sin(11*a+b)+sin(11*a+5*b)+sin(21*a-5*b)-sin(21*a-3*b)").build();
    // As above, with sin(b) factored out
    const auto eq1 = parse_lin_com_sin_ab("-sin(4*b)+sin(2*a-4*b)-sin(2*a+4*b)+sin(4*a-4*b)-sin(4*a+4*b)+sin(6*a-4*b)-sin(6*a+4*b)+sin(8*a-4*b)-sin(8*a+4*b)+sin(10*a-4*b)-sin(10*a+2*b)-sin(10*a+4*b)+sin(12*a-4*b)+sin(14*a-4*b)+sin(16*a-4*b)+sin(18*a-4*b)+sin(20*a-4*b)").build();

    const Interval a = boost::math::constants::pi<Interval>();

    const auto r0 = evalf<Interval>(eq0, a, a);
    std::cout << r0 << std::endl;
    std::cout << boost::multiprecision::width(r0) << std::endl;

    const auto r1 = evalf<Interval>(eq1, a, a);
    std::cout << r1 << std::endl;
    std::cout << boost::multiprecision::width(r1) << std::endl;

    // The interval width for the first one is smaller, but not by much. Certainly not
    // as much as you would expect considering the differences in lengths. I suppose
    // the factors of 21 in the original equation increase the width somewhat.
    // However, the first one will certainly evaluate be faster than the second one. That is for
    // sure, since there are far less sines.

}
