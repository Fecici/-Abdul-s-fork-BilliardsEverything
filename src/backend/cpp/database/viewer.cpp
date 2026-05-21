#include "database/viewer.hpp"
#include "database/serialize.hpp"

template <typename T>
std::string convert_points(const T& points) {

    std::ostringstream oss{};
    constexpr auto prec = std::numeric_limits<float64_t>::max_digits10;
    oss << std::scientific << std::setprecision(prec);

    bool first = true;
    for (const auto& point : points) {
        const auto x = static_cast<float64_t>(boost::multiprecision::median(point[0]));
        const auto y = static_cast<float64_t>(boost::multiprecision::median(point[1]));

        if (!first) {
            oss << '\n';
        }

        oss << x << ' ' << y;

        first = false;
    }

    return oss.str();
}

template <typename T>
std::string convert_equations(const T& equations) {

    std::string concat{};

    bool first = true;
    for (const auto& equation : equations) {

        if (!first) {
            concat.push_back('\n');
        }

        concat.append(equation);

        first = false;
    }

    return concat;
}

template <typename T>
std::string convert_left_rights(const T& left_rights) {
    std::ostringstream oss{};

    bool first = true;
    for (const auto& left_right : left_rights) {
        if (!first) {
            oss << '\n';
        }

        oss << left_right.left.number << ' ' << left_right.left.branch << ' '
            << left_right.right.number << ' ' << left_right.right.branch;

        first = false;
    }

    return oss.str();
}

bool database::in(const CodeSequence& code_seq, const CodeType& code_type, sqlite::Database& db) {

    const std::string sql = "select exists(select 1 from " + database::serialize(code_type) + " where code_sequence = ?);";

    int64_t exists = 0;
    db.prepare(sql).bind(database::serialize(code_seq)).exec(exists);

    return exists;
}

// works for stables and unstables
template <typename T>
void save_impl(const CodeSequence& code_seq, const CodeType& code_type, const T& info, sqlite::Database& db) {

    // Zhao Yu Li, Jun 26, 2025.
    // Added "on conflict do nothing". This clause handles the case where more than one threads save the exact same thing concurrently.
    // Jul 3, 2025.
    // Replace "on conflict do nothing" with "or ignore" for backward compatibility.
    const std::string sql = "insert or ignore into " + database::serialize(code_type) + "(code_sequence, initial_angles, points, equations, left_rights, lr_code_sequence, polygon, sin_equations, cos_equations) values (?, ?, ?, ?, ?, ?, ?, ?, ?);";

    db.prepare(sql)
        .bind(
            database::serialize(code_seq),
            database::serialize(info.initial_angles),
            convert_points(info.points),
            convert_equations(info.equations),
            convert_left_rights(info.left_rights),
            "",
            "",
            "",
            "")
        .exec();
}

void database::delete_from_db(const CodeSequence& code_seq, const CodeType& code_type, sqlite::Database& db) {
    const std::string sql = "delete from " + database::serialize(code_type) + " where code_sequence = ?;";

    db.prepare(sql)
        .bind(database::serialize(code_seq))
        .exec();
}

void database::save(const CodeSequence& code_seq, const CodeType& code_type, const Stable& info, sqlite::Database& db) {
    save_impl(code_seq, code_type, info, db);
}

void database::save(const CodeSequence& code_seq, const CodeType& code_type, const Unstable& info, sqlite::Database& db) {
    save_impl(code_seq, code_type, info, db);
}

// works for stables and unstables
template <typename T>
void save_impl(const CodeSequence& base_code_seq, const CodeSequence& code_seq, const CodeType& code_type, const T& info, sqlite::Database& db) {

    const std::string sql = "insert into " + database::serialize(code_type) + "(code_sequence, initial_angles, points, equations, left_rights, lr_code_sequence, polygon, sin_equations, cos_equations) values (?, ?, ?, ?, ?, ?, ?, ?, ?);";

    db.prepare(sql)
        .bind(
            database::serialize(code_seq),
            database::serialize(info.initial_angles),
            convert_points(info.points),
            convert_equations(info.equations),
            convert_left_rights(info.left_rights),
            database::serialize(base_code_seq),
            "",
            "",
            "")
        .exec();
}

void database::save(const CodeSequence& base_code_seq, const CodeSequence& code_seq, const CodeType& code_type, const Stable& info, sqlite::Database& db) {
    save_impl(base_code_seq, code_seq, code_type, info, db);
}

void database::save(const CodeSequence& base_code_seq, const CodeSequence& code_seq, const CodeType& code_type, const Unstable& info, sqlite::Database& db) {
    save_impl(base_code_seq, code_seq, code_type, info, db);
}

Picture database::load_picture(const CodeSequence& code_seq, const CodeType& code_type, sqlite::Database& db) {

    const std::string sql = "select initial_angles, points, equations from " + database::serialize(code_type) + " where code_sequence = ?;";

    std::string initial_angles{};
    std::string points{};
    std::string equations{};
    db.prepare(sql).bind(database::serialize(code_seq)).exec(initial_angles, points, equations);

    return Picture{initial_angles, points, equations};
}

Info database::load_info(const CodeSequence& code_seq, const CodeType& code_type, sqlite::Database& db) {

    const std::string sql = "select initial_angles, points, equations, left_rights, lr_code_sequence from " + database::serialize(code_type) + " where code_sequence = ?;";

    std::string initial_angles{};
    std::string points{};
    std::string equations{};
    std::string left_rights{};
    std::string code_seq_lr{};
    db.prepare(sql).bind(database::serialize(code_seq)).exec(initial_angles, points, equations, left_rights, code_seq_lr);

    return Info{initial_angles, points, equations, left_rights, code_seq_lr};
}

static std::vector<LeftRight> parse_left_rights(const std::string& str) {

    std::vector<LeftRight> left_rights{};

    const auto lines = split(str, "\n");

    for (const auto& line : lines) {
        const auto nums = split(line, " ");

        if (nums.size() != 4) {
            throw std::runtime_error("incorrect nums size in parse_left_rights");
        }

        const auto left_number = boost::lexical_cast<size_t>(nums.at(0));
        const auto left_branch = boost::lexical_cast<size_t>(nums.at(1));

        const auto right_number = boost::lexical_cast<size_t>(nums.at(2));
        const auto right_branch = boost::lexical_cast<size_t>(nums.at(3));

        left_rights.emplace_back(Vertex{left_number, left_branch}, Vertex{right_number, right_branch});
    }

    return left_rights;
}

std::vector<LeftRight> database::load_left_rights(const CodeSequence& code_seq, const CodeType& code_type, sqlite::Database& db) {

    const std::string sql = "select left_rights from " + database::serialize(code_type) + " where code_sequence = ?;";

    std::string left_rights{};
    db.prepare(sql).bind(database::serialize(code_seq)).exec(left_rights);

    return parse_left_rights(left_rights);
}
