#include <boost/multiprecision/cpp_dec_float.hpp>
#include <tbb/parallel_invoke.h>
#include <algorithm>
#include <random>

#include "common.hpp"
#include "cover/save.hpp"
#include "database.hpp"
#include "database/serialize.hpp"
#include "database/deserialize.hpp"
#include "evaluator.hpp"
#include "equations.hpp"
#include "parse.hpp"
#include "verify.hpp"

using DecReal = boost::multiprecision::cpp_dec_float_50;

// TODO should we use the MRR polygon instead of the bounding polygon?
// That might speed things up somewhat

// 1 3 3
static std::set<CodeSequence> parse_singles(const std::string& str) {

    std::set<CodeSequence> singles{};

    const auto lines = split(str, "\n");

    for (const auto& line : lines) {
        const auto stable = parse_code_sequence(line);
        singles.insert(stable);
    }

    return singles;
}

static std::set<Triple> parse_triples(const std::string& str) {

    std::set<Triple> triples{};

    const auto lines = split(str, "\n");

    for (const auto& line : lines) {

        const auto comps = split(line, ",");

        const auto stable_neg = parse_code_sequence(comps.at(0));
        const auto unstable = parse_code_sequence(comps.at(1));
        const auto stable_pos = parse_code_sequence(comps.at(2));

        triples.emplace(stable_neg, unstable, stable_pos);
    }

    return triples;
}

static std::set<HalfTriple> parse_triples_half(const std::string& str) {

    std::set<HalfTriple> half_triples{};

    const auto lines = split(str, "\n");

    for (const auto& line : lines) {

        const auto comps = split(line, ",");

        const auto stable_neg = parse_code_sequence(comps.at(0));
        const auto unstable = parse_code_sequence(comps.at(1));


        if(comps.size()==3){
             const auto unstable2 = parse_code_sequence(comps.at(2));
             half_triples.emplace(stable_neg, unstable2);
        }


        half_triples.emplace(stable_neg, unstable);


    }

    return half_triples;
}

// x y\nx y\n
ClosedConvexPolygonQ parse_polygon(const std::string& str) {

    std::vector<PointQ> points{};

    const auto lines = split(str, "\n");

    for (const auto& line : lines) {

        const auto comps = split(line, " ");

        const Rational x = decimal_to_rational(comps.at(0)) / 90;
        const Rational y = decimal_to_rational(comps.at(1)) / 90;

        points.emplace_back(x, y);
    }

    return ClosedConvexPolygonQ{points};
}

// The is square small enough check can wait until we know what the common factor is, because
// we need to know what that is when doing this

#if 0
static std::string full_precision(const Float val) {
    std::ostringstream oss{};
    oss.precision(std::numeric_limits<Float>::max_digits10);
    oss << val;
    return oss.str();
}
#endif

// rational -> degrees is just multiple by 90 and then convert to float
#ifdef OLD
static std::string to_degrees(const ClosedRectangleQ& rect) {
    const auto min_x = static_cast<DecReal>(rect.interval_x.min * 90);
    const auto max_x = static_cast<DecReal>(rect.interval_x.max * 90);

    const auto min_y = static_cast<DecReal>(rect.interval_y.min * 90);
    const auto max_y = static_cast<DecReal>(rect.interval_y.max * 90);

    return (boost::format("x : [%1%, %2%], y : [%3%, %4%]") % min_x % max_x % min_y % max_y).str();
}
#endif

using StableInfos = std::vector<std::pair<SinglePair, StableInfo>>;
using TripleInfos = std::vector<std::pair<TriplePair, TripleInfo>>;
using HalfTripleInfos = std::vector<std::pair<HalfTriplePair, HalfTripleInfo>>;

// Returns the index in the single_infos of the code sequence that covers the square (if it exists)
static boost::optional<size_t> any_singles_positive(const StableInfos& single_infos, const std::vector<size_t>& single_indices, const ClosedRectangleQ& square, Evaluator& eval) {

    const auto center = square.center();
    // It's a square, so we can use width or height
    const Rational radius = square.width() / 2;
// george aug16th 2021 change to reverse order to make a better cover
    for (size_t j = 0 ; j < single_indices.size(); j++)
    {
//start
   // for (int j = single_indices.size() - 1; j >= 0; j--) {
        size_t i = single_indices[j];
        const auto& kv = single_infos.at(i);

        if (is_positive(kv.second, square, center, radius, eval)) {
            return i;
        }
    }
//end
    return boost::none;
}

// Returns the index in the triple_infos of the triple that covers the square (if it exists)
// This function is now identical to the above one
static boost::optional<size_t> any_triples_positive(const TripleInfos& triple_infos, const std::vector<size_t>& triple_indices, const ClosedRectangleQ& square, Evaluator& eval) {

    const auto center = square.center();
    // It's a square, so we can use width or height
    const Rational radius = square.width() / 2;

    for (const auto i : triple_indices) {
        const auto& kv = triple_infos.at(i);
        
        if (is_positive(kv.second, square, center, radius, eval)) {
            return i;
        }
    }

    return boost::none;
}

// Returns the index in the triple_infos of the triple that covers the square (if it exists)
// This function is now identical to the above one
static boost::optional<size_t> any_half_triples_positive(const HalfTripleInfos& triple_infos, const std::vector<size_t>& triple_indices, const ClosedRectangleQ& square, Evaluator& eval) {

    const auto center = square.center();
    // It's a square, so we can use width or height
    const Rational radius = square.width() / 2;


    for (const auto i : triple_indices) {
        const auto& kv = triple_infos.at(i);

        if (is_positive(kv.second, square, center, radius, eval)) {
            return i;
        }
    }

/*
    //int max_size = triple_indices.size();
    int max_size = static_cast<int>(triple_indices.size());
    //std::cout << "max_size " << max_size << std::endl;
    for (int k = 0; k < max_size - 1; k++)  {
        size_t kv_index1 = triple_indices[k];
        const auto& kv1 = triple_infos.at(kv_index1);
        for (int j = k + 1; j < max_size; j++) {
            size_t kv_index2 = triple_indices[j];
            const auto& kv2 = triple_infos.at(kv_index2);
            if(kv1.first.stable_neg.get() == kv2.first.stable_neg.get() && !(kv1.first.unstable.get() == kv2.first.unstable.get())) {
                if(is_positive_corner(kv1.second, kv2.second, square, center, radius, eval)) {
                    return kv_index1;
                }
            }
        }
    }
    */
    return boost::none;
}

