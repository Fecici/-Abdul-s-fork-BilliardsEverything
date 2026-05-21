#include "parse.hpp"
#include "equations.hpp"

int main() {
    // 1 1 23 1 2 1 25 1 1 48

    // Intersection of these two curves
    const auto eq0 = parse_lin_com_cos_ab("cos(a)-cos(25*a+24*b)-cos(25*a+26*b)+cos(49*a+48*b)").build();
    const auto eq1 = parse_lin_com_cos_ab("cos(a-2*b)+2*cos(a)+cos(23*a+22*b)-cos(25*a+22*b)-2*cos(25*a+24*b)-cos(25*a+26*b)-cos(27*a+24*b)+cos(51*a+48*b)").build();

    const Interval a{Real{"1.967091538343228993488541623069435020423602574314364"}, Real{"1.967091538343228993488541623069435020423602576314362"}};
    const Interval b{Real{"1.17450111524656424497410176021006786377356682306074"}, Real{"1.174501115246564244974101760210067863773566825060738"}};

    // Evaluating this curve at the point
    const auto eq = parse_lin_com_cos_ab("-cos(a+2*b)+cos(23*a+20*b)+cos(23*a+22*b)-cos(49*a+46*b)").build();

}
