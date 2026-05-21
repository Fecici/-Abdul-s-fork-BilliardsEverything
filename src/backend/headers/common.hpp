#pragma once

#include <boost/format.hpp>
#include <boost/numeric/conversion/cast.hpp>
#include <chrono>

#include "cover/cover.hpp"
#include "cover/load.hpp"
#include "cover/save.hpp"
#include "evaluator.hpp"
#include "general.hpp"

template <template <typename> class Trig>
Coeff64 gradient_bound(const LinComMapZ<Trig<LinComArrZ<XY>>>& eq) {

    Coeff64 sum = 0;

    for (const auto& kv : eq) {

        const auto coeff = kv.second;

        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff(XY::X);
        const auto y_coeff = arg.coeff(XY::Y);

        sum += math::abs(coeff) * (math::abs(x_coeff) + math::abs(y_coeff));
    }

    return sum;
}

template <template <typename> class Trig>
EqVec<Trig> map_to_vec(const Equation<Trig>& eq) {

    EqVec<Trig> vec{};
    for (const auto& kv : eq) {

        const auto x_coeff = kv.first.arg.coeff(XY::X);
        const auto y_coeff = kv.first.arg.coeff(XY::Y);

        const math::LinComArr<XY, Coeff16> arg{boost::numeric_cast<Coeff16>(x_coeff),
                                               boost::numeric_cast<Coeff16>(y_coeff)};
        const Trig<math::LinComArr<XY, Coeff16>> trig{arg};

        vec.coeffs.emplace_back(trig, boost::numeric_cast<Coeff16>(kv.second));
    }

    return vec;
}

class Triple final {
  public:
    CodeSequence stable_neg;
    CodeSequence unstable;
    CodeSequence stable_pos;

    explicit Triple(const CodeSequence& stable_neg_, const CodeSequence& unstable_, const CodeSequence& stable_pos_)
        : stable_neg{stable_neg_}, unstable{unstable_}, stable_pos{stable_pos_} {}

    friend bool operator==(const Triple& lhs, const Triple& rhs) {
        return std::tie(lhs.stable_neg, lhs.unstable, lhs.stable_pos) == std::tie(rhs.stable_neg, rhs.unstable, rhs.stable_pos);
    }

    friend bool operator<(const Triple& lhs, const Triple& rhs) {
        return std::tie(lhs.stable_neg, lhs.unstable, lhs.stable_pos) < std::tie(rhs.stable_neg, rhs.unstable, rhs.stable_pos);
    }

    friend std::ostream& operator<<(std::ostream& os, const Triple& triple) {
        return os << triple.stable_neg << ", " << triple.unstable << ", " << triple.stable_pos;
    }
};

class HalfTriple final {
  public:
    CodeSequence stable_neg;
    CodeSequence unstable;

    explicit HalfTriple(const CodeSequence& stable_neg_, const CodeSequence& unstable_)
        : stable_neg{stable_neg_}, unstable{unstable_} {};

    friend bool operator==(const HalfTriple& lhs, const HalfTriple& rhs) {
        return std::tie(lhs.stable_neg, lhs.unstable) == std::tie(rhs.stable_neg, rhs.unstable);
    }

    friend bool operator<(const HalfTriple& lhs, const HalfTriple& rhs) {
        return std::tie(lhs.stable_neg, lhs.unstable) < std::tie(rhs.stable_neg, rhs.unstable);
    }

    friend std::ostream& operator<<(std::ostream& os, const HalfTriple& triple) {
        return os << triple.stable_neg << ", " << triple.unstable;
    }
};


class CodeInfo final {
  public:
    std::vector<PointQ> points;
    std::set<Equation<Sin>> sin_equations;
    std::set<Equation<Cos>> cos_equations;

    explicit CodeInfo(const std::vector<PointQ>& points_,
                      const std::set<Equation<Sin>>& sin_equations_,
                      const std::set<Equation<Cos>>& cos_equations_)
        : points{points_},
          sin_equations{sin_equations_},
          cos_equations{cos_equations_} {}