// Returns the index in the triple_infos of the triple that covers the square (if it exists)
// This function is now identical to the above one
static boost::optional<size_t> any_unstables_positive(const HalfTripleInfos& triple_infos, const std::vector<size_t>& triple_indices, const ClosedRectangleQ& square, Evaluator& eval) {

    const auto center = square.center();
    // It's a square, so we can use width or height
    const Rational radius = square.width() / 2;


    for (const auto i : triple_indices) {
        const auto& kv = triple_infos.at(i);

        if (is_positive_unstable(kv.second, square, center, radius, eval)) {
            return i;
        }
    }

    return boost::none;
}

// Returns a list of indices of code sequences that intersect the given square
static std::vector<size_t> trim_single_indices(const ClosedRectangleQ& square, const StableInfos& single_infos, const std::vector<size_t>& single_indices) {

    std::vector<size_t> trimmed{};

    // really just a filter
    for (const auto i : single_indices) {
        const auto& single_info = single_infos.at(i).second;
        if (intersects(square, single_info.polygon)) {
            trimmed.push_back(i);
        }
    }


    return trimmed;
}

// Returns a list of indices of triples whose unstable code intersects the given square
// Again, this one is the same as the above one
static std::vector<size_t> trim_triple_indices(const ClosedRectangleQ& square, const TripleInfos& triple_infos, const std::vector<size_t>& triple_indices) {

    std::vector<size_t> trimmed{};

    // really just a filter
    for (const auto i : triple_indices) {
        const auto& triple_info = triple_infos.at(i).second;
        if (intersects(square, triple_info.unstable_info.segment)) {
            trimmed.push_back(i);
        }
    }

    return trimmed;
}

// Returns a list of indices of half triples whose unstable code intersects the given square

// Again, this one is the same as the above one
static std::vector<size_t> trim_half_triple_indices(const ClosedRectangleQ& square, const HalfTripleInfos& triple_infos, const std::vector<size_t>& triple_indices) {

    std::vector<size_t> trimmed{};

    // really just a filter
    for (const auto i : triple_indices) {
        const auto& triple_info = triple_infos.at(i).second;


        if (intersects(square, triple_info.unstable_info.segment) ) {
            trimmed.push_back(i);
        }
    }

    return trimmed;
}

// the stable indices are a superset of all code sequences that intersect the current square
// we need to trim it down
static cover::Cover cover_square(const ClosedRectangleQ& square, const ClosedConvexPolygonQ& polygon,
                                 const StableInfos& single_infos, const std::vector<size_t>& single_indices,
                                 const TripleInfos& triple_infos, const std::vector<size_t>& triple_indices,
                                 bool subset, const uint32_t prec, const uint32_t mags_left) {

    // The bigger square was not a subset so, check if the smaller square is a subset
    if (!subset) {
        subset = geometry::subset(square, polygon);

        if (!subset && !geometry::intersects(square, polygon)) {
            // This empty does not intersect the polygon, so we don't need to fill it in
            return cover::Empty{};
        }
        // Else if it is a subset or it intersects, then we can continue
    }

    // Each thread must get its own evaluator, and we want to re-use the evaluator as much as possible,
    // so let's create it here and then pass it along. In theory, it would be more efficient to have
    // one thread-local evaluator for each thread, but I unfortunately don't have that kind of control here.

    Evaluator eval{prec};

    const auto single_trimmed = trim_single_indices(square, single_infos, single_indices);
     /*std::cout<<square<< std::endl;
    for (const auto i : single_trimmed) {
          const auto& kv = single_infos.at(i);
          std::cout<<" choices "<< std::endl;
          std::cout<< kv.first<< std::endl;
    }*/
    const auto stab = any_singles_positive(single_infos, single_trimmed, square, eval);

    if (stab) {
        return cover::Single{single_infos.at(*stab).first};
    }

    const auto triple_trimmed = trim_triple_indices(square, triple_infos, triple_indices);
    const auto trip = any_triples_positive(triple_infos, triple_trimmed, square, eval);

    if (trip) {
        return cover::Triple{triple_infos.at(*trip).first};
    }

    const auto have_codes_left = !(single_trimmed.empty() && triple_trimmed.empty());

    if (have_codes_left && mags_left != 0) {
        // Some of the bounding polygons intersect the squares, so subdividing might work
        // We also have magnifications left, so we can continue subdividing

        const auto quarters = subdivide(square);

        // Contain cover::Empty by default, but these get overwritten, so it doesn't matter
        cover::Cover cover0{};
        cover::Cover cover1{};
        cover::Cover cover2{};
        cover::Cover cover3{};

        const auto l0 = [&] {
            cover0 = cover_square(std::get<0>(quarters), polygon, single_infos, single_trimmed, triple_infos, triple_trimmed, subset, prec, mags_left - 1);
        };

        const auto l1 = [&] {
            cover1 = cover_square(std::get<1>(quarters), polygon, single_infos, single_trimmed, triple_infos, triple_trimmed, subset, prec, mags_left - 1);
        };

        const auto l2 = [&] {
            cover2 = cover_square(std::get<2>(quarters), polygon, single_infos, single_trimmed, triple_infos, triple_trimmed, subset, prec, mags_left - 1);
        };

        const auto l3 = [&] {
            cover3 = cover_square(std::get<3>(quarters), polygon, single_infos, single_trimmed, triple_infos, triple_trimmed, subset, prec, mags_left - 1);
        };

        tbb::parallel_invoke(l0, l1, l2, l3);

        return cover::Divide{std::move(cover0), std::move(cover1), std::move(cover2), std::move(cover3)};
    }

    // Else no subdivisions left, so empty
    return cover::Empty{};
}

struct UpdateCover : public boost::static_visitor<cover::Cover> {

