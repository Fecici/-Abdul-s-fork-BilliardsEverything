#include "search.hpp"
#include "code_sequence.hpp"
#include "database/serialize.hpp"
#include "parse.hpp"

// All the even-odd strings we are printing have the same length.
// Also, lexicographically 0 comes before 1, and E comes before O,
// so this will be the same order as if we had used that
static std::string find_even_odd(const CodeSequence& code_seq) {

    std::string eo{};
    for (const auto& num : code_seq) {
        if (num % 2 == 0) {
            eo.push_back('E');
        } else {
            eo.push_back('O');
        }
    }

    return eo;
}

static bool check_even_odd(const CodeSequence& code_seq, const std::string& even_odd) {

    const auto& numbers = code_seq.numbers();

    if (numbers.size() != even_odd.size()) {
        return false;
    }

    for (size_t i = 0; i < numbers.size(); ++i) {

        const auto num = numbers.at(i);
        const auto eo = even_odd.at(i);

        const char ch = (num % 2 == 0) ? 'E' : 'O';

        if (ch != eo) {
            return false;
        }
    }

    return true;
}

std::string code_search(const CodeType code_type, const size_t length, sqlite::Database& db) {

    const std::string sql = "select code_sequence from " + database::serialize(code_type) + ";";

    auto stmnt = db.prepare(sql).bind();

    std::map<std::string, std::set<CodeSequence>> eo_map{};

    std::string code_sequence_str{};
    while (stmnt.step(code_sequence_str)) {

        const auto code_seq = parse_code_sequence(code_sequence_str);

        if (code_seq.length() == length) {

            const auto eo = find_even_odd(code_seq);

            eo_map[eo].insert(code_seq);
        }
    }

    std::ostringstream oss{};
    for (const auto& kv : eo_map) {
        oss << "// " << kv.first << '\n';

        for (const auto& code_seq : kv.second) {
            oss << code_seq << '\n';
        }

        oss << '\n';
    }

    return oss.str();
}

std::string code_search(const CodeType code_type, const std::string& even_odd, sqlite::Database& db) {

    const std::string sql = "select code_sequence from " + database::serialize(code_type) + ";";

    auto stmnt = db.prepare(sql).bind();

    std::set<CodeSequence> code_seq_set{};

    std::string code_sequence_str{};
    while (stmnt.step(code_sequence_str)) {

        const auto code_seq = parse_code_sequence(code_sequence_str);

        if (check_even_odd(code_seq, even_odd)) {
            code_seq_set.insert(code_seq);
        }
    }

    std::ostringstream oss{};

    if (!code_seq_set.empty()) {

        oss << "// " << even_odd << '\n';

        for (const auto& code_seq : code_seq_set) {
            oss << code_seq << '\n';
        }
    }

    return oss.str();
}
