#include <boost/format.hpp>

#include "cover/load.hpp"
#include "cover/save.hpp"
#include "merge.hpp"
#include "trim.hpp"

struct CoverTrim : public boost::static_visitor<cover::Cover> {

    const ClosedConvexPolygonQ& polygon;
    const ClosedRectangleQ& square;

    explicit CoverTrim(const ClosedConvexPolygonQ& polygon_, const ClosedRectangleQ& square_)
        : polygon{polygon_}, square{square_} {}

    cover::Cover operator()(const cover::Empty) const {
        return cover::Empty{};
    }

    cover::Cover operator()(cover::Single& single) const {
        if (geometry::intersects(square, polygon)) {
            return std::move(single);
        } else {
            return cover::Empty{};
        }
    }

    cover::Cover operator()(cover::Triple& triple) const {
        if (geometry::intersects(square, polygon)) {
            return std::move(triple);
        } else {
            return cover::Empty{};
        }
    }


    cover::Cover operator()(cover::Divide& divide) const {

        if (geometry::subset(square, polygon)) {
            return std::move(divide);
        }

        if (geometry::intersects(square, polygon)) {

            // Apply recursively to each subsquare

            const auto quarter_squares = subdivide(square);
            auto& quarter_covers = divide.quarters.get();

            auto cover0 = boost::apply_visitor(CoverTrim{polygon, std::get<0>(quarter_squares)}, quarter_covers.get<0>());
            auto cover1 = boost::apply_visitor(CoverTrim{polygon, std::get<1>(quarter_squares)}, quarter_covers.get<1>());
            auto cover2 = boost::apply_visitor(CoverTrim{polygon, std::get<2>(quarter_squares)}, quarter_covers.get<2>());
            auto cover3 = boost::apply_visitor(CoverTrim{polygon, std::get<3>(quarter_squares)}, quarter_covers.get<3>());

            return cover::Divide{std::move(cover0), std::move(cover1), std::move(cover2), std::move(cover3)};
        }

        return cover::Empty{};
    }
};

void trim_cover(const ClosedConvexPolygonQ& polygon, const std::string& in_dir, const std::string& out_dir) {

    const auto in_square = cover::load_square(in_dir);
    const auto in_singles = cover::load_singles(in_dir);
    const auto in_triples = cover::load_triples(in_dir);

    auto in_cover = cover::load_cover(in_dir, in_singles, in_triples);
    const auto in_digits = cover::load_digits(in_dir);

    //const OpenRectangleQ square{{in_square.interval_x().lower(), in_square.interval_x().upper()},
    //{in_square.interval_y().lower(), in_square.interval_y().upper()}};

    const auto out_cover = boost::apply_visitor(CoverTrim{polygon, in_square}, in_cover);

    auto file = open_file_write(out_dir + "/info.txt");

    CoverUsedVisitor used{};
    boost::apply_visitor(used, out_cover);

    file << "// The following stables colored squares:" << '\n';
    for (const auto& single : used.singles) {

        const auto& stable = single.stable.get().sequence;

        const auto code_type = stable.type();
        const auto code_length = stable.length();
        const auto code_sum = stable.sum();

        const auto s = str(boost::format("%1% (%2%, %3%) - %4%") % code_type % code_length % code_sum % stable);

        file << s << '\n';
    }

    file << "// The following triples colored squares:" << '\n';
    for (const auto& p : used.triples) {

        file << p.stable_neg.get().sequence << ", " << p.unstable.get().sequence << ", " << p.stable_pos.get().sequence << '\n';
    }

    file << "// " << used.singles.size() << " stables used in the cover" << '\n';
    file << "// " << used.triples.size() << " triples used in the cover" << '\n';

    const auto single_index_info = get_index_info(used.singles);
    const auto triple_index_info = get_index_info(used.triples);

    cover::save_polygon(out_dir, polygon);
    cover::save_square(out_dir, in_square);
    cover::save_singles(out_dir, single_index_info);
    cover::save_triples(out_dir, triple_index_info);
    cover::save_cover(out_dir, out_cover, single_index_info, triple_index_info);
    cover::save_digits(out_dir, in_digits);
}