    const ClosedRectangleQ& square;
    const ClosedConvexPolygonQ& polygon;
    const StableInfos& single_infos;
    const TripleInfos& triple_infos;
    const uint32_t prec;
    const uint32_t mags_left;
/*
    explicit UpdateCover(const ClosedRectangleQ& square_, const ClosedConvexPolygonQ& polygon_,
                         const StableInfos& single_infos_, const TripleInfos& triple_infos_,
                         const uint32_t prec_, const uint32_t mags_left_)
        : square{square_}, polygon{polygon_}, single_infos{single_infos_}, triple_infos{triple_infos_},
          prec{prec_}, mags_left{mags_left_}, isHalf{false}, half_triple_infos{NULL} {}


    explicit UpdateCover(const ClosedRectangleQ& square_, const ClosedConvexPolygonQ& polygon_,
                             const StableInfos& single_infos_, const HalfTripleInfos& half_triple_infos_,
                             const uint32_t prec_, const uint32_t mags_left_)
            : square{square_}, polygon{polygon_}, single_infos{single_infos_}, half_triple_infos{half_triple_infos_},
              prec{prec_}, mags_left{mags_left_}, isHalf{true}, triple_infos{NULL} {}

*/
    explicit UpdateCover(const ClosedRectangleQ& square_, const ClosedConvexPolygonQ& polygon_,
                             const StableInfos& single_infos_, const TripleInfos& triple_infos_,
                             const uint32_t prec_, const uint32_t mags_left_)
            : square{square_}, polygon{polygon_}, single_infos{single_infos_}, triple_infos{triple_infos_},
              prec{prec_}, mags_left{mags_left_}
              {
              }

    cover::Cover operator()(const cover::Empty) const {

        // In theory, there is some information that we could be calculating as we go along,
        // such as trimming and subsetting. However, I would like to make the filled in-square
        // paths fast, so I don't want to do that
        //std::cout << "isHalf: " << isHalf << std::endl;
        std::vector<size_t> single_indices{};
        for (const auto i : falgo::range(single_infos.size())) {
            single_indices.push_back(i);
        }

            std::vector<size_t> triple_indices{};
            for (const auto i : falgo::range(triple_infos.size())) {
                triple_indices.push_back(i);
            }

            // We'll say it's not a subset right now
            return cover_square(square, polygon, single_infos, single_indices, triple_infos, triple_indices, false, prec, mags_left);


    }

    // For singles and triples, we just return what was already done
    cover::Cover operator()(const cover::Single& single) const {
        return single;
    }

    cover::Cover operator()(const cover::Triple& triple) const {
        return triple;
    }


    cover::Cover operator()(const cover::Divide& divide) const {

        // Apply recursively to each subsquare

        const auto quarter_squares = subdivide(square);
        const auto& quarter_covers = divide.quarters.get();

        // These values get overwritten anyway
        cover::Cover cover0{};
        cover::Cover cover1{};
        cover::Cover cover2{};
        cover::Cover cover3{};

        const auto l0 = [&] {

                //const UpdateCover update{std::get<0>(quarter_squares), polygon, single_infos, triple_infos, prec, mags_left - 1};
                HalfTripleInfos temp{};
                const UpdateCover update{std::get<0>(quarter_squares), polygon, single_infos, triple_infos, prec, mags_left - 1};
                cover0 = boost::apply_visitor(update, quarter_covers.get<0>());

        };

        const auto l1 = [&] {

                //const UpdateCover update{std::get<1>(quarter_squares), polygon, single_infos, triple_infos, prec, mags_left - 1};
                HalfTripleInfos temp{};
                const UpdateCover update{std::get<1>(quarter_squares), polygon, single_infos, triple_infos, prec, mags_left - 1};
                cover0 = boost::apply_visitor(update, quarter_covers.get<0>());

        };

        const auto l2 = [&] {

                //const UpdateCover update{std::get<0>(quarter_squares), polygon, single_infos, triple_infos, prec, mags_left - 1};
                HalfTripleInfos temp{};
                const UpdateCover update{std::get<2>(quarter_squares), polygon, single_infos, triple_infos, prec, mags_left - 1};
                cover0 = boost::apply_visitor(update, quarter_covers.get<0>());

        };

        const auto l3 = [&] {

                //const UpdateCover update{std::get<0>(quarter_squares), polygon, single_infos, triple_infos, prec, mags_left - 1};
                HalfTripleInfos temp{};
                const UpdateCover update{std::get<3>(quarter_squares), polygon, single_infos, triple_infos, prec, mags_left - 1};
                cover0 = boost::apply_visitor(update, quarter_covers.get<0>());

        };

        tbb::parallel_invoke(l0, l1, l2, l3);

        return cover::Divide{std::move(cover0), std::move(cover1), std::move(cover2), std::move(cover3)};
    }
};

static DecReal rationalToDegrees(const Rational& rat) {
    return DecReal{rat * 90};
}

template <typename K>
uint64_t get_or(const std::map<K, uint64_t>& m, const K& k, const uint64_t v) {

    const auto it = m.find(k);
    if (it == m.cend()) {
        return v;
    } else {
        return it->second;
    }
}

const char* getEmpties(const std::string& polygon_str, const std::string& singles_str, const std::string& triples_str,
    const uint32_t digits, const uint32_t max_depth, const size_t empty,
    const bool mrr, sqlite::ConnectionPool& pool, const bool is_last_cycle) {

    const cover::Cover& old_cover = cover::Empty{};
    const ClosedRectangleQ square{{0, 1}, {0, 1}};
    sqlite::PooledConnection conn{pool};
    const auto polygon = parse_polygon(polygon_str); // polygon to check
    const auto singles = parse_singles(singles_str); // singles to check holes with
    const auto triples = parse_triples(triples_str); // triples to check holes with

    const auto single_infos = get_single_infos(singles, mrr, conn.db);
    const auto triple_infos = get_triple_infos(triples, mrr, conn.db);

    const auto prec = digits_to_bits(digits);

    if (!geometry::subset(polygon, square)) {
        throw std::runtime_error("polygon is not a subset of the square");
    }

    // With the above check, the square will only be a subset of the polygon if the polygon
    // is equal to the square (so pretty well never), so we can say it's false
    HalfTripleInfos temp{};
    const UpdateCover updater{square, polygon, single_infos, triple_infos, prec, max_depth};
    const auto cover = boost::apply_visitor(updater, old_cover);

    const auto cover_info = cover_to_info(polygon, square, cover);

    const size_t num_to_print = falgo::min(cover_info.not_filled.size(), empty);
    static std::string new_coordinates;
    new_coordinates.clear();
    if (num_to_print != 0) {
        const size_t inc = cover_info.not_filled.size() / num_to_print;
        for (size_t i = 0; i < num_to_print * inc; i += inc) {
            new_coordinates.append(center_degrees(cover_info.not_filled.at(i))).append("\n");
        }
    }

    if (is_last_cycle || cover_info.not_filled.empty()) {
        const auto single_index_info = get_index_info(cover_info.single_square_count);
        const auto triple_index_info = get_index_info(cover_info.triple_square_count);

        const std::string dir{"cover"};

        cover::save_polygon(dir, polygon);
        cover::save_square(dir, square);
        cover::save_singles(dir, single_index_info);
        cover::save_triples(dir, triple_index_info);

        cover::save_cover(dir, cover, single_index_info, triple_index_info);
        cover::save_digits(dir, digits);
    }

    return new_coordinates.c_str();
}

