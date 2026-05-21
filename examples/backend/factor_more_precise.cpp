#include "equations.hpp"
#include "parse.hpp"

int main() {

    // Here we see that the factored version, even though it is longer, is more precise.
    // 1 1 3 1 2 1 4
    const auto eq0 = parse_lin_com_sin_ab("sin(a-2*b)+sin(a+2*b)-sin(5*a+2*b)+sin(5*a+4*b)").build();
    const auto eq1 = parse_lin_com_cos_ab("-cos(2*a-b)+cos(2*a+b)-cos(4*a+b)+cos(4*a+3*b)+cos(b)").build();

    const Interval a{Real{"1.570796326794896861766946727972724961005046815809727"},
                     Real{"1.570796326794896861766946727972724961005046815809738"}};
    const Interval b{Real{"1.570796326794895649088821546307857366472736235198834"},
                     Real{"1.570796326794895649088821546307857366472736235198845"}};

    const auto r0 = evalf<Interval>(eq0, a, b);
    std::cout << r0 << std::endl;
    std::cout << boost::multiprecision::width(r0) << std::endl;

    const auto r1 = evalf<Interval>(eq1, a, b);
    std::cout << r1 << std::endl;
    std::cout << boost::multiprecision::width(r1) << std::endl;
}

