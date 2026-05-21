#include <boost/algorithm/string.hpp>
#include <boost/lexical_cast.hpp>

#include "parse.hpp"

// This file is intended for parsing strings for the outside world, eg. user input or files
// database/deserialize.cpp is intended for parsing things from the database

CodeSequence parse_code_sequence(const std::string& str) {

    const auto strs = split(str, " ");

    std::vector<CodeNumber> code_numbers{};
    for (const auto& num : strs) {
        const auto n = boost::lexical_cast<CodeNumber>(num);
        code_numbers.push_back(n);
    }

    return CodeSequence{code_numbers};
}

InitialAngles parse_initial_angles(const std::string& str) {
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
        throw std::runtime_error("Unable to parse initial angles " + str);
    }
}

static bool is_digit(const char ch) {
    return ('0' <= ch) && (ch <= '9');
}

static Coeff64 parse_number(const std::string& num) {
    if (num.empty()) {
        return 1;
    } else if (num == "-") {
        return -1;
    } else {
        // The input string should match this regex: D+ U +D+ U -D+, where D is 0-9
        // If it doesn't, it should throw an exception
        return boost::lexical_cast<Coeff64>(num);
    }
}

LinComArrZ<XY> parse_lin_com_arr_xy(const std::string& str) {

    LinComArrZ<XY> builder{};

    if (str == "0") {
        return builder;
    }

    size_t index = 0;
    size_t length = 0;
    for (size_t i = 0; i < str.length(); i += 1) {
        const char ch = str.at(i);
        if (ch == '-') {
            index = i;
            length = 1;
        } else if (ch == 'x') {
            const auto coeff = parse_number(str.substr(index, length));
            builder.add(coeff, XY::X);
            // not parsing number any more
            length = 0;

        } else if (ch == 'y') {
            const auto coeff = parse_number(str.substr(index, length));
            builder.add(coeff, XY::Y);
            // not parsing number any more
            length = 0;

        } else if (is_digit(ch)) {
            if (length == 0) {
                // we are not reading a number currently,
                // so restart
                index = i;
            }

            length += 1;

        } else if (ch == '+') {
            // the next character should be the start of an integer
            // 3x+y
            index = i + 1;
            length = 0;

        } else {
            std::ostringstream err;
            err << "unrecognized character '" << ch << "' in \"" << str << "\" at position " << i;
            throw std::runtime_error(err.str());
        }
    }

    return builder;
}

// TODO there has to be a nice way than this
LinComArrZ<XYPi> parse_lin_com_arr_xypi(const std::string& str) {

    LinComArrZ<XYPi> builder{};

    if (str == "0") {
        return builder;
    }

    size_t index = 0;
    size_t length = 0;
    for (size_t i = 0; i < str.length(); i += 1) {
        const char ch = str.at(i);
        if (ch == '-') {
            index = i;
            length = 1;
        } else if (ch == 'x') {
            const auto coeff = parse_number(str.substr(index, length));
            builder.add(coeff, XYPi::X);
            // not parsing number any more
            length = 0;

        } else if (ch == 'y') {
            const auto coeff = parse_number(str.substr(index, length));
            builder.add(coeff, XYPi::Y);
            // not parsing number any more
            length = 0;

        } else if (ch == 'p') {
            const char next_char = str.at(i + 1);
            if (next_char == 'i') {
                const auto coeff = parse_number(str.substr(index, length));
                builder.add(coeff, XYPi::Pi);
                length = 0;
                // skip over the 'i'
                i += 1;
            } else {
                std::ostringstream err;
                err << "unrecognized character '" << ch << "' in \"" << str << "\" at position " << i;
                throw std::runtime_error(err.str());
            }
        } else if (is_digit(ch)) {
            if (length == 0) {
                // we are not reading a number currently,
                // so restart
                index = i;
            }

            length += 1;

        } else if (ch == '+') {
            // the next character should be the start of an integer
            // 3x+y
            index = i + 1;
            length = 0;

        } else {
            std::ostringstream err;
            err << "unrecognized character '" << ch << "' in \"" << str << "\" at position " << i;
            throw std::runtime_error(err.str());
        }
    }

    return builder;
}