const char* cover_polygon(const cover::Cover& old_cover,
                          const ClosedRectangleQ& square, const ClosedConvexPolygonQ& polygon,
                          const StableInfos& single_infos, const TripleInfos& triple_infos,
                          const uint32_t digits, const uint32_t max_mag, const size_t empties, const bool mrr) {

    const std::string dir{"cover"};

    auto file = open_file_write(dir + "/info.txt");

    for (const auto& vertex : polygon) {
        std::cout << rationalToDegrees(vertex.x) << ' ' << rationalToDegrees(vertex.y) << std::endl;//George aug19th 2021 customize the precision for the decimal in the info.txt
        file << "// " << rationalToDegrees(vertex.x)<< ' ' << rationalToDegrees(vertex.y) << '\n';
        //std::cout << rationalToDegrees(vertex.x).str(20) << ' ' << rationalToDegrees(vertex.y).str(20) << std::endl;//George aug19th 2021 customize the precision for the decimal in the info.txt
        //file << "// " << rationalToDegrees(vertex.x).str(20) << ' ' << rationalToDegrees(vertex.y).str(20) << '\n';
    }

    const auto prec = digits_to_bits(digits);

    const auto begin = std::chrono::steady_clock::now();

    if (!geometry::subset(polygon, square)) {
        throw std::runtime_error("polygon is not a subset of the square");
    }

    // With the above check, the square will only be a subset of the polygon if the polygon
    // is equal to the square (so pretty well never), so we can say it's false
    HalfTripleInfos temp{};
    const UpdateCover updater{square, polygon, single_infos, triple_infos, prec, max_mag};
    const auto cover = boost::apply_visitor(updater, old_cover);

    const auto end = std::chrono::steady_clock::now();

    const auto cover_info = cover_to_info(polygon, square, cover);

    const auto covered = cover_info.not_filled.empty();

    auto file_unused = open_file_write(dir + "/unused.txt");
    file_unused << "// Unused singles\n";
    for (const auto& p : single_infos) {
        const auto count = get_or(cover_info.single_square_count, p.first, 0);
        if (count == 0) {

            const auto& stable = p.first.stable.get().sequence;
            const auto cost = get_cost(p.second);

            const auto code_type = stable.type();
            const auto code_length = stable.length();
            const auto code_sum = stable.sum();

            // These colored in no squares, so they are left out, hence 0 for the square count

            const auto s = str(boost::format("%1% (%2%, %3%) (%4%, 0) - %5%") % code_type % code_length % code_sum % cost % stable);
            file_unused << s << '\n';
        }
    }

    file_unused << "// Unused triples\n";
    for (const auto& p : triple_infos) {
        const auto count = get_or(cover_info.triple_square_count, p.first, 0);
        if (count == 0) {
            const Triple triple{p.first.stable_neg.get().sequence,
                                p.first.unstable.get().sequence,
                                p.first.stable_pos.get().sequence};
            file_unused << triple << '\n';
        }
    }

    Integer total_single_cost = 0;

    std::cout << "The following stables colored squares:" << std::endl;
    file << "// The following stables colored squares:" << '\n';
    // We need to do this to have them sorted by cost when printing
    for (const auto& p : single_infos) {
        const auto count = get_or(cover_info.single_square_count, p.first, 0);
        if (count != 0) {
            const auto& stable = p.first.stable.get().sequence;
            const auto cost = get_cost(p.second);

            total_single_cost += count * cost;

            const auto code_type = stable.type();
            const auto code_length = stable.length();
            const auto code_sum = stable.sum();

            const auto s = str(boost::format("%1% (%2%, %3%) (%4%, %5%) - %6%") % code_type % code_length % code_sum % cost % count % stable);

            std::cout << s << std::endl;
            file << s << '\n';
        }
    }

    std::cout << "The following triples colored squares:" << std::endl;
    file << "// The following triples colored squares:" << '\n';
    for (const auto& p : triple_infos) {
        const auto count = get_or(cover_info.triple_square_count, p.first, 0);
        if (count != 0) {
            const Triple triple{p.first.stable_neg.get().sequence,
                                p.first.unstable.get().sequence,
                                p.first.stable_pos.get().sequence};
            std::cout << triple << std::endl;
            file << triple << '\n';
        }
    }

    std::cout << cover_info.not_filled.size() << " squares were not filled in" << std::endl;
    file << "// " << cover_info.not_filled.size() << " squares were not filled in" << '\n';

    static std::string res;
    res.clear();
    const size_t num_to_print = falgo::min(cover_info.not_filled.size(), empties);
    if (num_to_print != 0) {
        const size_t inc = cover_info.not_filled.size() / num_to_print;
        for (size_t i = 0; i < num_to_print * inc; i += inc) {
            std::cout << center_degrees(cover_info.not_filled.at(i)) << std::endl;
            file << "// " << center_degrees(cover_info.not_filled.at(i)) << '\n';

            const auto rect = cover_info.not_filled.at(i);
            const auto& interval_x = rect.interval_x();
            const auto& interval_y = rect.interval_y();
            std::string interval_x_str = boost::str(boost::format("%1% %2%") % interval_x.lower() % interval_x.upper());
            std::string interval_y_str = boost::str(boost::format("%1% %2%") % interval_y.lower() % interval_y.upper());
            res.append(interval_x_str).append(" ").append(interval_y_str).append("\n");
        }
    }

    uint64_t single_squares = 0;
    for (const auto& p : cover_info.single_square_count) {
        single_squares += p.second;
    }

    uint64_t triple_squares = 0;
    for (const auto& p : cover_info.triple_square_count) {
        triple_squares += p.second;
    }

    std::cout << single_squares << " stable squares used in the cover" << std::endl;
    file << "// " << single_squares << " stable squares used in the cover" << '\n';

    std::cout << triple_squares << " triple squares used in the cover" << std::endl;
    file << "// " << triple_squares << " triple squares used in the cover" << '\n';

    std::cout << cover_info.single_square_count.size() << " stables used in the cover" << std::endl;
    file << "// " << cover_info.single_square_count.size() << " stables used in the cover" << '\n';

    std::cout << cover_info.triple_square_count.size() << " triples used in the cover" << std::endl;
    file << "// " << cover_info.triple_square_count.size() << " triples used in the cover" << '\n';

    if (mrr) {
        std::cout << "MRR ";
        file << "// MRR ";
    } else {
        std::cout << "ALL ";
        file << "// ALL ";
    }

    std::cout << boost::format("at %1% decimals, deepest magnification %2%") % digits % cover_info.deepest << std::endl;
    file << boost::format("at %1% decimals, deepest magnification %2%") % digits % cover_info.deepest << '\n';

    std::cout << "Total stable cost: " << total_single_cost << std::endl;
    file << "// Total stable cost: " << total_single_cost << '\n';

    //added these two lines George Oct4,2017
    std::cout << cover_info.not_filled.size() << " squares were not filled in" << std::endl;
    file << "// " << cover_info.not_filled.size() << " squares were not filled in" << '\n';

    if (covered) {
        std::cout << "Covered" << std::endl;
        file << "// Covered" << '\n';
    } else {
        std::cout << "Not Covered" << std::endl;
        file << "// Not Covered" << '\n';
    }

    const auto single_index_info = get_index_info(cover_info.single_square_count);
    const auto triple_index_info = get_index_info(cover_info.triple_square_count);

    cover::save_polygon(dir, polygon);
    cover::save_square(dir, square);
    cover::save_singles(dir, single_index_info);
    cover::save_triples(dir, triple_index_info);

    cover::save_cover(dir, cover, single_index_info, triple_index_info);
    cover::save_digits(dir, digits);

    const auto hours = std::chrono::duration_cast<std::chrono::hours>(end - begin).count();
    const auto minutes = std::chrono::duration_cast<std::chrono::minutes>(end - begin).count() % 60;
    const auto seconds = std::chrono::duration_cast<std::chrono::seconds>(end - begin).count() % 60;
    const auto micros = std::chrono::duration_cast<std::chrono::microseconds>(end - begin).count() % 1000;

    std::ostringstream oss{};
    if (hours != 0) {
        oss << hours << "h ";
    }

    if (minutes != 0) {
        oss << minutes << "m ";
    }

    oss << seconds << '.' << micros << 's';

    std::cout << "Time elapsed: " << oss.str() << std::endl;
    file << "// Time elapsed: " << oss.str() << '\n';

    return res.c_str();
}