    explicit CodeInfo() {}
};

struct StableInfo {
    OpenConvexPolygonQ polygon;
    std::vector<std::pair<EqVec<Sin>, Coeff64>> sines;
    std::vector<std::pair<EqVec<Cos>, Coeff64>> cosines;

    explicit StableInfo(const CodeInfo& code_info)
        // IMPORTANT: some mrr polygons are not strictly convex based on the parsing
        // So we don't check them here.
        : polygon{code_info.points, geometry::Check::Unchecked} {

        for (const auto& equation : code_info.sin_equations) {
            const auto gbound = gradient_bound(equation);
            sines.emplace_back(map_to_vec(equation), gbound);
        }

        for (const auto& equation : code_info.cos_equations) {
            const auto gbound = gradient_bound(equation);
            cosines.emplace_back(map_to_vec(equation), gbound);
        }
    }
};

struct UnstableInfo {
    OpenSegmentQ segment;
    std::vector<std::pair<EqVec<Sin>, Coeff64>> sines;
    std::vector<std::pair<EqVec<Cos>, Coeff64>> cosines;

    explicit UnstableInfo(const CodeInfo& code_info)
        : segment{code_info.points.at(0), code_info.points.at(1)} {

        for (const auto& equation : code_info.sin_equations) {
            const auto gbound = gradient_bound(equation);
            sines.emplace_back(map_to_vec(equation), gbound);
        }

        for (const auto& equation : code_info.cos_equations) {
            const auto gbound = gradient_bound(equation);
            cosines.emplace_back(map_to_vec(equation), gbound);
        }
    }
};

struct TripleInfo {
    StableInfo stable_neg_info;
    UnstableInfo unstable_info;
    StableInfo stable_pos_info;

    explicit TripleInfo(const StableInfo& stable_neg_info_, const UnstableInfo& unstable_info_, const StableInfo& stable_pos_info_)
        : stable_neg_info{stable_neg_info_}, unstable_info{unstable_info_}, stable_pos_info{stable_pos_info_} {}
};

struct HalfTripleInfo {
    StableInfo stable_neg_info;
    UnstableInfo unstable_info;

    explicit HalfTripleInfo(const StableInfo& stable_neg_info_, const UnstableInfo& unstable_info_)
        : stable_neg_info{stable_neg_info_}, unstable_info{unstable_info_} {}
};


void remove_factor(CodeInfo& stable_neg_info, const CodePair& unstable, CodeInfo& stable_pos_info);
std::pair<bool, bool> remove_factor_duplicate_stables(CodeInfo& stable_neg_info, const CodePair& unstable, CodeInfo& stable_pos_info, const bool show);
void remove_factor_duplicate_stables(CodeInfo& stable_neg_info, const CodePair& unstable);
bool remove_factor_duplicate_stables_2(CodeInfo& stable_neg_info, const CodePair& unstable);
void remove_factor(CodeInfo& stable_neg_info, const CodePair& unstable);

uint32_t digits_to_bits(const uint32_t digits);

// the first is if the square was
bool is_positive(const StableInfo& info, const ClosedRectangleQ& square, const PointQ& center, const Rational& radius, Evaluator& eval);

bool is_positive(const TripleInfo& info, const ClosedRectangleQ& square, const PointQ& center, const Rational& radius, Evaluator& eval);

bool is_positive(const HalfTripleInfo& info, const ClosedRectangleQ& square, const PointQ& center, const Rational& radius, Evaluator& eval);

bool is_positive_unstable(const HalfTripleInfo& info, const ClosedRectangleQ& square, const PointQ& center, const Rational& radius, Evaluator& eval);

Integer get_cost(const StableInfo& info);

struct CoverAll final : public boost::static_visitor<cover::Cover> {

    const ClosedRectangleQ& square;
    const ClosedConvexPolygonQ& polygon;
    const std::map<SinglePair, StableInfo>& stable_infos;
    const std::map<TriplePair, TripleInfo>& triple_infos;
    const std::map<HalfTriplePair, HalfTripleInfo> half_triple_infos;
    const uint32_t prec;
    const uint32_t extra_depth;

