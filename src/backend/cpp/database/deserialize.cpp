#include "database/deserialize.hpp"

template <>
InitialAngles database::deserialize(const std::string& str) {

    if (str == "xy") {
        return InitialAngles{XYZ::X, XYZ::Y};
    } else if (str == "xz") {
        return InitialAngles{XYZ::X, XYZ::Z};
    } else if (str == "yx") {
        return InitialAngles{XYZ::Y, XYZ::X};
    } else if (str == "yz") {
        return InitialAngles{XYZ::Y, XYZ::Z};
    } else if (str == "zx") {
        return InitialAngles{XYZ::Z, XYZ::X};
    } else if (str == "zy") {
        return InitialAngles{XYZ::Z, XYZ::Y};
    } else {
        throw std::runtime_error("database::deserialize: unable to parse initial angles " + str);
    }
}

template <>
PointQ database::deserialize(const std::string& point_str) {

    const auto coords = split(point_str, " ");

    const Rational x{coords.at(0)};
    const Rational y{coords.at(1)};

    return {x, y};
}

template <>
std::vector<PointQ> database::deserialize(const std::string& polygon_str) {

    std::vector<PointQ> vertices{};

    const auto lines = split(polygon_str, "\n");

    for (const auto& line : lines) {

        const auto point = deserialize<PointQ>(line);

        vertices.push_back(point);
    }

    return vertices;
}

template <template <typename> class T>
LinComMapZ<T<LinComArrZ<XY>>> parse_equation(const std::string& coeffs_str) {

    const auto coeffs = split(coeffs_str, " ");

    if (coeffs.size() % 3 != 0) {
        std::ostringstream err{};
        err << "coeffs are not a multiple of 3\n"
            << coeffs;
        throw std::runtime_error(err.str());
    }

    LinComMapZ<T<LinComArrZ<XY>>> sum{};

    for (size_t i = 0; i < coeffs.size(); i += 3) {
        const auto trig_coeff = boost::lexical_cast<Coeff64>(coeffs.at(i));
        const auto x_coeff = boost::lexical_cast<Coeff64>(coeffs.at(i + 1));
        const auto y_coeff = boost::lexical_cast<Coeff64>(coeffs.at(i + 2));

        const LinComArrZ<XY> arg{x_coeff, y_coeff};
        const T<LinComArrZ<XY>> trig{arg};

        sum.add(trig_coeff, trig);
    }

    return sum;
}

template <>
Equation<Sin> database::deserialize(const std::string& coeffs_str) {
    return parse_equation<Sin>(coeffs_str);
}

template <>
Equation<Cos> database::deserialize(const std::string& coeffs_str) {
    return parse_equation<Cos>(coeffs_str);
}

template <template <typename> class T>
std::set<Equation<T>> parse_equations(const std::string& equations_str) {

    std::set<Equation<T>> equations{};

    const auto lines = split(equations_str, "\n");

    for (const auto& line : lines) {
        const auto equation = parse_equation<T>(line);
        equations.insert(equation);
    }

    return equations;
}

template <>
std::set<Equation<Sin>> database::deserialize(const std::string& equations_str) {
    return parse_equations<Sin>(equations_str);
}

template <>
std::set<Equation<Cos>> database::deserialize(const std::string& equations_str) {
    return parse_equations<Cos>(equations_str);
}