// Static variables need to be reset between each invocation. They are saved, surprisingly

const char* check_cover(const std::string& polygon_str, const std::string& singles_str, const std::string& triples_str,
                 const uint32_t digits, const uint32_t max_depth, const size_t empty,
                 const bool mrr, sqlite::ConnectionPool& pool) {

    // TODO it would be really nice to fix the number of digits in the precision.
    // That way we don't have to worry about it changing on us in between runs.
    // TODO should we also hardcode the square?

    // TODO we are also doing a lot of extra work. Once an equation is positive, you do not need to check
    // those again on later subdivisions, even if other equations fail.

    sqlite::PooledConnection conn{pool};

#if 0
    const std::string dir{"cover"};

    // Right now, the new polygon will always overwrite the old one
    const auto square = cover::load_square(dir);
    const auto cover_singles = cover::load_singles(dir);
    const auto cover_triples = cover::load_triples(dir);
    const auto cover = cover::load_cover(dir, cover_singles, cover_triples);
    const auto cover_digits = cover::load_digits(dir);
#endif

    // Just use the default values for now
    const auto cover = cover::Empty{};
    const ClosedRectangleQ square{{0, 1}, {0, 1}};

    // Right now we just check using the new singles and triples. It would be nice to merge them
    // (especially if the max_depth is increased), but George only adds, not subtracts, so we'll
    // leave it
    const auto polygon = parse_polygon(polygon_str); // polygon to check
    const auto singles = parse_singles(singles_str); // singles to check holes with
    const auto triples = parse_triples(triples_str); // triples to check holes with

    const auto single_infos = get_single_infos(singles, mrr, conn.db);
    const auto triple_infos = get_triple_infos(triples, mrr, conn.db);

    return cover_polygon(cover, square, polygon, single_infos, triple_infos, digits, max_depth, empty, mrr);
}

