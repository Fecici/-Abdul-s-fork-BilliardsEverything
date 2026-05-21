#include <boost/math/constants/constants.hpp>

#include "reorder.hpp"
#include "trig_identities.hpp"

// For performance reasons, it is sometimes necessary to hold on to a reference
// to an external object inside a class instead of copying it over. In cases like
// this, the class that you create should be made as a temporary, to prevent
// potential lifetime issues. Clases that need this behaviour are essentially
// "temporary" classes. They only exist to help some other function, like the
// boost static visitors, so they are only around for one line anyway.

static std::array<Interval, 3> all_three_angles(const Vector2<Interval>& point) {

    const auto& x = point[0];
    const auto& y = point[1];
    // z = pi - x - y
    const Interval z = boost::math::constants::pi<Interval>() - x - y;

    return {{x, y, z}};
}

std::vector<std::array<Interval, 3>> calculate_all_points(const IntervalPolygon& polygon) {

    std::vector<std::array<Interval, 3>> vec;
    // Perfect case for a map here
    for (const auto& rat_pair : polygon) {
        const auto angles = all_three_angles(rat_pair.point);
        vec.push_back(angles);
    }

    return vec;
}

// TODO make this an array, not a vector
std::vector<std::array<Interval, 3>> calculate_all_points(const IntervalLineSegment& line_seg) {

    const auto angles0 = all_three_angles(line_seg.point0);
    const auto angles1 = all_three_angles(line_seg.point1);

    return {angles0, angles1};
}

#if 0
// TODO convert this over
bool same_orientation(const std::array<XYZ, 3>& angles) {
    // This is the order of the permutations (lexicographic)
    // (X, Y, Z)
    // (X, Z, Y)
    // (Y, X, Z)
    // (Y, Z, X)
    // (Z, X, Y)
    // (Z, Y, X)

    if (angles == std::make_tuple(XYZ::X, XYZ::Y, XYZ::Z)) {
        return true;
    } else if (angles == std::make_tuple(XYZ::X, XYZ::Z, XYZ::Y)) {
        return false;
    } else if (angles == std::make_tuple(XYZ::Y, XYZ::X, XYZ::Z)) {
        return false;
    } else if (angles == std::make_tuple(XYZ::Y, XYZ::Z, XYZ::X)) {
        return true;
    } else if (angles == std::make_tuple(XYZ::Z, XYZ::X, XYZ::Y)) {
        return true;
    } else if (angles == std::make_tuple(XYZ::Z, XYZ::Y, XYZ::X)) {
        return false;
    } else {
        throw std::runtime_error("invalid angles in same_orientation");
    }
}
#endif

// So we have the bounding region in terms of the all points, and that
// lets us sort the angles into some order, eg ZXY.
// Now, we want to relabel the angles in our equations according to this
// order. So, if in our equations we have variables x, y, z we have
// z -> x', x -> y', y -> z' (where -> indicates substitution)
// To do the substitution, we then need to resort the variables
// x -> y', y -> z', z -> x'
// Then, we can drop the first variables to give us the new ones
// y', z', x', or YZX

std::array<XYZ, 3> permute_angles(const std::vector<std::array<Interval, 3>>& points) {

    const auto min_x = min<0>(points);
    const auto min_y = min<1>(points);
    const auto min_z = min<2>(points);

    const auto max_x = max<0>(points);
    const auto max_y = max<1>(points);
    const auto max_z = max<2>(points);

    std::array<std::tuple<XYZ, Interval, Interval>, 3> tuples{{std::make_tuple(XYZ::X, min_x, max_x),
                                                               std::make_tuple(XYZ::Y, min_y, max_y),
                                                               std::make_tuple(XYZ::Z, min_z, max_z)}};

    std::sort(std::begin(tuples), std::end(tuples), [](const auto& first, const auto& second) {
        // Do a lexicographical comparison in the order (2, 1)
        const auto ord2 = compare_interval(std::get<2>(first), std::get<2>(second));

        if (ord2 == Order::Equal) {
            const auto ord1 = compare_interval(std::get<1>(first), std::get<1>(second));
            return ord1 == Order::Less;
        }

        return ord2 == Order::Less;
    });

    return {{std::get<0>(std::get<0>(tuples)),
             std::get<0>(std::get<1>(tuples)),
             std::get<0>(std::get<2>(tuples))}};
}

