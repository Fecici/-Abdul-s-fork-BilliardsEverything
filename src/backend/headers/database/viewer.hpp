#pragma once

#include "../code_sequence.hpp"
#include "../sqlite.hpp"

struct Picture {
    std::string initial_angles;
    std::string points;
    std::string equations;

    explicit Picture(const std::string& initial_angles_, const std::string& points_, const std::string& equations_)
        : initial_angles{initial_angles_}, points{points_}, equations{equations_} {}
};

struct Info {
    std::string initial_angles;
    std::string points;
    std::string equations;
    std::string left_rights;
    std::string code_seq_lr;

    explicit Info(const std::string& initial_angles_, const std::string& points_, const std::string& equations_,
                  const std::string& left_rights_, const std::string& code_seq_lr_)
        : initial_angles{initial_angles_}, points{points_}, equations{equations_}, left_rights{left_rights_},
          code_seq_lr{code_seq_lr_} {}
};

// TODO reorganize these classes nicer
class StableRef final {
  public:
    LeftRight left_right;
    size_t index;

    explicit StableRef(const LeftRight& left_right_, const size_t index_)
        : left_right{left_right_},
          index{index_} {
    }

    friend bool operator==(const StableRef& lhs, const StableRef& rhs) {
        return lhs.left_right == rhs.left_right;
    }

    friend bool operator<(const StableRef& lhs, const StableRef& rhs) {
        return lhs.left_right < rhs.left_right;
    }
};

// Look at all rotations and reflections
class Stable final {
  public:
    InitialAngles initial_angles;
    std::vector<Vector2<Interval>> points;
    std::vector<std::string> equations;
    std::vector<LeftRight> left_rights;

    explicit Stable(const InitialAngles& initial_angles_,
                    const std::vector<Vector2<Interval>>& points_,
                    const std::vector<std::string>& equations_,
                    const std::vector<LeftRight>& left_rights_)
        : initial_angles{initial_angles_} {

        // TODO there is a better way of ordering these equations. Do it that way
        std::vector<StableRef> stable_refs;
        for (const auto i : falgo::range(left_rights_.size())) {
            stable_refs.emplace_back(left_rights_.at(i), i);
        }

        falgo::min_rotation(stable_refs);

        // If a map doesn't quite cut it, you can always use a for_each
        const auto for_each = [&](const auto& stable_ref) {
            const auto i = stable_ref.index;
            left_rights.push_back(left_rights_.at(i));
            points.push_back(points_.at(i));
            equations.push_back(equations_.at(i));
        };

        falgo::for_each(stable_refs, for_each);
    }

    friend std::ostream & operator << (std::ostream& os, const Stable& stable) {
        os << "Stable: " << std::endl;
        os << "    " << "Initial angles: " << std::endl << stable.initial_angles << std::endl;
        os << "    " << "Points: " << std::endl;

        // trying to format points
        constexpr auto prec = std::numeric_limits<float64_t>::max_digits10;
        os << std::scientific << std::setprecision(prec);
        for (const auto& point : stable.points) {
            const auto x = static_cast<float64_t>(boost::multiprecision::median(point[0]));
            const auto y = static_cast<float64_t>(boost::multiprecision::median(point[1]));
            os << x << ' ' << y << std::endl;
        }

        os <<"    " << "Equations: " << std::endl;
        for (const auto& equation : stable.equations)
        {
            std::cout << equation << std::endl;
        }

        os << "    " << "Left Rights: " << stable.left_rights << std::endl;
        return os;
    }
};

// Arrays of length 2 are the natural analogue of a vector
class Unstable final {
  public:
    InitialAngles initial_angles;
    std::array<Vector2<Interval>, 2> points;
    std::array<std::string, 2> equations;
    std::array<LeftRight, 2> left_rights;

    explicit Unstable(const InitialAngles& initial_angles_,
                      const Vector2<Interval>& point0, const Vector2<Interval>& point1,
                      const std::string& equation0, const std::string& equation1,
                      const LeftRight& left_right0, const LeftRight& left_right1)
        : initial_angles{initial_angles_},
          // A LeftRight has no default constructor, so we have to do this (silly)
          left_rights{{left_right0, left_right1}} {

        if (left_right0 < left_right1) {
            points = {{point0, point1}};
            equations = {{equation0, equation1}};
            left_rights = {{left_right0, left_right1}};
        } else if (left_right1 < left_right0) {
            points = {{point1, point0}};
            equations = {{equation1, equation0}};
            left_rights = {{left_right1, left_right0}};
        } else {
            // Equal left rights almost certainly indicates a bug
            throw std::runtime_error("equal left rights in Unstable");
        }
    }

    friend std::ostream& operator<<(std::ostream& os, const Unstable& unstable) {
        os << "Unstable: " << std::endl;
        os << "    " << "Initial angles: " << std::endl << unstable.initial_angles << std::endl;
        os << "    " << "Points: " << std::endl;
        // trying to format points
        constexpr auto prec = std::numeric_limits<float64_t>::max_digits10;
        os << std::scientific << std::setprecision(prec);
        for (const auto& point : unstable.points) {
            const auto x = static_cast<float64_t>(boost::multiprecision::median(point[0]));
            const auto y = static_cast<float64_t>(boost::multiprecision::median(point[1]));
            os << x << ' ' << y << std::endl;
        }

        os <<"    " << "Equations: " << std::endl;
        for (const auto& equation : unstable.equations)
        {
            std::cout << equation << std::endl;
        }

        os << "    " << "Left Rights: " << unstable.left_rights << std::endl;
        return os;
    }
};

namespace database {

bool in(const CodeSequence& code_seq, const CodeType& code_type, sqlite::Database& db);

void save(const CodeSequence& code_seq, const CodeType& code_type, const Stable& info, sqlite::Database& db);

void save(const CodeSequence& code_seq, const CodeType& code_type, const Unstable& info, sqlite::Database& db);

void save(const CodeSequence& base_code_seq, const CodeSequence& code_seq, const CodeType& code_type, const Stable& info, sqlite::Database& db);

void save(const CodeSequence& base_code_seq, const CodeSequence& code_seq, const CodeType& code_type, const Unstable& info, sqlite::Database& db);

void delete_from_db(const CodeSequence& code_seq, const CodeType& code_type, sqlite::Database& db);

Picture load_picture(const CodeSequence& code_seq, const CodeType& code_type, sqlite::Database& db);

Info load_info(const CodeSequence& code_seq, const CodeType& code_type, sqlite::Database& db);

std::vector<LeftRight> load_left_rights(const CodeSequence& code_seq, const CodeType& code_type, sqlite::Database& db);
}
