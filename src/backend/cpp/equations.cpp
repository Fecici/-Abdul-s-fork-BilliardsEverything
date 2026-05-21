// george jun11th 2021 to print the shooting vector, uncomment the lines with label_shooting_vector, (Note: 15 lines in total)

#include "equations.hpp"
#include "bounding_region.hpp"
#include "conversion.hpp"
#include "evalf.hpp"
#include "general.hpp"
#include "refine.hpp"
#include "reorder.hpp"
#include "shooting_vectors.hpp"
#include "trig_identities.hpp"
#include "unfolding.hpp"

// WARNING: always make this class a temporary
class LeftRightVariant final : public boost::static_visitor<LeftRight> {
  private:
    const CurvesLR& curves;

  public:
    explicit LeftRightVariant(const CurvesLR& curves_)
        : curves{curves_} {
    }

    LeftRight operator()(const EquationGradient<XY, LinComArrZ<XYEta>>& eq_grad) const {
        std::ostringstream err;
        err << "line " << eq_grad.equation << " in LeftRightVariant";
        throw std::runtime_error(err.str());
    }

    LeftRight operator()(const EquationGradient<XY, LinComMapZ<Sin<LinComArrZ<XY>>>>& eq_grad) const {
        // We only return the first one
        return curves.first.at(eq_grad.equation).at(0);
    }

    LeftRight operator()(const EquationGradient<XY, LinComMapZ<Cos<LinComArrZ<XY>>>>& eq_grad) const {
        return curves.second.at(eq_grad.equation).at(0);
    }
};

static std::vector<LeftRight> stable_left_right(const IntervalPolygon& polygon, const CurvesLR& curves) {

    std::vector<LeftRight> result;
    for (const auto& int_pair : polygon) {
        const auto left_right = boost::apply_visitor(LeftRightVariant{curves}, int_pair.equation);
        result.push_back(left_right);
        

//ridderikhoff aug 27,2019 didn't print anything
       //std::cout << left_right << "\n";

    }

    return result;
}

static std::pair<LeftRight, LeftRight> unstable_left_right(const IntervalLineSegment& line_seg, const CurvesLR& curves) {

    const auto eq0 = boost::apply_visitor(LeftRightVariant{curves}, line_seg.equation0);
    const auto eq1 = boost::apply_visitor(LeftRightVariant{curves}, line_seg.equation1);

//george aug 27,2019
//std::cout << eq0 << "\n";
//std::cout << eq1 << "\n";

    return {eq0, eq1};
}

// TODO replace these with more maps
static auto stable_equations_to_string(const IntervalPolygon& polygon, const std::array<LinComArrZ<XYEta>, 3>& inverse_perm_eta, const std::array<LinComArrZ<XYPi>, 3>& inverse_perm_pi) {

    std::vector<std::string> result;
    for (const auto& int_pair : polygon) {
        const auto p = boost::apply_visitor(RearrangeVariant{inverse_perm_eta, inverse_perm_pi}, int_pair.equation);
        result.push_back(p);
    }

    return result;
}

static std::pair<std::string, std::string> unstable_equations_to_string(const IntervalLineSegment& line_seg, const std::array<LinComArrZ<XYEta>, 3>& inverse_perm_eta, const std::array<LinComArrZ<XYPi>, 3>& inverse_perm_pi) {

    const auto eq0 = boost::apply_visitor(RearrangeVariant{inverse_perm_eta, inverse_perm_pi}, line_seg.equation0);
    const auto eq1 = boost::apply_visitor(RearrangeVariant{inverse_perm_eta, inverse_perm_pi}, line_seg.equation1);

    return {eq0, eq1};
}

static Vector2<Interval> point_to_vector(const PointQ& point) {

    const Interval x = Interval{point.x} * boost::math::constants::half_pi<Interval>();
    const Interval y = Interval{point.y} * boost::math::constants::half_pi<Interval>();

    return {x, y};
}

static IntervalLineSegment convert_to_interval(const RationalLineSegment& rat_line_seg) {

    const auto point0 = point_to_vector(rat_line_seg.point0);
    const auto point1 = point_to_vector(rat_line_seg.point1);

    const EquationGradient<XY, LinComArrZ<XYEta>> line0{rat_line_seg.line0};
    const EquationGradient<XY, LinComArrZ<XYEta>> line1{rat_line_seg.line1};

    return IntervalLineSegment{point0, line0, point1, line1};
}

static IntervalPolygon convert_to_interval(const RationalPolygon& rat_polygon) {

    IntervalPolygon int_polygon;
    for (const auto& rat_pair : rat_polygon) {
        const auto int_point = point_to_vector(rat_pair.point);
        const EquationGradient<XY, LinComArrZ<XYEta>> side_line{rat_pair.side_line};
        int_polygon.emplace_back(int_point, side_line);
    }

    return int_polygon;
}

