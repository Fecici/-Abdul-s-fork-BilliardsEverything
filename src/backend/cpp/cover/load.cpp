#include "cover/load.hpp"
#include "parse.hpp"

ClosedRectangleQ cover::load_square(const std::string& dir) {

    auto line = read_file(dir + "/square.txt");
    boost::trim(line);

    const auto coords = split(line, " ");

    if (coords.size() != 4) {
        std::ostringstream err{};
        err << "cover::load_square: expected 4 coordinates, got " << coords.size();
        throw std::runtime_error(err.str());
    }

    const Rational x_min{coords.at(0)};
    const Rational x_max{coords.at(1)};

    const Rational y_min{coords.at(2)};
    const Rational y_max{coords.at(3)};

    const ClosedRectangleQ square{{x_min, x_max}, {y_min, y_max}};

    if (!square.is_square()) {
        throw std::runtime_error("cover::load_square: not a square");
    }

    return square;
}

ClosedConvexPolygonQ cover::load_polygon(const std::string& dir) {

    auto polygon_str = read_file(dir + "/polygon.txt");
    boost::trim(polygon_str);

    std::vector<PointQ> vertices{};

    const auto lines = split(polygon_str, "\n");

    for (const auto& line : lines) {

        const auto coords = split(line, " ");

        if (coords.size() != 2) {
            std::ostringstream err{};
            err << "cover::load_polygon: expected 2 coordinates, got " << coords.size();
            throw std::runtime_error(err.str());
        }

        const Rational x{coords.at(0)};
        const Rational y{coords.at(1)};

        vertices.emplace_back(x, y);
    }

    return ClosedConvexPolygonQ{vertices};
}

std::vector<SinglePair> cover::load_singles(const std::string& dir) {

    // TODO rename to singles.txt
    auto infile = open_file_read(dir + "/stables.txt");

    std::vector<SinglePair> singles{};

    size_t i = 0;
    std::string line{};
    while (std::getline(infile, line)) {

        // Makes it easier to parse
        boost::algorithm::replace_first(line, ":", ",");

        auto comps = split(line, ",");

        if (comps.size() != 3) {
            std::ostringstream err{};
            err << "cover::load_singles: expected 3 components, got " << comps.size();
            throw std::runtime_error(err.str());
        }

        for (auto& str : comps) {
            boost::trim(str);
        }

        const auto index = boost::lexical_cast<size_t>(comps.at(0));
        const auto stable_sequence = parse_code_sequence(comps.at(1));
        const auto stable_angles = parse_initial_angles(comps.at(2));

        // The indices must be in order
        if (index != i) {
            std::ostringstream err{};
            err << "cover::load_singles: mismatched indices: expected " << i << ", got " << index;
            throw std::runtime_error(err.str());
        }

        const SharedPtr<CodePair> stable{stable_sequence, stable_angles};

        singles.emplace_back(stable);

        ++i;
    }

    return singles;
}

std::vector<TriplePair> cover::load_triples(const std::string& dir) {

    auto infile = open_file_read(dir + "/triples.txt");

    std::vector<TriplePair> triples{};

    size_t i = 0;
    std::string line{};
    while (std::getline(infile, line)) {

        // Makes it easier to parse
        boost::algorithm::replace_first(line, ":", ",");
        boost::algorithm::replace_all(line, ";", ",");

        auto comps = split(line, ",");

        if (comps.size() != 7) {
            std::ostringstream err{};
            err << "cover::load_triples: expected 7 components, got " << comps.size();
            throw std::runtime_error(err.str());
        }

        for (auto& str : comps) {
            boost::trim(str);
        }

        const auto index = boost::lexical_cast<size_t>(comps.at(0));

        const auto stable_neg_sequence = parse_code_sequence(comps.at(1));
        const auto stable_neg_angles = parse_initial_angles(comps.at(2));

        const auto unstable_sequence = parse_code_sequence(comps.at(3));
        const auto unstable_angles = parse_initial_angles(comps.at(4));

        const auto stable_pos_sequence = parse_code_sequence(comps.at(5));
        const auto stable_pos_angles = parse_initial_angles(comps.at(6));

        // The indices must be in order
        if (index != i) {
            std::ostringstream err{};
            err << "cover::load_triples: mismatched indices: expected " << i << ", got " << index;
            throw std::runtime_error(err.str());
        }

        const SharedPtr<CodePair> stable_neg{stable_neg_sequence, stable_neg_angles};
        const SharedPtr<CodePair> unstable{unstable_sequence, unstable_angles};
        const SharedPtr<CodePair> stable_pos{stable_pos_sequence, stable_pos_angles};

        triples.emplace_back(stable_neg, unstable, stable_pos);

        ++i;
    }

    return triples;
}

template <typename It>
class Iterator final {

  private:
    It current;
    It end;

  public:
    explicit Iterator(const It start, const It end_)
        : current{start}, end{end_} {}

    const typename std::iterator_traits<It>::value_type& next() {
        if (current == end) {
            throw std::runtime_error("Iterator::next: iterator out of range");
        } else {
            return *current++;
        }
    }
};

static cover::Cover parse_cover_recursive(Iterator<std::vector<std::string>::const_iterator>& iter,
                                          const std::vector<SinglePair>& singles, const std::vector<TriplePair>& triples) {

    const auto& token = iter.next();

    if (token == "E") {
        return cover::Empty{};
    } else if (token == "H") {
        // TODO delete this
        // For compatability with old covers
        return cover::Empty{};
    } else if (token == "S") {

        const auto& str = iter.next();
        const auto index = boost::lexical_cast<size_t>(str);

        return cover::Single{singles.at(index)};
    } else if (token == "T") {

        const auto& str = iter.next();
        const auto index = boost::lexical_cast<size_t>(str);

        return cover::Triple{triples.at(index)};
    }
    else if (token == "D") {

        auto cover0 = parse_cover_recursive(iter, singles, triples);
        auto cover1 = parse_cover_recursive(iter, singles, triples);
        auto cover2 = parse_cover_recursive(iter, singles, triples);
        auto cover3 = parse_cover_recursive(iter, singles, triples);

        return cover::Divide{std::move(cover0), std::move(cover1), std::move(cover2), std::move(cover3)};

    } else {
        throw std::runtime_error("parse_cover_recursive: unknown cover token: " + token);
    }
}

cover::Cover cover::load_cover(const std::string& dir, const std::vector<SinglePair>& singles, const std::vector<TriplePair>& triples) {

    auto str = read_file(dir + "/cover.txt");
    boost::trim(str);

    const auto vec = split(str, " ");

    Iterator<std::vector<std::string>::const_iterator> iter{std::cbegin(vec), std::cend(vec)};

    return parse_cover_recursive(iter, singles, triples);
}


uint32_t cover::load_digits(const std::string& dir) {

    // TODO change this to digits.txt
    auto str = read_file(dir + "/precision.txt");
    boost::trim(str);

    return boost::lexical_cast<uint32_t>(str);
}