const char* cover_small_polygon(const cover::Cover& old_cover,
                          const ClosedRectangleQ& square, const ClosedConvexPolygonQ& polygon,
                          const StableInfos& single_infos, const TripleInfos& triple_infos,
                          const uint32_t digits, const uint32_t max_mag, const size_t empties, const bool mrr, const bool printInfo) {

    const std::string dir{"small_cover"};

    auto file = open_file_write(dir + "/info.txt");

    for (const auto& vertex : polygon) {
        if (printInfo) std::cout << rationalToDegrees(vertex.x) << ' ' << rationalToDegrees(vertex.y) << std::endl;//George aug19th 2021 customize the precision for the decimal in the info.txt
        file << "// " << rationalToDegrees(vertex.x)<< ' ' << rationalToDegrees(vertex.y) << '\n';
        //std::cout << rationalToDegrees(vertex.x).str(20) << ' ' << rationalToDegrees(vertex.y).str(20) << std::endl;//George aug19th 2021 customize the precision for the decimal in the info.txt
        //file << "// " << rationalToDegrees(vertex.x).str(20) << ' ' << rationalToDegrees(vertex.y).str(20) << '\n';
    }

    const auto prec = digits_to_bits(digits);

    const auto begin = std::chrono::steady_clock::now();

    if (!geometry::subset(polygon, square)) {
        throw std::runtime_error("polygon is not a subset of the square");
    }

    // With the above check, the square will only be a subset of the polygon if the polygon
    // is equal to the square (so pretty well never), so we can say it's false
    HalfTripleInfos temp{};
    const UpdateCover updater{square, polygon, single_infos, triple_infos, prec, max_mag};
    const auto cover = boost::apply_visitor(updater, old_cover);

    const auto end = std::chrono::steady_clock::now();

    const auto cover_info = cover_to_info(polygon, square, cover);

    const auto covered = cover_info.not_filled.empty();

    auto file_unused = open_file_write(dir + "/unused.txt");
    file_unused << "// Unused singles\n";
    for (const auto& p : single_infos) {
        const auto count = get_or(cover_info.single_square_count, p.first, 0);
        if (count == 0) {

            const auto& stable = p.first.stable.get().sequence;
            const auto cost = get_cost(p.second);

            const auto code_type = stable.type();
            const auto code_length = stable.length();
            const auto code_sum = stable.sum();

            // These colored in no squares, so they are left out, hence 0 for the square count

            const auto s = str(boost::format("%1% (%2%, %3%) (%4%, 0) - %5%") % code_type % code_length % code_sum % cost % stable);
            file_unused << s << '\n';
        }
    }

    file_unused << "// Unused triples\n";
    for (const auto& p : triple_infos) {
        const auto count = get_or(cover_info.triple_square_count, p.first, 0);
        if (count == 0) {
            const Triple triple{p.first.stable_neg.get().sequence,
                                p.first.unstable.get().sequence,
                                p.first.stable_pos.get().sequence};
            file_unused << triple << '\n';
        }
    }

    Integer total_single_cost = 0;

    static std::string res;
    res.clear();

    if (printInfo) std::cout << "The following stables colored squares:" << std::endl;
    file << "// The following stables colored squares:" << '\n';
    // We need to do this to have them sorted by cost when printing
    for (const auto& p : single_infos) {
        const auto count = get_or(cover_info.single_square_count, p.first, 0);
        if (count != 0) {
            const auto& stable = p.first.stable.get().sequence;
            const auto cost = get_cost(p.second);

            total_single_cost += count * cost;

            const auto code_type = stable.type();
            const auto code_length = stable.length();
            const auto code_sum = stable.sum();

            const auto s = str(boost::format("%1% (%2%, %3%) (%4%, %5%) - %6%") % code_type % code_length % code_sum % cost % count % stable);

            if (printInfo) std::cout << s << std::endl;
            file << s << '\n';
            res.append(s).append("\n");
        }
    }

    res.append("-----\n");

    if (printInfo) std::cout << "The following triples colored squares:" << std::endl;
    file << "// The following triples colored squares:" << '\n';
    for (const auto& p : triple_infos) {
        const auto count = get_or(cover_info.triple_square_count, p.first, 0);
        if (count != 0) {
            const Triple triple{p.first.stable_neg.get().sequence,
                                p.first.unstable.get().sequence,
                                p.first.stable_pos.get().sequence};
            if (printInfo) std::cout << triple << std::endl;
            file << triple << '\n';

            const auto stable_neg = boost::str(boost::format("%1%") % p.first.stable_neg.get().sequence);
            const auto unstable = boost::str(boost::format("%1%") % p.first.unstable.get().sequence);
            const auto stable_pos = boost::str(boost::format("%1%") % p.first.stable_pos.get().sequence);

            res.append(stable_neg).append(", ").append(unstable).append(", ").append(stable_pos).append("\n");
        }
    }

    if (printInfo) std::cout << cover_info.not_filled.size() << " squares were not filled in" << std::endl;
    file << "// " << cover_info.not_filled.size() << " squares were not filled in" << '\n';

    res.append("-----\n");
    const size_t num_to_print = falgo::min(cover_info.not_filled.size(), empties);
    if (num_to_print != 0) {
        const size_t inc = cover_info.not_filled.size() / num_to_print;
        for (size_t i = 0; i < num_to_print * inc; i += inc) {
            res.append(center_degrees(cover_info.not_filled.at(i))).append("\n");
            if (printInfo) std::cout << center_degrees(cover_info.not_filled.at(i)) << std::endl;
            file << "// " << center_degrees(cover_info.not_filled.at(i)) << '\n';
        }
    }

    uint64_t single_squares = 0;
    for (const auto& p : cover_info.single_square_count) {
        single_squares += p.second;
    }

    uint64_t triple_squares = 0;
    for (const auto& p : cover_info.triple_square_count) {
        triple_squares += p.second;
    }

    if (printInfo) std::cout << single_squares << " stable squares used in the cover" << std::endl;
    file << "// " << single_squares << " stable squares used in the cover" << '\n';

    if (printInfo) std::cout << triple_squares << " triple squares used in the cover" << std::endl;
    file << "// " << triple_squares << " triple squares used in the cover" << '\n';

    if (printInfo) std::cout << cover_info.single_square_count.size() << " stables used in the cover" << std::endl;
    file << "// " << cover_info.single_square_count.size() << " stables used in the cover" << '\n';

    if (printInfo) std::cout << cover_info.triple_square_count.size() << " triples used in the cover" << std::endl;
    file << "// " << cover_info.triple_square_count.size() << " triples used in the cover" << '\n';

    if (mrr) {
        if (printInfo) std::cout << "MRR ";
        file << "// MRR ";
    } else {
        if (printInfo) std::cout << "ALL ";
        file << "// ALL ";
    }

    if (printInfo) std::cout << boost::format("at %1% decimals, deepest magnification %2%") % digits % cover_info.deepest << std::endl;
    file << boost::format("at %1% decimals, deepest magnification %2%") % digits % cover_info.deepest << '\n';

    if (printInfo) std::cout << "Total stable cost: " << total_single_cost << std::endl;
    file << "// Total stable cost: " << total_single_cost << '\n';

    //added these two lines George Oct4,2017
    if (printInfo) std::cout << cover_info.not_filled.size() << " squares were not filled in" << std::endl;
    file << "// " << cover_info.not_filled.size() << " squares were not filled in" << '\n';

    if (covered) {
        if (printInfo) std::cout << "Covered" << std::endl;
        file << "// Covered" << '\n';
    } else {
        if (printInfo) std::cout << "Not Covered" << std::endl;
        file << "// Not Covered" << '\n';
    }

    const auto single_index_info = get_index_info(cover_info.single_square_count);
    const auto triple_index_info = get_index_info(cover_info.triple_square_count);

    cover::save_polygon(dir, polygon);
    cover::save_square(dir, square);
    cover::save_singles(dir, single_index_info);
    cover::save_triples(dir, triple_index_info);

    cover::save_cover(dir, cover, single_index_info, triple_index_info);
    cover::save_digits(dir, digits);

    const auto hours = std::chrono::duration_cast<std::chrono::hours>(end - begin).count();
    const auto minutes = std::chrono::duration_cast<std::chrono::minutes>(end - begin).count() % 60;
    const auto seconds = std::chrono::duration_cast<std::chrono::seconds>(end - begin).count() % 60;
    const auto micros = std::chrono::duration_cast<std::chrono::microseconds>(end - begin).count() % 1000;

    std::ostringstream oss{};
    if (hours != 0) {
        oss << hours << "h ";
    }

    if (minutes != 0) {
        oss << minutes << "m ";
    }

    oss << seconds << '.' << micros << 's';

    if (printInfo) std::cout << "Time elapsed: " << oss.str() << std::endl << "// ----------" << std::endl;
    file << "// Time elapsed: " << oss.str() << '\n';

    return res.c_str();
}