// TODO give all of these more consistent names
// TODO also do the refinement as the curves are generated
// that should reduce the memory usage
static boost::optional<IntervalPolygon> calculate_final_polygon(const std::vector<CodeNumber>& code_numbers, const std::vector<XYZ>& code_angles, const CurvesLR& curves) {

    const auto rational_polygon = calculate_bounding_polygon(code_numbers, code_angles);

    if (!rational_polygon) {
        return boost::none;
    }

    auto interval_polygon = convert_to_interval(*rational_polygon);
 
 //george aug 26,2019 this starts with a bounding polygon
 //   print_region(interval_polygon);
 // std::cout << std::endl;

    // Refine using the sines first
    for (const auto& kv : curves.first) {
        //std::cout << kv.first << std::endl;

        const auto maybe = refine_polygon(interval_polygon, kv.first);

        if (!maybe) {
            return boost::none;
        }

        interval_polygon = *maybe;
        //george aug 26,2019 this refines using the all equations
        //print_region(interval_polygon);
        //std::cout << std::endl;
    }

    // Now the cosines
    for (const auto& kv : curves.second) {
        //std::cout << kv.first << std::endl;

        const auto maybe = refine_polygon(interval_polygon, kv.first);

        if (!maybe) {
            return boost::none;
        }

        interval_polygon = *maybe;
        //george aug 26,2019 this refines using the all equations
       // print_region(interval_polygon);
       // std::cout << std::endl;
    }

    return interval_polygon;//note george aug 26,2019 the last stuff is the mrr region
}

static boost::optional<IntervalLineSegment> calculate_final_line_segment(const std::vector<CodeNumber>& code_numbers, const std::vector<XYZ>& code_angles, const LinComArrZ<XYEta>& constraint, const CurvesLR& curves) {

    const auto rational_line_segment = calculate_bounding_line_segment(code_numbers, code_angles, constraint);

    if (!rational_line_segment) {
        return boost::none;
    }
    //std::cout << "bounding line"  << std::endl;

    //std::cout << rational_line_segment->point0 << "->" << rational_line_segment->point1 << std::endl;//XIU



    auto interval_line_segment = convert_to_interval(*rational_line_segment);
    //std::cout << interval_line_segment.point0 << "->" << interval_line_segment.point1 << std::endl;

    const EquationGradient<XY, LinComArrZ<XYEta>> constraint_grad{constraint};
//george uncomment aug 26,2019 this gives the final mrr region but not factored yet
    //print_region(interval_line_segment);

    for (const auto& kv : curves.first) {

    	//george uncomment aug 26,2019 this gives the final mrr region but not factored yet
        //std::cout << kv.first << std::endl;

        const auto maybe = refine_line_segment(interval_line_segment, kv.first, constraint_grad);

        if (!maybe) {
            return boost::none;
        }

        interval_line_segment = *maybe;

        //george uncomment aug 26,2019 this gives the final mrr region but not factored yet
       // print_region(interval_line_segment);
    }

    for (const auto& kv : curves.second) {

    	//george aug 26,2019 uncomment this gives the final mrr region but not factored yet
        //std::cout << kv.second << std::endl;

        const auto maybe = refine_line_segment(interval_line_segment, kv.first, constraint_grad);

        if (!maybe) {
            return boost::none;
        }

        interval_line_segment = *maybe;

        //george uncomment aug 26,2019 this gives the final mrr region but not factored yet
        //print_region(interval_line_segment);
    }
    /*std::cout << "after refinement"  << std::endl;
        std::cout << "point1"  << std::endl;

    std::cout << interval_line_segment.point0  << std::endl;
        std::cout << "point2"  << std::endl;

    std::cout << interval_line_segment.point1  << std::endl;
    */


    return interval_line_segment;
}