std::vector<Vector2<Interval>> rearrange_points(const std::vector<std::array<Interval, 3>>& all_points, const std::array<XYZ, 3>& permutation) {

    // the first two angles become the new x and y
    const auto x_angle = std::get<0>(permutation);
    const auto y_angle = std::get<1>(permutation);

    const auto x_index = static_cast<size_t>(x_angle);
    const auto y_index = static_cast<size_t>(y_angle);

    std::vector<Vector2<Interval>> new_points;

    for (const auto& triple : all_points) {
        const auto& x = triple.at(x_index);
        const auto& y = triple.at(y_index);

        new_points.emplace_back(x, y);
    }

    return new_points;
}

std::array<XYZ, 3> invert_permutation(const std::array<XYZ, 3>& perm) {

    std::array<std::pair<XYZ, XYZ>, 3> sigma{{{XYZ::X, std::get<0>(perm)},
                                              {XYZ::Y, std::get<1>(perm)},
                                              {XYZ::Z, std::get<2>(perm)}}};

    std::sort(std::begin(sigma), std::end(sigma), [](const auto& first, const auto& second) {
        return first.second < second.second;
    });

    return {{std::get<0>(sigma).first,
             std::get<1>(sigma).first,
             std::get<2>(sigma).first}};
}

// TODO rename these and the variables
// the first three things correspond to A, B, C
LinComArrZ<XYPi> rearrange_enum_com(const LinComArrZ<XY>& enum_com, const std::array<LinComArrZ<XYPi>, 3>& perm) {

    const auto a_coeff = enum_com.coeff<XY::X>();
    const auto b_coeff = enum_com.coeff<XY::Y>();

    const auto a_angle = std::get<0>(perm);
    const auto b_angle = std::get<1>(perm);

    LinComArrZ<XYPi> builder{};
    builder.add(a_coeff, a_angle);
    builder.add(b_coeff, b_angle);

    return builder;
}

LinComArrZ<XYEta> rearrange_enum_com(const LinComArrZ<XYEta>& enum_com, const std::array<LinComArrZ<XYEta>, 3>& perm) {

    const auto a_coeff = enum_com.coeff<XYEta::X>();
    const auto b_coeff = enum_com.coeff<XYEta::Y>();
    const auto eta_coeff = enum_com.coeff<XYEta::Eta>();

    const auto a_angle = std::get<0>(perm);
    const auto b_angle = std::get<1>(perm);

    LinComArrZ<XYEta> builder{};
    builder.add(a_coeff, a_angle);
    builder.add(b_coeff, b_angle);
    builder.add(eta_coeff, XYEta::Eta);

    return builder;
}

// TODO a bit of code duplication here, but I'm not sure how to get rid of it
// The solution is to make the simplifies templated. Can do that later
LinComMapZ<Sin<LinComArrZ<XY>>> rearrange_lin_com(const LinComMapZ<Sin<LinComArrZ<XY>>>& lin_com, const std::array<LinComArrZ<XYPi>, 3>& perm) {

    LinComMapZ<Sin<LinComArrZ<XY>>> builder{};

    for (const auto& kv : lin_com) {
        const auto& enum_com_ab = kv.first.arg;
        const auto coeff = kv.second;

        const auto enum_com_xypi = rearrange_enum_com(enum_com_ab, perm);

        const auto sin = simplify_sin_xypi(enum_com_xypi);

        builder.add(coeff * sin.first, sin.second);
    }

    return builder;
}

LinComMapZ<Cos<LinComArrZ<XY>>> rearrange_lin_com(const LinComMapZ<Cos<LinComArrZ<XY>>>& lin_com, const std::array<LinComArrZ<XYPi>, 3>& perm) {

    LinComMapZ<Cos<LinComArrZ<XY>>> builder{};

    for (const auto& kv : lin_com) {
        const auto& enum_com_ab = kv.first.arg;
        const auto coeff = kv.second;

        const auto enum_com_xypi = rearrange_enum_com(enum_com_ab, perm);

        const auto cos = simplify_cos_xypi(enum_com_xypi);

        builder.add(coeff * cos.first, cos.second);
    }

    return builder;
}
