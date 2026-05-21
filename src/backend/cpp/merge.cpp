#include <boost/format.hpp>

#include "cover/load.hpp"
#include "cover/save.hpp"
#include "merge.hpp"

struct CoverUnion : public boost::static_visitor<cover::Cover> {

    // Empty
    cover::Cover operator()(const cover::Empty, const cover::Empty) const {
        return cover::Empty{};
    }

    cover::Cover operator()(const cover::Empty, cover::Single& single) const {
        return std::move(single);
    }

    cover::Cover operator()(const cover::Empty, cover::Triple& triple) const {
        return std::move(triple);
    }


    cover::Cover operator()(const cover::Empty, cover::Divide& divide) const {
        return std::move(divide);
    }



    // Single
    cover::Cover operator()(cover::Single& single, const cover::Empty) const {
        return std::move(single);
    }

    cover::Cover operator()(cover::Single& single0, cover::Single& single1) const {

        if (single0.single_pair < single1.single_pair) {
            return std::move(single0);
        } else {
            return std::move(single1);
        }
    }

    cover::Cover operator()(cover::Single& single, const cover::Triple&) const {
        return std::move(single);
    }


    cover::Cover operator()(cover::Single& single, const cover::Divide&) const {
        return std::move(single);
    }

    // Triples

    cover::Cover operator()(cover::Triple& triple, const cover::Empty) const {
        return std::move(triple);
    }

    cover::Cover operator()(const cover::Triple&, cover::Single& single) const {
        return std::move(single);
    }


    cover::Cover operator()(cover::Triple& triple0, cover::Triple& triple1) const {

        if (triple0.triple_pair < triple1.triple_pair) {
            return std::move(triple0);
        } else {
            return std::move(triple1);
        }
    }

    cover::Cover operator()(cover::Triple& triple, const cover::Divide&) const {
        return std::move(triple);
    }

    // Divide

    cover::Cover operator()(cover::Divide& divide, const cover::Empty) const {
        return std::move(divide);
    }

    cover::Cover operator()(const cover::Divide&, cover::Single& single) const {
        return std::move(single);
    }

    cover::Cover operator()(const cover::Divide&, cover::Triple& triple) const {
        return std::move(triple);
    }


    cover::Cover operator()(cover::Divide& divide0, cover::Divide& divide1) const {
        // The recursive case
        auto merged0 = boost::apply_visitor(*this, divide0.quarters.get().get<0>(), divide1.quarters.get().get<0>());
        auto merged1 = boost::apply_visitor(*this, divide0.quarters.get().get<1>(), divide1.quarters.get().get<1>());
        auto merged2 = boost::apply_visitor(*this, divide0.quarters.get().get<2>(), divide1.quarters.get().get<2>());
        auto merged3 = boost::apply_visitor(*this, divide0.quarters.get().get<3>(), divide1.quarters.get().get<3>());

        return cover::Divide{std::move(merged0), std::move(merged1), std::move(merged2), std::move(merged3)};
    }

};

void union_covers(const std::string& merged_dir, const std::vector<std::string>& dirs, sqlite::Database&) {

    uint32_t max_prec = 0;

    const ClosedRectangleQ unit_square{{0, 1}, {0, 1}};

    cover::Cover cover_union = cover::Empty{};

    for (const auto& dir : dirs) {

        const auto singles = cover::load_singles(dir);
        //std::cout << "loaded singles" << std::endl;
        const auto triples = cover::load_triples(dir);
        //std::cout << "loaded triples" << std::endl;

        auto cover = cover::load_cover(dir, singles, triples);
        //std::cout << "loaded cover" << std::endl;
        const auto prec = cover::load_digits(dir);
        //std::cout << "loaded digits" << std::endl;
        const auto square = cover::load_square(dir);
        //std::cout << "loaded square" << std::endl;

        if (square != unit_square) {
            throw std::runtime_error("union_covers: not unit square");
        }

        if (prec > max_prec) {
            max_prec = prec;
        }

        cover_union = boost::apply_visitor(CoverUnion{}, cover_union, cover);

        std::cout << "merged " << dir << std::endl;
    }

    auto file = open_file_write(merged_dir + "/info.txt");

    CoverUsedVisitor used{};
    boost::apply_visitor(used, cover_union);

    //std::cout << "The following stables colored squares:" << std::endl;
    file << "// The following stables colored squares:" << '\n';
    for (const auto& single : used.singles) {

        const auto& stable = single.stable.get().sequence;

        const auto code_type = stable.type();
        const auto code_length = stable.length();
        const auto code_sum = stable.sum();

        const auto s = str(boost::format("%1% (%2%, %3%) - %4%") % code_type % code_length % code_sum % stable);

        //std::cout << s << std::endl;
        file << s << '\n';
    }

    //std::cout << "The following triples colored squares:" << std::endl;
    file << "// The following triples colored squares:" << '\n';
    for (const auto& p : used.triples) {

        //std::cout << p.stable_neg.sequence << ", " << p.unstable.sequence << ", " << p.stable_pos.sequence << std::endl;
        file << p.stable_neg.get().sequence << ", " << p.unstable.get().sequence << ", " << p.stable_pos.get().sequence << '\n';
    }

    //std::cout << "// " << used.singles.size() << " stables used in the cover" << std::endl;
    file << "// " << used.singles.size() << " stables used in the cover" << '\n';

    //std::cout << "// " << used.triples.size() << " triples used in the cover" << std::endl;
    file << "// " << used.triples.size() << " triples used in the cover" << '\n';

    const auto single_index_info = get_index_info(used.singles);
    const auto triple_index_info = get_index_info(used.triples);
    const auto half_triple_index_info = get_index_info(used.half_triples);
    // We use the unit square again because we can't calculate unions right now
    // It is up to the user to fix them
    const ClosedConvexPolygonQ polygon{{{0, 0}, {1, 0}, {1, 1}, {0, 1}}};

    cover::save_polygon(merged_dir, polygon);
    cover::save_square(merged_dir, unit_square);
    cover::save_singles(merged_dir, single_index_info);
    cover::save_triples(merged_dir, triple_index_info);

    cover::save_cover(merged_dir, cover_union, single_index_info, triple_index_info);
    cover::save_digits(merged_dir, max_prec);
}