static void convex_counterexample_checker(const IntervalPolygon& polygon) {

    const auto size = polygon.size();
    for (const auto i : falgo::range(size)) {

        const auto& point = polygon.at(i).point;

        const auto prev_i = i == 0 ? size - 1 : i - 1;
        const auto& eq0 = polygon.at(prev_i).equation;
        const auto& eq1 = polygon.at(i).equation;

        const auto gradient0 = boost::apply_visitor(GradientVariant{point}, eq0);
        const auto gradient1 = boost::apply_visitor(GradientVariant{point}, eq1);

        // z component of the 3d cross product
        const Interval cross = gradient0[0] * gradient1[1] - gradient0[1] * gradient1[0];

        // Are parallel if the cross is zero

        const auto s = sign(cross);

        if (s == Sign::ZERO) {
            std::ostringstream oss{};
            oss << "possible non-convex counterexample found:" << std::endl
                << boost::apply_visitor(EquationPrinter{}, eq0) << std::endl
                << boost::apply_visitor(EquationPrinter{}, eq1) << std::endl
                << point << std::endl;
            throw std::runtime_error(oss.str());
        }
    }
}
//
static boost::optional<Stable> points_and_stuff_stable(const std::vector<CodeNumber>& code_numbers, const std::vector<XYZ>& code_angles, const CurvesLR& curves) {

    const auto polygon = calculate_final_polygon(code_numbers, code_angles, curves);

    if (!polygon) {
        return boost::none;
    }

    convex_counterexample_checker(*polygon);

    const auto all_points = calculate_all_points(*polygon);
    const auto perm = permute_angles(all_points);
    const auto rearranged_points = rearrange_points(all_points, perm);

    const auto inverse_perm = invert_permutation(perm);
    const auto inverse_perm_eta = falgo::transform(inverse_perm, xyz_to_xyeta);
    const auto inverse_perm_pi = falgo::transform(inverse_perm, xyz_to_xypi);

    const auto rearranged_equations = stable_equations_to_string(*polygon, inverse_perm_eta, inverse_perm_pi);
    //print the equations here
    /*for (const auto& equation: rearranged_equations){
        std::cout << equation << std::endl;
    }*/
    const auto left_rights = stable_left_right(*polygon, curves);

    const InitialAngles initial_angles{std::get<0>(inverse_perm), std::get<1>(inverse_perm)};

    return Stable{initial_angles, rearranged_points, rearranged_equations, left_rights};
}

static boost::optional<Unstable> points_and_stuff_unstable(const std::vector<CodeNumber>& code_numbers, const std::vector<XYZ>& code_angles, const LinComArrZ<XYEta>& constraint, const CurvesLR& curves) {

    const auto line_segment = calculate_final_line_segment(code_numbers, code_angles, constraint, curves);

    if (!line_segment) {
        return boost::none;
    }

    // TODO replace this with an array instead of vector and optimize that
    const auto all_points = calculate_all_points(*line_segment);
    const auto perm = permute_angles(all_points);
    const auto rearranged_points = rearrange_points(all_points, perm);

    const auto inverse_perm = invert_permutation(perm);
    const auto inverse_perm_eta = falgo::transform(inverse_perm, xyz_to_xyeta);
    const auto inverse_perm_pi = falgo::transform(inverse_perm, xyz_to_xypi);

    /*
    const auto rearranged_constraint = [&]() {
        auto rearranged = rearrange_enum_com(constraint, inverse_perm_eta);
        rearranged.divide_content();
        rearranged.divide_unit();
        return rearranged;
    }();
    */
    //for(const auto& point : rearranged_points){
        //std::cout<< point<< std::endl;
   // }
    const auto rearranged_equations = unstable_equations_to_string(*line_segment, inverse_perm_eta, inverse_perm_pi);

    const auto left_right = unstable_left_right(*line_segment, curves);

    const InitialAngles initial_angles{std::get<0>(inverse_perm), std::get<1>(inverse_perm)};

    return Unstable{initial_angles,
                    rearranged_points.at(0), rearranged_points.at(1),
                    rearranged_equations.first, rearranged_equations.second,
                    left_right.first, left_right.second};
}

// TODO make a calculate_equations function or something or other
boost::optional<Stable> calculate_stable(const CodeSequence& code_sequence, const CodeType code_type) {

    const auto code_numbers = code_sequence.numbers();
    const auto code_angles = code_sequence.angles(XYZ::X, XYZ::Y);

    const auto code_angles_eta = falgo::transform(code_angles, xyz_to_xyeta);
    const auto code_angles_pi = falgo::transform(code_angles, xyz_to_xypi);

    // Note: it is possible that we could calculate the bounding polygon first to
    // check if it is empty. This way, we can return an empty optional without
    // finding the unfolding

    const Unfolding unfold{code_numbers, code_angles};

    // Some of the generated equations are duplicates, so we put them into a std::set
    // first. Order matters, because I want the order on which the polygon is
    // reduced to be deterministic.
    CurvesLR curves{};
    // george jun11th 2021 to print the shooting vector, uncomment the lines with label_shooting_vector
    if (code_type == CodeType::OSO) {

        // the return type of equations is determined at runtime, so we can't template
        // this code
        const auto shooting_vector = shooting_vector_open(code_sequence, code_angles_pi);
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        curves = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second);

    } else if (code_type == CodeType::CS) {

        const auto shooting_vector = shooting_vector_closed(code_sequence, code_angles_eta);
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        curves = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second);

    } else if (code_type == CodeType::OSNO) {

        const auto shooting_vector = unfold.shooting_vector_general();
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        curves = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second);

    } else {
        throw std::runtime_error("unstable code type passed to stable case");
    }

    return points_and_stuff_stable(code_numbers, code_angles, curves);
}