    explicit CoverAll(const ClosedRectangleQ& square_, const ClosedConvexPolygonQ& polygon_,
                      const std::map<SinglePair, StableInfo>& stable_infos_,
                      const std::map<TriplePair, TripleInfo>& triple_infos_,
                      const uint32_t prec_, const uint32_t extra_depth_)
        : square{square_}, polygon{polygon_}, stable_infos{stable_infos_}, triple_infos{triple_infos_}, prec{prec_}, extra_depth{extra_depth_}, half_triple_infos{} {}

    explicit CoverAll(const ClosedRectangleQ& square_, const ClosedConvexPolygonQ& polygon_,
                      const std::map<SinglePair, StableInfo>& stable_infos_,
                      const std::map<TriplePair, TripleInfo>& triple_infos_,
                      const std::map<HalfTriplePair, HalfTripleInfo>& half_triple_infos_,
                      const uint32_t prec_, const uint32_t extra_depth_)
        : square{square_}, polygon{polygon_}, stable_infos{stable_infos_}, triple_infos{triple_infos_}, prec{prec_}, extra_depth{extra_depth_}, half_triple_infos{half_triple_infos_} {}

    cover::Cover operator()(const cover::Empty) const;

    cover::Cover operator()(const cover::Single& single) const;

    cover::Cover operator()(const cover::Triple& triple) const;

    cover::Cover operator()(const cover::HalfTriple& triple) const;

    cover::Cover operator()(const cover::Divide& divide) const;
};

struct CoverInfo final {
    std::vector<ClosedRectangleQ> not_filled;
    std::map<SinglePair, uint64_t> single_square_count; // map from code pair to number of squares it colored in
    std::map<TriplePair, uint64_t> triple_square_count; // map from triple pair to number of squares it colored in
    std::map<HalfTriplePair, uint64_t> half_triple_square_count;
    uint32_t deepest;

    explicit CoverInfo(const uint32_t deep)
        : not_filled{}, single_square_count{}, triple_square_count{}, half_triple_square_count{}, deepest{deep} {}
};

struct CoverVisitor final : public boost::static_visitor<> {

    CoverInfo& cover_info;
    const ClosedConvexPolygonQ& polygon;
    const ClosedRectangleQ& square;
    const uint32_t depth;

    explicit CoverVisitor(CoverInfo& cover_info_, const ClosedConvexPolygonQ& polygon_, const ClosedRectangleQ& square_, const uint32_t depth_)
        : cover_info{cover_info_}, polygon{polygon_}, square{square_}, depth{depth_} {}

    void operator()(const cover::Empty) {
        cover_info.deepest = falgo::max(cover_info.deepest, depth);

        if (geometry::intersects(square, polygon)) {
            // this is a hole, so not filled in
            cover_info.not_filled.push_back(square);
        }
    }

    void operator()(const cover::Single& stable_covered) {
        cover_info.deepest = falgo::max(cover_info.deepest, depth);

        // This will default construct to 0 if it isn't in the map yet.
        cover_info.single_square_count[stable_covered.single_pair] += 1;
    }

    void operator()(const cover::Triple& triple_covered) {
        cover_info.deepest = falgo::max(cover_info.deepest, depth);

        cover_info.triple_square_count[triple_covered.triple_pair] += 1;
    }
    void operator()(const cover::HalfTriple& triple_covered) {
            cover_info.deepest = falgo::max(cover_info.deepest, depth);

            cover_info.half_triple_square_count[triple_covered.half_triple_pair] += 1;
        }


    void operator()(const cover::Divide& sub) {

        const auto quarters = subdivide(square);

        for (size_t i = 0; i < quarters.size(); ++i) {
            const auto& quarter_square = quarters.at(i);
            const auto& quarter_cover = sub.quarters.get().at(i);

            CoverVisitor cover_visitor{cover_info, polygon, quarter_square, depth + 1};

            boost::apply_visitor(cover_visitor, quarter_cover);
        }
    }
};

