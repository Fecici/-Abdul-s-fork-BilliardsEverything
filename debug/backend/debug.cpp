#include "merge.hpp"

int main() {

    std::vector<std::string> covers{"/Users/boyanmarinov/Documents/workspace/merge/cover_stable",
                                   "/Users/boyanmarinov/Documents/workspace/merge/cover_triangle"};
    merge::union_covers(covers);

    //const auto eq = parse_lin_com_map_sin_xy("sin(y)-sin(3y)+2sin(2x+y)");
    //const auto line = parse_lin_com_arr_xyeta("x-y");

    //const auto eq = parse_lin_com_map_sin_xy("-sin(x-2y)+sin(x)-sin(3x-2y)");
    //const auto line = parse_lin_com_arr_xyeta("3x-4y");

    //std::cout << in_terms_of<XY::X>(eq, line) << std::endl;
    //std::cout << in_terms_of<XY::Y>(eq, line) << std::endl;

    //std::cout << linear_derivative_sign(eq, line, {0, 0}, {1, 1}) << std::endl;

    //const auto eq = parse_lin_com_map_cos_xy("cos(6x+5y)-cos(10x-y)");
    //const auto factor = parse_lin_com_map_sin_xy("sin(2x-3y)");

    //std::cout << divide_once(eq, factor) << std::endl;

    //const auto eq = parse_lin_com_map_sin_xy("sin(14x+y)+sin(22x-y)");
    //const auto factor1 = parse_lin_com_map_sin_xy("sin(18x)");

    //const auto factor2 = parse_lin_com_map_cos_xy("cos(4x-y)");

    //std::cout << divide_once(eq, factor1) << std::endl;
    //std::cout << divide_once(eq, factor2) << std::endl;

    //const auto eq2 = parse_lin_com_map_cos_xy("-cos(x+2y)-cos(9x)+cos(11x+2y)-cos(25x-2y)+cos(27x)+cos(35x-2y)");
    //const auto factor3 = parse_lin_com_map_sin_xy("sin(7x-2y)-sin(9x)-sin(17x-2y)");

    //std::cout << divide_once(eq2, factor3) << std::endl;

    //const auto db_path = "/Users/boyanmarinov/billiard-databases/test-debug.sqlite";
    //const auto polygon_str = "23 49\n23 52\n26 52\n26 49";
    //const auto codes_str = "1 1 2 2 5\n1 3 2 6 3\n1 1 3 1 2 1 6\n1 1 4 2 3 1 3 7\n1 2 1 5 3 1 3 2 4 2 3 1 3 5\n1 1 2 1 1 7 3 1 3 2 6 2 3 1 3 7\n1 1 2 2 5 1 2 1 5 2 2 1 1 5 2 4 2 5\n1 1 3 1 2 1 5 2 3 1 3 6 2 3 1 3 7 1 1 3 1 2 1 6\n1 1 2 2 5 1 2 1 4 1 2 1 5 3 1 2 1 3 6 3 1 3 2 4 2 4 2 4 2 5\n1 1 2 2 5 1 2 1 5 3 1 2 1 3 5 1 2 1 5 2 2 1 1 5 2 4 2 4 2 4 2 5";
    //const auto unstable_str = "";

    //const int32_t digits = 5;
    //const int32_t subdivide = 10;
    //const int mrr = 1;

    //CCover cover;
    //cover_wrapper(db_path, polygon_str, codes_str, unstable_str, digits, subdivide, mrr, &cover);
    //cleanup_ccover(&cover);

    //const auto cover = check_cover(db_path, polygon_str, codes_str, unstable_str, digits, subdivide, mrr);

    //std::cout << cover.size() << std::endl;

    // Wrong!
    //const PointQ p{0, 2};

    //const ClosedConvexPolygonQ poly{{{0, 0}, {0, 1}, {1, 1}, {1, 0}}};

    //const ClosedRectangleQ rect{{0, 1}, {0, 1}};

    //const ClosedLineSegmentQ line{{-1, 0}, {2, 0}};
    //const ClosedLineSegmentQ line{{-1, 0}, {1, 2}};

    //std::cout << special_intersect(rect, line) << std::endl;
    //std::cout << intersection(rect, line) << std::endl;

    //std::cout << in(p, poly) << std::endl;

    //if (argc != 2) {
        //throw std::runtime_error("need exactly one arg");
    //}

    //const std::vector<CodeNumber> code_numbers{1, 1, 4, 2, 3, 1, 3, 7};
    //const auto var = CodeSequence::create(code_numbers);
    //const auto cs = boost::apply_visitor(CodeSequenceVariant{}, var);

    //auto db = connect(":memory:");

    //calculate_info(cs, true, db);

    //const auto eq1 = parse_lin_com_map_cos_xy("-2*cos(x-3*y)-4*cos(x-y)-5*cos(x+y)-2*cos(x+3*y)-2*cos(3*x-3*y)-3*cos(3*x-y)-4*cos(3*x+y)-cos(3*x+3*y)-cos(5*x-3*y)-2*cos(5*x-y)-2*cos(5*x+y)-cos(7*x-y)-cos(7*x+y)");
    //const auto eq2 = parse_lin_com_map_cos_xy("-2*cos(x+y)-cos(x+3*y)-cos(3*x-3*y)-cos(3*x+y)+cos(5*x+y)+cos(5*x+3*y)+cos(7*x-3*y)+cos(9*x-y)+cos(9*x+y)");

    //const auto eq1 = parse_lin_com_map_sin_xy("sin(y)-sin(2*x-y)-sin(4*x-3*y)-sin(4*x-y)-sin(6*x-3*y)-sin(6*x-y)");
    // I think we can factor a sin(2x) out of this. If that is the case, however, factoring out just
    // a sin(x) makes it longer (it gives us the above)
    //const auto eq2 = parse_lin_com_map_cos_xy("-cos(x+y)-cos(3*x-3*y)+cos(7*x-3*y)+cos(7*x-y)");

    //auto str = read_file(argv[1]);
    //boost::trim(str);

    //const auto eq = parse_lin_com_map_sin_xy(str);
    //const Interval x{0.351351351};
    //const Interval y{0.535351353};
    //std::cout.precision(50);
    //std::cout << evalf<Interval>(eq, x, y) << std::endl;

    //for (const auto& kv : eq) {
        //const auto trig_coeff = kv.second;
        //const auto x_coeff = kv.first.arg.coeff(XY::X);
        //const auto y_coeff = kv.first.arg.coeff(XY::Y);

        //std::cout << trig_coeff << ' ' << x_coeff << ' ' << y_coeff << ' ';
    //}

    //std::cout << std::endl;


    //std::cout << factor_full(eq1) << std::endl;
    //std::cout << factor_full(eq2) << std::endl;
}