boost::optional<Unstable> calculate_unstable(const CodeSequence& code_sequence, const CodeType code_type) {

    const auto code_numbers = code_sequence.numbers();
    const auto code_angles = code_sequence.angles(XYZ::X, XYZ::Y);

    const auto constraint = code_sequence.constraint(XYZ::X, XYZ::Y);

    const auto code_angles_eta = falgo::transform(code_angles, xyz_to_xyeta);
    const auto code_angles_pi = falgo::transform(code_angles, xyz_to_xypi);

    // Note: it is possible that we could calculate the bounding polygon first to
    // check if it is empty. This way, we can return an empty optional without
    // finding the unfolding

    const Unfolding unfold{code_numbers, code_angles};

    // Some of the generated equations are duplicates, so we put them into a std::set
    // first. Order matters, because I want the order on which the polygon is
    // reduced to be deterministic.
    CurvesLR curves{};
    // george jun11th 2021 to print the shooting vector, uncomment the lines with label_shooting_vector
    if (code_type == CodeType::CNS) {

        const auto shooting_vector = shooting_vector_closed(code_sequence, code_angles_eta);
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        curves = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second);

    } else if (code_type == CodeType::ONS) {

        const auto shooting_vector = unfold.shooting_vector_general();
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        curves = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second);

    } else {
        throw std::runtime_error("stable code type in unstable case");
    }


    return points_and_stuff_unstable(code_numbers, code_angles, constraint, curves);
}

boost::optional<Stable> calculate_stable(const CodeSequence& code_sequence, const CodeType code_type, const std::vector<LeftRight>& left_rights) {

    const auto code_numbers = code_sequence.numbers();
    const auto code_angles = code_sequence.angles(XYZ::X, XYZ::Y);

    const auto code_angles_eta = falgo::transform(code_angles, xyz_to_xyeta);
    const auto code_angles_pi = falgo::transform(code_angles, xyz_to_xypi);

    // Note: it is possible that we could calculate the bounding polygon first to
    // check if it is empty. This way, we can return an empty optional without
    // finding the unfolding

    const Unfolding unfold{code_numbers, code_angles};

    CurvesLR curves{};

    // george jun11th 2021 to print the shooting vector, uncomment the lines with label_shooting_vector
    if (code_type == CodeType::OSO) {

        const auto shooting_vector = shooting_vector_open(code_sequence, code_angles_pi);
        // Passing the left_rights is the only difference
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        curves = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second, left_rights);

    } else if (code_type == CodeType::CS) {

        const auto shooting_vector = shooting_vector_closed(code_sequence, code_angles_eta);
        // Passing the left_rights is the only difference
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        curves = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second, left_rights);

    } else if (code_type == CodeType::OSNO) {

        const auto shooting_vector = unfold.shooting_vector_general();
        // Passing the left_rights is the only difference
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        curves = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second, left_rights);

    } else {
        throw std::runtime_error("unstable code type passed to stable case");
    }
    auto result = points_and_stuff_stable(code_numbers, code_angles, curves);


    return result;
}

boost::optional<Unstable> calculate_unstable(const CodeSequence& code_sequence, const CodeType code_type, const std::vector<LeftRight>& left_rights) {

    const auto code_numbers = code_sequence.numbers();
    const auto code_angles = code_sequence.angles(XYZ::X, XYZ::Y);

    const auto constraint = code_sequence.constraint(XYZ::X, XYZ::Y);

    const auto code_angles_eta = falgo::transform(code_angles, xyz_to_xyeta);
    const auto code_angles_pi = falgo::transform(code_angles, xyz_to_xypi);

    // Note: it is possible that we could calculate the bounding polygon first to
    // check if it is empty. This way, we can return an empty optional without
    // finding the unfolding

    const Unfolding unfold{code_numbers, code_angles};

    // Some of the generated equations are duplicates, so we put them into a std::set
    // first. Order matters, because I want the order on which the polygon is
    // reduced to be deterministic.
    CurvesLR curves{};
    // george jun11th 2021 to print the shooting vector, uncomment the lines with label_shooting_vector
    if (code_type == CodeType::CNS) {

        const auto shooting_vector = shooting_vector_closed(code_sequence, code_angles_eta);
        // Passing the left_rights is the only difference
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        curves = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second, left_rights);

    } else if (code_type == CodeType::ONS) {

        const auto shooting_vector = unfold.shooting_vector_general();
        // Passing the left_rights is the only difference
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        curves = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second, left_rights);

    } else {
        throw std::runtime_error("stable code type in unstable case");
    }
    auto result = points_and_stuff_unstable(code_numbers, code_angles, constraint, curves);
    return result;
}

// TODO split the stable and unstable functions apart into two files
// Perhaps do that with the refinement/boundary stuff too?