// Zhao Yu Li, Aug 1, 2025.
// From Google Gemini
// Prompt: c++ parse fraction to double
double parse_fraction_to_double(const std::string& fractionString) {
    size_t slashPos = fractionString.find('/');
    if (slashPos == std::string::npos) {
        // Handle cases where no '/' is found (e.g., "5")
        try {
            return std::stod(fractionString);
        } catch (const std::invalid_argument& e) {
            throw std::invalid_argument("Invalid number format: " + fractionString);
        } catch (const std::out_of_range& e) {
            throw std::out_of_range("Number out of range for double: " + fractionString);
        }
    }

    std::string numeratorStr = fractionString.substr(0, slashPos);
    std::string denominatorStr = fractionString.substr(slashPos + 1);

    try {
        int numerator = std::stoi(numeratorStr);
        int denominator = std::stoi(denominatorStr);

        if (denominator == 0) {
            throw std::runtime_error("Division by zero in fraction: " + fractionString);
        }

        return static_cast<double>(numerator) / denominator;
    } catch (const std::invalid_argument& e) {
        throw std::invalid_argument("Invalid number in fraction: " + fractionString);
    } catch (const std::out_of_range& e) {
        throw std::out_of_range("Number out of range in fraction: " + fractionString);
    }
}

const char* check_small_cover(const std::string& polygon_str, const std::string& singles_str, const std::string& triples_str,
                 const uint32_t digits, const uint32_t max_depth, const size_t empty,
                 const bool mrr, sqlite::ConnectionPool& pool, const bool printInfo) {

    // TODO it would be really nice to fix the number of digits in the precision.
    // That way we don't have to worry about it changing on us in between runs.
    // TODO should we also hardcode the square?

    // TODO we are also doing a lot of extra work. Once an equation is positive, you do not need to check
    // those again on later subdivisions, even if other equations fail.

    sqlite::PooledConnection conn{pool};

#if 0
    const std::string dir{"cover"};

    // Right now, the new polygon will always overwrite the old one
    const auto square = cover::load_square(dir);
    const auto cover_singles = cover::load_singles(dir);
    const auto cover_triples = cover::load_triples(dir);
    const auto cover = cover::load_cover(dir, cover_singles, cover_triples);
    const auto cover_digits = cover::load_digits(dir);
#endif

    // Just use the default values for now
    const auto cover = cover::Empty{};

    const auto coordinates = split(polygon_str, " ");
    const ClosedRectangleQ square{
        {parse_fraction_to_double(coordinates[0]), parse_fraction_to_double(coordinates[1])},
        {parse_fraction_to_double(coordinates[2]), parse_fraction_to_double(coordinates[3])}
    };

    // Right now we just check using the new singles and triples. It would be nice to merge them
    // (especially if the max_depth is increased), but George only adds, not subtracts, so we'll
    // leave it
    const ClosedConvexPolygonQ polygon{
        {square.lower_left(), square.upper_left(), square.upper_right(), square.lower_right()},
    }; // polygon to check
    const auto singles = parse_singles(singles_str); // singles to check holes with
    const auto triples = parse_triples(triples_str); // triples to check holes with

    const auto single_infos = get_single_infos(singles, mrr, conn.db);
    const auto triple_infos = get_triple_infos(triples, mrr, conn.db);

    return cover_small_polygon(cover, square, polygon, single_infos, triple_infos, digits, max_depth, empty, mrr, printInfo);
}

int32_t check_cover_duplicate_stables(const std::string& polygon_str, const std::string& singles_str, const std::string& triples_str,
                 const bool mrr, sqlite::ConnectionPool& pool, const bool show) {

    sqlite::PooledConnection conn{pool};

    const auto triples = parse_triples(triples_str); //triples to check holes with

    std::pair<bool, bool> Q = get_triple_infos_duplicate_stables(triples, mrr, conn.db, show);

    if(Q.first && Q.second){
        return 2;
    }
    if(Q.first || Q.second){
        return 1;
    }
    return 0;
}

std::string update_equation(CodeSequence code_sequence, StableInfo stable_neg, sqlite::Database& db) {
    std::string origin_eq{};
    const auto sql_select = "select equations from " + database::serialize(code_sequence.type()) + " where code_sequence = ?;";
    db.prepare(sql_select)
        .bind(database::serialize(code_sequence))
        .exec(origin_eq);

    //std::set<EqVec<Sin>> sines;
    std::ostringstream oss_sin{};
    //oss_sin << "(";
    for (const auto eq_pair : stable_neg.sines) {
        //sines.insert(eq_pair.first);

        oss_sin << eq_pair.first << "\n";
    }
    oss_sin << "sin 1 1 0";
    std::string sines = oss_sin.str();
    //std::cout << "sines: " << sines << std::endl;

    std::ostringstream oss_cos{};
    //oss_cos << "(";
    //std::set<EqVec<Cos>> cosines;
    for (const auto eq_pair : stable_neg.cosines) {
        //cosines.insert(eq_pair.first);
        if (eq_pair.second == 0) continue;
        oss_cos << eq_pair.first << "\n";
        //std::cout << eq_pair.first << "\n";
    }
    oss_cos << "sin 1 0 1";
    std::string cosines = oss_cos.str();
    //std::cout << "cosines: " << cosines << std::endl;

    const std::string sql_update = "update " + database::serialize(code_sequence.type()) + " set equations = ? where code_sequence = ?;";

    db.prepare(sql_update)
        .bind(sines + "\n" + cosines, database::serialize(code_sequence))
        .exec();

    return origin_eq;
}