CoverInfo cover_to_info(const ClosedConvexPolygonQ& polygon, const ClosedRectangleQ& square, const cover::Cover& cover);

std::string center_degrees(const ClosedRectangleQ& rect);

// the map is code sequence -> index should be in file
// because this is an ordered map, the index is just for quick lookup
// We can ignore the counts here
template <typename T>
std::map<T, size_t> get_index_info(const std::map<T, uint64_t>& counts) {

    // The order that the codes are passed in is arbirary. It only matters for
    // trying to optimize how a cover is found. Once you've found a cover though, it is simplest
    // to put the codes in sorted order when they're going in the file

    std::map<T, size_t> indices{};

    size_t index = 0;
    for (const auto& kv : counts) {
        indices.emplace(kv.first, index);
        ++index;
    }

    return indices;
}

template <typename Func>
bool cover_square_all(const std::string& mrr_dir, const std::string& all_dir, const Func& load, const uint32_t extra_depth) {

    auto file = open_file_write(all_dir + "/info.txt");

    const auto print_time = [&](const std::string& msg, const auto& begin, const auto& end) {
        const auto diff = end - begin;

        const auto hours = std::chrono::duration_cast<std::chrono::hours>(diff).count();
        const auto minutes = std::chrono::duration_cast<std::chrono::minutes>(diff).count() % 60;
        const auto seconds = std::chrono::duration_cast<std::chrono::seconds>(diff).count() % 60;
        const auto micros = std::chrono::duration_cast<std::chrono::microseconds>(diff).count() % 1000;

        std::ostringstream oss{};
        if (hours != 0) {
            oss << hours << "h ";
        }

        if (minutes != 0) {
            oss << minutes << "m ";
        }

        oss << seconds << '.' << micros << 's';

        std::cout << msg << oss.str() << std::endl;
        file << "// " << msg << oss.str() << '\n';
    };

    std::cout << "Beginning cover check" << std::endl;

    const auto t0 = std::chrono::steady_clock::now();

    const auto polygon = cover::load_polygon(mrr_dir);
    const auto square = cover::load_square(mrr_dir);
    const auto mrr_stables = cover::load_singles(mrr_dir);
    const auto mrr_triples = cover::load_triples(mrr_dir);

    const auto mrr_cover = cover::load_cover(mrr_dir, mrr_stables, mrr_triples);
    const auto digits = cover::load_digits(mrr_dir);

    const auto prec = digits_to_bits(digits);

    const auto t1 = std::chrono::steady_clock::now();

    print_time("Parsing MRR input: ", t0, t1);

    std::map<SinglePair, StableInfo> stable_infos{};
    std::map<TriplePair, TripleInfo> triple_infos{};

    load(stable_infos, triple_infos, mrr_stables, mrr_triples);

    const auto t2 = std::chrono::steady_clock::now();

    print_time("Loading equations: ", t1, t2);

    // Now we do the thing here

    const CoverAll verifier{square, polygon, stable_infos, triple_infos, prec, extra_depth};
    const auto all_cover = boost::apply_visitor(verifier, mrr_cover);

    const auto t3 = std::chrono::steady_clock::now();

    print_time("Calculating cover: ", t2, t3);

#if 0
    for (const auto& vertex : polygon) {
        std::cout << rational_to_degrees(vertex.x) << ' ' << rational_to_degrees(vertex.y) << std::endl;
        file << "// " << rational_to_degrees(vertex.x) << ' ' << rational_to_degrees(vertex.y) << '\n';
    }
#endif

    const auto cover_info = cover_to_info(polygon, square, all_cover);

    std::cout << "The following stables colored squares:" << std::endl;
    file << "// The following stables colored squares:" << '\n';
    for (const auto& kv : cover_info.single_square_count) {

        const auto& p = kv.first;
        const auto& stable = p.stable.get().sequence;
        const auto count = kv.second;
        //const auto cost = stable_infos.at(i).second.cost;

        const auto code_type = stable.type();
        const auto code_length = stable.length();
        const auto code_sum = stable.sum();

        //const auto s = str(boost::format("%1% (%2%, %3%) (%4%, %5%) - %6%") % code_type % code_length % code_sum % cost % count % stable);
        const auto s = str(boost::format("%1% (%2%, %3%) (%4%) - %5%") % code_type % code_length % code_sum % count % stable);

        std::cout << s << std::endl;
        file << s << '\n';
    }

    std::cout << "The following triples colored squares:" << std::endl;
    file << "// The following triples colored squares:" << '\n';
    for (const auto& kv : cover_info.triple_square_count) {

        const auto& p = kv.first;

        std::cout << '(' << kv.second << ") - "
                  << p.stable_neg.get().sequence << ", "
                  << p.unstable.get().sequence << ", "
                  << p.stable_pos.get().sequence << std::endl;
        file << '(' << kv.second << ") - "
             << p.stable_neg.get().sequence << ", "
             << p.unstable.get().sequence << ", "
             << p.stable_pos.get().sequence << '\n';
    }

    const auto covered = cover_info.not_filled.empty();

    std::cout << cover_info.not_filled.size() << " squares were not filled in" << std::endl;
    file << "// " << cover_info.not_filled.size() << " squares were not filled in" << '\n';

    constexpr size_t empties = 10;

    const size_t num_to_print = falgo::min(cover_info.not_filled.size(), empties);
    if (num_to_print != 0) {
        const size_t inc = cover_info.not_filled.size() / num_to_print;
        for (size_t i = 0; i < num_to_print * inc; i += inc) {
            std::cout << center_degrees(cover_info.not_filled.at(i)) << std::endl;
            file << "// " << center_degrees(cover_info.not_filled.at(i)) << '\n';
        }
    }

    uint64_t stable_squares = 0;
    for (const auto& p : cover_info.single_square_count) {
        stable_squares += p.second;
    }

    uint64_t triple_squares = 0;
    for (const auto& p : cover_info.triple_square_count) {
        triple_squares += p.second;
    }

    std::cout << stable_squares << " stable squares used in the cover" << std::endl;
    file << "// " << stable_squares << " stable squares used in the cover" << '\n';

    std::cout << triple_squares << " triple squares used in the cover" << std::endl;
    file << "// " << triple_squares << " triple squares used in the cover" << '\n';

    std::cout << cover_info.single_square_count.size() << " stables used in the cover" << std::endl;
    file << cover_info.single_square_count.size() << " stables used in the cover" << '\n';

    std::cout << cover_info.triple_square_count.size() << " triples used in the cover" << std::endl;
    file << cover_info.triple_square_count.size() << " triples used in the cover" << '\n';

    std::cout << boost::format("ALL at %1% decimals, deepest magnification %2%") % digits % cover_info.deepest << std::endl;
    file << boost::format("ALL at %1% decimals, deepest magnification %2%") % digits % cover_info.deepest << '\n';

#if 0
    Integer total_stable_cost = 0;
    for (const auto i : used.first) {
        total_stable_cost += cover_info.single_square_count.at(i) * stable_infos.at(i).second.cost;
    }

    std::cout << "Total stable cost: " << total_stable_cost << std::endl;
    file << "// Total stable cost: " << total_stable_cost << '\n';
#endif

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

    const auto stable_index_info = get_index_info(cover_info.single_square_count);
    const auto triple_index_info = get_index_info(cover_info.triple_square_count);

    cover::save_polygon(all_dir, polygon);
    cover::save_square(all_dir, square);
    cover::save_singles(all_dir, stable_index_info);
    cover::save_triples(all_dir, triple_index_info);
    cover::save_cover(all_dir, all_cover, stable_index_info, triple_index_info);
    cover::save_digits(all_dir, digits);

    const auto t4 = std::chrono::steady_clock::now();

    print_time("Saving output: ", t3, t4);

    print_time("Total time elapsed: ", t0, t4);

    return covered;
}


bool is_positive_corner(const HalfTripleInfo& info, const HalfTripleInfo& info2 , const ClosedRectangleQ& square, const PointQ& center, const Rational& radius, Evaluator& eval);
