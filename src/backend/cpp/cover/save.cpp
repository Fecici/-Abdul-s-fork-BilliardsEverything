#include "cover/save.hpp"

void cover::save_polygon(const std::string& dir, const ClosedConvexPolygonQ& polygon) {
    auto file = open_file_write(dir + "/polygon.txt");

    for (const auto& point : polygon) {
        file << point.x << ' ' << point.y << '\n';
    }
}

void cover::save_square(const std::string& dir, const ClosedRectangleQ& square) {
    auto file = open_file_write(dir + "/square.txt");

    file << square.interval_x().lower() << ' ' << square.interval_x().upper() << ' '
         << square.interval_y().lower() << ' ' << square.interval_y().upper() << '\n';
}

void cover::save_digits(const std::string& dir, const uint32_t digits) {
    // TODO rename to digits.txt
    auto file = open_file_write(dir + "/precision.txt");

    file << digits << '\n';
}

void cover::save_singles(const std::string& dir, const std::map<SinglePair, size_t>& singles) {

    // TODO rename to singles.txt
    auto file = open_file_write(dir + "/stables.txt");

    for (const auto& kv : singles) {
        file << kv.second << ": " << kv.first << '\n';
    }
}

void cover::save_triples(const std::string& dir, const std::map<TriplePair, size_t>& triples) {

    auto file = open_file_write(dir + "/triples.txt");

    for (const auto& kv : triples) {
        file << kv.second << ": " << kv.first << '\n';
    }
}



struct CoverSaver final : public boost::static_visitor<> {

    std::ostream& os;
    const std::map<SinglePair, size_t>& singles;
    const std::map<TriplePair, size_t>& triples;


    explicit CoverSaver(std::ostream& os_,
                        const std::map<SinglePair, size_t>& singles_,
                        const std::map<TriplePair, size_t>& triples_)
        : os{os_}, singles{singles_}, triples{triples_}{}



    void operator()(const cover::Empty) {
        os << "E ";
    }

    void operator()(const cover::Single& single) {
        os << "S " << singles.at(single.single_pair) << ' ';
    }

    void operator()(const cover::Triple& triple) {
        os << "T " << triples.at(triple.triple_pair) << ' ';
    }


    void operator()(const cover::Divide& divide) {

        os << "D ";

        for (const auto& subcover : divide.quarters.get()) {
            boost::apply_visitor(*this, subcover);
        }
    }
};

void cover::save_cover(const std::string& dir, const cover::Cover& cover,
                       const std::map<SinglePair, size_t>& singles,
                       const std::map<TriplePair, size_t>& triples) {

    auto file = open_file_write(dir + "/cover.txt");

    CoverSaver visitor{file, singles, triples};

    boost::apply_visitor(visitor, cover);

    file << '\n';
    file.close();
}