std::string update_initial_angles(CodeSequence code_sequence, sqlite::Database& db ) {
    std::string origin_initial_angles{};
    const auto sql_select = "select initial_angles from " + database::serialize(code_sequence.type()) + " where code_sequence = ?;";
    db.prepare(sql_select)
        .bind(database::serialize(code_sequence))
        .exec(origin_initial_angles);

    std::ostringstream oss_init_angles{};
    for (const auto angle : origin_initial_angles) {
        if (angle == 'z') {
            oss_init_angles << "z";
        }
        else if (angle == 'x') {
            oss_init_angles << "y";
        }
        else if (angle == 'y'){
            oss_init_angles << "x";
        }
    }

    const std::string sql_update = "update " + database::serialize(code_sequence.type()) + " set initial_angles = ? where code_sequence = ?;";
    db.prepare(sql_update)
        .bind(oss_init_angles.str(), database::serialize(code_sequence))
        .exec();

    return origin_initial_angles;
}

std::string update_points(CodeSequence code_sequence, sqlite::Database& db) {
    std::string origin_points{};
    const auto sql_select = "select points from " + database::serialize(code_sequence.type()) + " where code_sequence = ?;";
    db.prepare(sql_select)
        .bind(database::serialize(code_sequence))
        .exec(origin_points);

    std::string new_points = "3.1415926 0\n0 3.1415926\n0 0";
    std::string sql_update = "update " + database::serialize(code_sequence.type()) + " set points = ? where code_sequence = ?;";
    db.prepare(sql_update)
        .bind(new_points, database::serialize(code_sequence))
        .exec();
    return origin_points;
}

std::string update_unstable_equations(CodeSequence code_sequence, sqlite::Database& db) {
    std::string origin_eq{};
    const auto sql_select = "select equations from " + database::serialize(code_sequence.type()) + " where code_sequence = ?;";
    db.prepare(sql_select)
        .bind(database::serialize(code_sequence))
        .exec(origin_eq);

    std::string eq_copy {origin_eq + "\n"};
    std::ostringstream oss{};
    int start = 0;
    int end = eq_copy.find("\n");
    //std::cout << "DEBUG " << code_sequence << std::endl;
    while (end != -1) {
        std::string line = eq_copy.substr(start, end - start) + " ";
        //std::cout << line << std::endl;
        int i = 0;
        int j = line.find(" ");
        oss << line.substr(i, j - i);
        i = j + 1;
        j = line.find(" ", i);
        while(j != -1) {
            std::string coeff1 = line.substr(i, j - i);
            i = j + 1;
            j = line.find(" ", i);
            std::string coeff2 = line.substr(i, j - i);
            i = j + 1;
            j = line.find(" ", i);
            std::string coeff3 = line.substr(i, j - i);
            i = j + 1;
            j = line.find(" ", i);
            oss << " " << coeff1 << " " << coeff3 << " " << coeff2;
        }
        oss << "\n";
        start = end + 1;
        end = eq_copy.find("\n", start);
    }
    //std::cout << "after " << std::endl;
    //std::cout << oss.str() << std::endl;

    std::string sql_update = "update " + database::serialize(code_sequence.type()) + " set equations = ? where code_sequence = ?;";
    db.prepare(sql_update)
        .bind(oss.str().substr(0, oss.str().size() - 1) , database::serialize(code_sequence))
        .exec();

    return origin_eq;
}


void restore(std::set<std::pair<CodeSequence, std::string>> sequence_equations, std::set<std::pair<CodeSequence, std::string>> sequence_init_angles,
            std::set<std::pair<CodeSequence, std::string>> sequence_points, sqlite::ConnectionPool& pool) {
    sqlite::PooledConnection conn{pool};
    for (const auto& pair : sequence_equations) {
        CodeSequence code_sequence = pair.first;
        std::string equations = pair.second;
        std::string sql_update = "update " + database::serialize(code_sequence.type()) + " set equations = ? where code_sequence = ?;";

        conn.db.prepare(sql_update)
            .bind(equations, database::serialize(code_sequence))
            .exec();
    }
    for (const auto& pair : sequence_init_angles) {
        CodeSequence code_sequence = pair.first;
        std::string initial_angles = pair.second;
        std::string sql_update = "update " + database::serialize(code_sequence.type()) + " set initial_angles = ? where code_sequence = ?;";

        conn.db.prepare(sql_update)
            .bind(initial_angles, database::serialize(code_sequence))
            .exec();
    }
    for (const auto& pair : sequence_points) {
        CodeSequence code_sequence = pair.first;
        std::string points = pair.second;
        std::string sql_update = "update " + database::serialize(code_sequence.type()) + " set points = ? where code_sequence = ?;";

        conn.db.prepare(sql_update)
            .bind(points, database::serialize(code_sequence))
            .exec();
    }
}

using namespace boost::multiprecision;
bool check_cover_half_duplicate_stables(const std::string& polygon_str, const std::string& singles_str, const std::string& triples_str,
                 const uint32_t digits, const uint32_t max_depth, const size_t empty,
                 const bool mrr, std::set<std::pair<CodeSequence, std::string>>& sequence_equations, sqlite::ConnectionPool& pool) {

    sqlite::PooledConnection conn{pool};

    const auto triples = parse_triples_half(triples_str); // triples to check holes with

    return get_triple_infos_half_duplicate_stables(triples, mrr, conn.db);
}

bool check_cover_all(const std::string& mrr_dir, sqlite::ConnectionPool& pool, const uint32_t extra_depth) {

    sqlite::PooledConnection conn{pool};

    const auto load = [&](std::map<SinglePair, StableInfo>& single_infos,
                          std::map<TriplePair, TripleInfo>& triple_infos,
                          const std::vector<SinglePair>& mrr_singles,
                          const std::vector<TriplePair>& mrr_triples) {
        // Need to use a set because we might get some duplicates
        std::set<CodePair> set_stables{};
        for (const auto& single_pair : mrr_singles) {
            set_stables.insert(single_pair.stable.get());
        }

        for (const auto& triple_pair : mrr_triples) {
            set_stables.insert(triple_pair.stable_neg.get());
            set_stables.insert(triple_pair.stable_pos.get());
        }

        // Reverse the order of the code pairs to ease the memory loading
        std::vector<CodePair> new_stables{set_stables.begin(), set_stables.end()};
        falgo::reverse(new_stables);

        single_infos = get_single_infos_map(new_stables, false, conn.db);
        triple_infos = get_triple_infos_map(mrr_triples, false, conn.db);
    };

    const std::string all_dir{"cover"};

    return cover_square_all(mrr_dir, all_dir, load, extra_depth);
}