LinComArrZ<XYEta> parse_lin_com_arr_xyeta(const std::string& str) {

    LinComArrZ<XYEta> builder{};

    if (str == "0") {
        return builder;
    }

    size_t index = 0;
    size_t length = 0;
    for (size_t i = 0; i < str.length(); i += 1) {
        const char ch = str.at(i);
        if (ch == '-') {
            index = i;
            length = 1;
        } else if (ch == 'x') {
            const auto coeff = parse_number(str.substr(index, length));
            builder.add(coeff, XYEta::X);
            // not parsing number any more
            length = 0;

        } else if (ch == 'y') {
            const auto coeff = parse_number(str.substr(index, length));
            builder.add(coeff, XYEta::Y);
            // not parsing number any more
            length = 0;

        } else if (ch == 'e') {
            const char next_char = str.at(i + 1);
            const char next_next_char = str.at(i + 2);
            if ((next_char == 't') && (next_next_char == 'a')) {
                const auto coeff = parse_number(str.substr(index, length));
                builder.add(coeff, XYEta::Eta);
                length = 0;
                // skip over the "ta"
                i += 2;
            } else {
                std::ostringstream err;
                err << "unrecognized characters \"" << next_char << next_next_char << "\" in \"" << str << "\" at position " << i;
                throw std::runtime_error(err.str());
            }
        } else if (is_digit(ch)) {
            if (length == 0) {
                // we are not reading a number currently,
                // so restart
                index = i;
            }

            length += 1;

        } else if (ch == '+') {
            // the next character should be the start of an integer
            // 3x+y
            index = i + 1;
            length = 0;

        } else {
            std::ostringstream err;
            err << "unrecognized character '" << ch << "' in \"" << str << "\" at position " << i;
            throw std::runtime_error(err.str());
        }
    }

    return builder;
}

template <template <typename> class Trig>
LinComMapZ<Trig<LinComArrZ<XY>>> parse_lin_com_map_xy(const std::string& str, const std::string& trig) {

    LinComMapZ<Trig<LinComArrZ<XY>>> builder{};

    if (str == "0") {
        return builder;
    }

    size_t index = 0;
    size_t length = 0;
    for (size_t i = 0; i < str.length(); i += 1) {
        const char ch = str.at(i);
        if (ch == '-') {
            index = i;
            length = 1;
        } else if (ch == trig.at(0)) {
            const auto coeff = parse_number(str.substr(index, length));

            // here is where the magic happens
            if (str.substr(i, 4) != (trig + '(')) {
                std::ostringstream err;
                err << "\"" << str.substr(i, 4) << "\" != \"" << trig << "(\" in \"" << str << "\" at position " << i;
                throw std::runtime_error(err.str());
            }

            const size_t ind = str.find(')', i);

            const auto arg = parse_lin_com_arr_xy(str.substr(i + 4, ind - i - 4));

            builder.add(coeff, Trig<LinComArrZ<XY>>{arg});
            // now skip past the sin
            i = ind;
            // not parsing number any more
            length = 0;

        } else if (is_digit(ch)) {
            if (length == 0) {
                // we are not reading a number currently,
                // so restart
                index = i;
            }

            length += 1;

        } else if (ch == '+') {
            // the next character should be the start of an integer
            // 3x+y
            index = i + 1;
            length = 0;

        } else {
            std::ostringstream err;
            err << "unrecognized character '" << ch << "' in \"" << str << "\" at position " << i;
            throw std::runtime_error(err.str());
        }
    }

    return builder;
}

LinComMapZ<Sin<LinComArrZ<XY>>> parse_lin_com_map_sin_xy(const std::string& str) {
    return parse_lin_com_map_xy<Sin>(str, "sin");
}

LinComMapZ<Cos<LinComArrZ<XY>>> parse_lin_com_map_cos_xy(const std::string& str) {
    return parse_lin_com_map_xy<Cos>(str, "cos");
}
