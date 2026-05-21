// george jun11th 2021 to print the shooting vector, uncomment the lines with label_shooting_vector, (Note: 15 lines in total)

#include "database.hpp"
#include "bounding_region.hpp"
#include "conversion.hpp"
#include "database/deserialize.hpp"
#include "database/serialize.hpp"
#include "shooting_vectors.hpp"
#include "unfolding.hpp"

// Should we sort the all vertices in a particular order when we put them in the database?

// The equations will all be separated by newlines
static std::pair<std::set<Equation<Sin>>, std::set<Equation<Cos>>> parse_mrr_equations(const std::string& equations_str) {

    std::pair<std::set<Equation<Sin>>, std::set<Equation<Cos>>> eqs{};

    const auto lines = split(equations_str, "\n");

    for (const auto& line : lines) {

        if (boost::starts_with(line, "sin ")) {

            const auto sub = line.substr(4);

            const auto eq = database::deserialize<Equation<Sin>>(sub);

            eqs.first.insert(eq);

        } else if (boost::starts_with(line, "cos ")) {

            const auto sub = line.substr(4);

            const auto eq = database::deserialize<Equation<Cos>>(sub);

            eqs.second.insert(eq);
        } else {
            throw std::runtime_error("unable to parse equation: " + line);
        }
    }

    return eqs;
}

// TODO it would be better to use the bounding polygon here instead. Oh well
static std::vector<PointQ> parse_mrr_polygon(const std::string& points_str) {

    std::vector<PointQ> vertices{};

    const auto lines = split(points_str, "\n");

    const auto half_pi = boost::math::constants::half_pi<Float>();

    for (const auto& line : lines) {

        const auto coords = split(line, " ");

        const auto x_flt = boost::lexical_cast<Float>(coords.at(0)) / half_pi;
        const auto y_flt = boost::lexical_cast<Float>(coords.at(1)) / half_pi;

        const Rational x{x_flt};
        const Rational y{y_flt};

        vertices.emplace_back(x, y);
    }

    return vertices;
}

// Currently unused because we calculated the line segment on demand
#if 0
static OpenSegmentQ parse_mrr_line_segment(const std::string& points_str) {

    std::vector<PointQ> vertices{};

    const auto lines = split(points_str, "\n");

    const auto half_pi = boost::math::constants::half_pi<Float>();

    for (const auto& line : lines) {

        const auto coords = split(line, " ");

        const auto x_flt = boost::lexical_cast<Float>(coords.at(0)) / half_pi;
        const auto y_flt = boost::lexical_cast<Float>(coords.at(1)) / half_pi;

        const Rational x{x_flt};
        const Rational y{y_flt};

        vertices.emplace_back(x, y);
    }

    if (vertices.size() != 2) {
        throw std::runtime_error("vertices do not have size 2 in parse_mrr_line_segment");
    }

    return OpenSegmentQ{vertices.at(0), vertices.at(1)};
}
#endif

// Right now we have to use the bounding line segment, because that actually works correctly
std::vector<PointQ> bounding_line_segment(const CodeSequence& code_sequence, const InitialAngles& initial_angles) {

    const auto code_angles = code_sequence.angles(initial_angles.first, initial_angles.second);

    const auto constraint = code_sequence.constraint(initial_angles.first, initial_angles.second);
    //std::cout << code_angles <<std::endl;
    const auto lseg = calculate_bounding_line_segment(code_sequence.numbers(), code_angles, constraint);

    return {lseg->point0, lseg->point1};
}

std::vector<PointQ> bounding_polygon(const CodeSequence& code_sequence, const InitialAngles& initial_angles) {

    const auto code_angles = code_sequence.angles(initial_angles.first, initial_angles.second);

    const auto poly = calculate_bounding_polygon(code_sequence.numbers(), code_angles);

    const auto get_vertices = [](const auto& rat_pair) {
        return rat_pair.point;
    };

    const auto points = falgo::transform(*poly, get_vertices);

    return points;
}

static std::pair<InitialAngles, CodeInfo> load_stable_mrr_from_database(const CodeSequence& code_sequence, sqlite::Database& db) {

    const auto sql = "select initial_angles, points, equations from " + table_name(code_sequence) + " where code_sequence = ?;";

    std::string initial_angles_str{};
    std::string points_str{};
    std::string equations_str{};

    db.prepare(sql)
        .bind(database::serialize(code_sequence))
        .exec(initial_angles_str, points_str, equations_str);

    const auto initial_angles = database::deserialize<InitialAngles>(initial_angles_str);
    const auto mrr_polygon = parse_mrr_polygon(points_str);
    const auto mrr_eqs = parse_mrr_equations(equations_str);
    
    return {initial_angles, CodeInfo{mrr_polygon, mrr_eqs.first, mrr_eqs.second}};
}

static std::pair<InitialAngles, CodeInfo> load_unstable_mrr_from_database(const CodeSequence& code_sequence, sqlite::Database& db) {

    const auto sql = "select initial_angles, equations from " + table_name(code_sequence) + " where code_sequence = ?;";

    std::string initial_angles_str{};
    std::string equations_str{};

    db.prepare(sql)
        .bind(database::serialize(code_sequence))
        .exec(initial_angles_str, equations_str);

    const auto initial_angles = database::deserialize<InitialAngles>(initial_angles_str);
    // We can't use the MRR line segment due to rounding issues. This is good for now
    const auto line_segment = bounding_line_segment(code_sequence, initial_angles);
    const auto mrr_eqs = parse_mrr_equations(equations_str);

    return {initial_angles, CodeInfo{line_segment, mrr_eqs.first, mrr_eqs.second}};
}

// Same for stable and unstable
static std::pair<InitialAngles, CodeInfo> load_all_from_database(const CodeSequence& code_sequence, sqlite::Database& db) {

    const std::string sql = "select initial_angles, polygon, sin_equations, cos_equations from " +
                            table_name(code_sequence) + " where code_sequence = ?;";

    std::string initial_angles_str{};
    std::string polygon_str{};
    std::string sin_equations_str{};
    std::string cos_equations_str{};
    db.prepare(sql)
        .bind(database::serialize(code_sequence))
        .exec(initial_angles_str, polygon_str, sin_equations_str, cos_equations_str);

    const auto initial_angles = database::deserialize<InitialAngles>(initial_angles_str);
    const auto points = database::deserialize<std::vector<PointQ>>(polygon_str);
    const auto sin_eqs = database::deserialize<std::set<Equation<Sin>>>(sin_equations_str);
    const auto cos_eqs = database::deserialize<std::set<Equation<Cos>>>(cos_equations_str);

    return {initial_angles, CodeInfo{points, sin_eqs, cos_eqs}};
}

// TODO change this so we do the polygon trimming when finding these
// That would require a database refresh for the all equations
// When we do this, we can simply copy over the calculate_stable_all_info from the cover/cpp/db.cpp file

CodeInfo calculate_stable_all_info(const CodeSequence& code_sequence, const InitialAngles& initial_angles) {

    const auto code_angles = code_sequence.angles(initial_angles.first, initial_angles.second);

    // TODO try zipping the code numbers and code angles together. That might make the
    // calculations and such easier
    const auto& code_numbers = code_sequence.numbers();

    // TODO throw this in a calculate_polygon function
    const auto rational_polygon = calculate_bounding_polygon(code_numbers, code_angles);

    const auto get_vertices = [](const auto& rat_pair) {
        return rat_pair.point;
    };

    const auto points = falgo::transform(*rational_polygon, get_vertices);

    // TODO throw this in a calculate_curves function
    const auto code_angles_eta = falgo::transform(code_angles, xyz_to_xyeta);
    const auto code_angles_pi = falgo::transform(code_angles, xyz_to_xypi);

    const auto code_type = code_sequence.type();

    const Unfolding unfold{code_numbers, code_angles};

    // Some of the generated equations are duplicates, so make sure they are in a std::set
    // TODO we could do some sort of rigourous refine as the equations are generated. That
    // would be better than generate them all first and then filtering
    // Another issue is the integer size. Could we use int32_t to save space?
    Curves equations{};
    if (code_type == CodeType::OSO) {

        const auto shooting_vector = shooting_vector_open(code_sequence, code_angles_pi);
        equations = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles);



    } else if (code_type == CodeType::CS) {

        const auto shooting_vector = shooting_vector_closed(code_sequence, code_angles_eta);
        equations = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles);



    } else if (code_type == CodeType::OSNO) {

        const auto shooting_vector = unfold.shooting_vector_general();
        equations = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles);

    } else {
        std::ostringstream err{};
        err << "unstable code " << code_sequence << " of type "
            << code_type << " passed to calculate_stable_all_info";
        throw std::runtime_error(err.str());
    }

    return CodeInfo{points, equations.first, equations.second};
}

CodeInfo calculate_unstable_all_info(const CodeSequence& code_sequence, const InitialAngles& initial_angles) {

    const auto code_angles = code_sequence.angles(initial_angles.first, initial_angles.second);

    // TODO try zipping the code numbers and code angles together. That might make the
    // calculations and such easier
    const auto& code_numbers = code_sequence.numbers();

    const auto constraint = code_sequence.constraint(initial_angles.first, initial_angles.second);

    // TODO throw this in a calculate_bounding_line_segment function
    const auto rational_segment = calculate_bounding_line_segment(code_numbers, code_angles, constraint);

    const std::vector<PointQ> points{rational_segment->point0, rational_segment->point1};

    // TODO throw this in a calculate_curves function
    const auto code_angles_eta = falgo::transform(code_angles, xyz_to_xyeta);
    const auto code_angles_pi = falgo::transform(code_angles, xyz_to_xypi);

    const auto code_type = code_sequence.type();

    const Unfolding unfold{code_numbers, code_angles};

    // Some of the generated equations are duplicates, so make sure they are in a std::set
    // TODO we could do some sort of rigourous refine as the equations are generated. That
    // would be better than generate them all first and then filtering
    // Another issue is the integer size. Could we use int32_t to save space?
    Curves curves{};

    if (code_type == CodeType::CNS) {

        const auto shooting_vector = shooting_vector_closed(code_sequence, code_angles_eta);
        curves = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles);

    } else if (code_type == CodeType::ONS) {

        const auto shooting_vector = unfold.shooting_vector_general();
        curves = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles);

    } else {
        std::ostringstream err{};
        err << "stable code " << code_sequence << " of type "
            << code_type << " passed to calculate_unstable_all_info";
        throw std::runtime_error(err.str());
    }
    return CodeInfo{points, curves.first, curves.second};
}

CodeInfo calculate_all_info(const CodeSequence& code_sequence, const InitialAngles& initial_angles) {
    const auto code_angles = code_sequence.angles(initial_angles.first, initial_angles.second);

    // TODO try zipping the code numbers and code angles together. That might make the
    // calculations and such easier
    const auto& code_numbers = code_sequence.numbers();



    // TODO throw this in a calculate_curves function
    const auto code_angles_eta = falgo::transform(code_angles, xyz_to_xyeta);
    const auto code_angles_pi = falgo::transform(code_angles, xyz_to_xypi);

    const auto code_type = code_sequence.type();

    const Unfolding unfold{code_numbers, code_angles};

    // Some of the generated equations are duplicates, so make sure they are in a std::set
    // TODO we could do some sort of rigourous refine as the equations are generated. That
    // would be better than generate them all first and then filtering
    // Another issue is the integer size. Could we use int32_t to save space?
    CurvesLR equations{};
    // george jun11th 2021 to print the shooting vector, uncomment the lines with label_shooting_vector
    if (code_type == CodeType::OSO) {

        const auto shooting_vector = shooting_vector_open(code_sequence, code_angles_pi);
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        equations = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second);

    } else if (code_type == CodeType::CS) {

        const auto shooting_vector = shooting_vector_closed(code_sequence, code_angles_eta);
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        equations = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second);

    } else if (code_type == CodeType::OSNO) {

        const auto shooting_vector = unfold.shooting_vector_general();
        //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
        equations = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second);

    }

    else if (code_type == CodeType::CNS) {

            const auto shooting_vector = shooting_vector_closed(code_sequence, code_angles_eta);
            //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
            equations = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second);

        } else if (code_type == CodeType::ONS) {

            const auto shooting_vector = unfold.shooting_vector_general();
            //std::cout << shooting_vector.first << " , " << shooting_vector.second << std::endl; //label_shooting_vector
            equations = unfold.generate_curves_lr(shooting_vector.first, shooting_vector.second);

        }

    else {
        std::ostringstream err{};
        err << "unstable code " << code_sequence << " of type "
            << code_type << " passed to calculate_stable_all_info";
        throw std::runtime_error(err.str());
    }
    const std::vector<PointQ> points {};

    std::set<Equation<Sin>> sin_set {};
    for(std::map<Equation<Sin>, std::vector<LeftRight>>::iterator it = equations.first.begin(); it != equations.first.end(); ++it) {
      sin_set.insert(it->first);
    }

    std::set<Equation<Cos>> cos_set {};
    for(std::map<Equation<Cos>, std::vector<LeftRight>>::iterator it = equations.second.begin(); it != equations.second.end(); ++it) {
      cos_set.insert(it->first);
    }

    return CodeInfo{ points, sin_set, cos_set};
}

CodeInfo calculate_all_vector(const CodeSequence& code_sequence, const InitialAngles& initial_angles) {
    const auto code_angles = code_sequence.angles(initial_angles.first, initial_angles.second);

    const auto& code_numbers = code_sequence.numbers();
    const auto code_type = code_sequence.type();

    const Unfolding unfold{code_numbers, code_angles};

    const std::vector<PointQ> points {};
    std::set<Equation<Sin>> sin_set {};
    std::set<Equation<Cos>> cos_set {};
    std::set<std::pair<Equation<Sin>, Equation<Cos>>> vector_set =  unfold.get_all_vectors();

    for(std::pair<Equation<Sin>, Equation<Cos>> pair : vector_set){
        sin_set.insert(pair.first);
        cos_set.insert(pair.second);
    }

    return CodeInfo{points, sin_set, cos_set};
}

static void save_to_database(const CodeSequence& code_sequence, const CodeInfo& info, sqlite::Database& db) {

    const std::string sql = "update " + table_name(code_sequence) + " set polygon = ?, sin_equations = ?, cos_equations = ? where code_sequence = ?;";

    db.prepare(sql)
        .bind(database::serialize(info.points),
              database::serialize(info.sin_equations),
              database::serialize(info.cos_equations),
              database::serialize(code_sequence))
        .exec();
}

// TODO the following two functions could be simplified
static void ensure_stable_all_in_database(const CodeSequence& code_sequence, sqlite::Database& db) {

    const std::string sql = "select initial_angles, polygon from " + table_name(code_sequence) + " where code_sequence = ?;";

    std::string initial_angles_str{};
    std::string polygon_str{};

    db.prepare(sql)
        .bind(database::serialize(code_sequence))
        .exec(initial_angles_str, polygon_str);

    if (polygon_str.empty()) {
        std::cout << "Calculating all equations for " << code_sequence << std::endl;
        const auto initial_angles = database::deserialize<InitialAngles>(initial_angles_str);
        const auto info = calculate_stable_all_info(code_sequence, initial_angles);
       // std::cout << "All sin equations: " << database::serialize(info.sin_equations) << std::endl;//george aug 30.2021
       // std::cout << "All cos equations: " << database::serialize(info.cos_equations) << std::endl;//turns off the unstables in ALL


        save_to_database(code_sequence, info, db);
    }
}

static void ensure_unstable_all_in_database(const CodeSequence& code_sequence, sqlite::Database& db) {

    const std::string sql = "select initial_angles, polygon from " + table_name(code_sequence) + " where code_sequence = ?;";

    std::string initial_angles_str{};
    std::string polygon_str{};

    db.prepare(sql)
        .bind(database::serialize(code_sequence))
        .exec(initial_angles_str, polygon_str);

    if (polygon_str.empty()) {
        std::cout << "Calculating all equations for " << code_sequence << std::endl;
        const auto initial_angles = database::deserialize<InitialAngles>(initial_angles_str);
        const auto info = calculate_unstable_all_info(code_sequence, initial_angles);

        save_to_database(code_sequence, info, db);
    }
}
std::pair<InitialAngles, CodeInfo> get_stable_info(const CodeSequence& code_sequence, const bool mrr, sqlite::Database& db) {

    if (mrr) {
        return load_stable_mrr_from_database(code_sequence, db);
    } else {
        // all
        ensure_stable_all_in_database(code_sequence, db);
        return load_all_from_database(code_sequence, db);
    }
}

static std::pair<InitialAngles, CodeInfo> get_unstable_info(const CodeSequence& code_sequence, const bool mrr, sqlite::Database& db) {

    if (mrr) {
        return load_unstable_mrr_from_database(code_sequence, db);
    } else {
        // all
        ensure_unstable_all_in_database(code_sequence, db);
        return load_all_from_database(code_sequence, db);
    }
}

static std::pair<SinglePair, StableInfo> get_single_info(const CodeSequence& code_seq, const bool mrr, sqlite::Database& db) {

    const auto info = get_stable_info(code_seq, mrr, db);

    const SharedPtr<CodePair> single_pair{code_seq, info.first};

    return {SinglePair{single_pair}, StableInfo{info.second}};
}

std::vector<std::pair<SinglePair, StableInfo>> get_single_infos(const std::set<CodeSequence>& code_seqs, const bool mrr, sqlite::Database& db) {

    std::vector<std::pair<SinglePair, StableInfo>> stable_infos{};
    for (const auto& code_seq : code_seqs) {
        stable_infos.push_back(get_single_info(code_seq, mrr, db));
    }

    std::map<SinglePair, Integer> costs{};
    for (const auto& p : stable_infos) {
        costs.emplace(p.first, get_cost(p.second));
    }

    // Sort the infos by cost

    const auto comp = [&](const auto& p0, const auto& p1) {
        const auto cost0 = costs.at(p0.first);
        const auto length0 = p0.first.stable.get().sequence.length();
        const auto sum0 = p0.first.stable.get().sequence.sum();

        const auto cost1 = costs.at(p1.first);
        const auto length1 = p1.first.stable.get().sequence.length();
        const auto sum1 = p1.first.stable.get().sequence.sum();

        return std::tie(cost0, length0, sum0) < std::tie(cost1, length1, sum1);
    };

    falgo::sort(stable_infos, comp);

    return stable_infos;
}

static std::pair<TriplePair, TripleInfo> get_triple_info(const Triple& triple, const bool mrr, sqlite::Database& db) {

    auto stable_neg_info = get_stable_info(triple.stable_neg, mrr, db);
    const auto unstable_info = get_unstable_info(triple.unstable, mrr, db);
    auto stable_pos_info = get_stable_info(triple.stable_pos, mrr, db);

    const SharedPtr<CodePair> stable_neg_pair{triple.stable_neg, stable_neg_info.first};
    const SharedPtr<CodePair> unstable_pair{triple.unstable, unstable_info.first};
    const SharedPtr<CodePair> stable_pos_pair{triple.stable_pos, stable_pos_info.first};

    remove_factor(stable_neg_info.second, unstable_pair.get(), stable_pos_info.second);

    const TriplePair triple_pair{stable_neg_pair, unstable_pair, stable_pos_pair};

    const TripleInfo triple_info{StableInfo{stable_neg_info.second},
                                 UnstableInfo{unstable_info.second},
                                 StableInfo{stable_pos_info.second}};         

    return {triple_pair, triple_info};
}


static std::pair<bool, bool> get_triple_info_duplicate_stables(const Triple& triple, const bool mrr, sqlite::Database& db, const bool show) {

    auto stable_neg_info = get_stable_info(triple.stable_neg, mrr, db);
    const auto unstable_info = get_unstable_info(triple.unstable, mrr, db);
    auto stable_pos_info = get_stable_info(triple.stable_pos, mrr, db);

    const SharedPtr<CodePair> stable_neg_pair{triple.stable_neg, stable_neg_info.first};
    const SharedPtr<CodePair> unstable_pair{triple.unstable, unstable_info.first};
    const SharedPtr<CodePair> stable_pos_pair{triple.stable_pos, stable_pos_info.first};

    return remove_factor_duplicate_stables(stable_neg_info.second, unstable_pair.get(), stable_pos_info.second, show);
}

static bool get_triple_info_half_duplicate_stables(const HalfTriple& triple, const bool mrr, sqlite::Database& db) {

    // DEBUG
    //std::cout << "get_triple_info_half" << std::endl;

    auto stable_neg_info = get_stable_info(triple.stable_neg, mrr, db);
    const auto unstable_info = get_unstable_info(triple.unstable, mrr, db);

    const SharedPtr<CodePair> stable_neg_pair{triple.stable_neg, stable_neg_info.first};
    const SharedPtr<CodePair> unstable_pair{triple.unstable, unstable_info.first};


    return remove_factor_duplicate_stables_2(stable_neg_info.second, unstable_pair.get());
}


std::vector<std::pair<TriplePair, TripleInfo>> get_triple_infos(const std::set<Triple>& triples, const bool mrr, sqlite::Database& db) {

    std::vector<std::pair<TriplePair, TripleInfo>> triple_infos{};

    for (const auto& triple : triples) {
        triple_infos.push_back(get_triple_info(triple, mrr, db));
    }

    // TODO should we sort the triples by cost too?

    return triple_infos;
}

std::pair<bool, bool> get_triple_infos_duplicate_stables(const std::set<Triple>& triples, const bool mrr, sqlite::Database& db, const bool show) {
    std::pair<bool, bool> Q{};
    for (const auto& triple : triples) {
        Q = get_triple_info_duplicate_stables(triple, mrr, db, show);
    }

    return Q;
}

bool get_triple_infos_half_duplicate_stables(const std::set<HalfTriple>& half_triples, const bool mrr, sqlite::Database& db) {
    bool temp = false;
    for (const auto& half_triple : half_triples) {
        temp = get_triple_info_half_duplicate_stables(half_triple, mrr, db);
    }
    return temp;
}

// Only difference is no sorting and return value
std::map<SinglePair, StableInfo> get_single_infos_map(const std::vector<CodePair>& code_seqs, const bool mrr, sqlite::Database& db) {

    std::map<SinglePair, StableInfo> stable_infos{};
    for (const auto& code_pair : code_seqs) {
        const auto info = get_single_info(code_pair.sequence, mrr, db);

        if (!(code_pair.angles == info.first.stable.get().angles)) {
            throw std::runtime_error("stable angles do not match");
        }

        stable_infos.insert(info);
    }

    return stable_infos;
}

std::map<TriplePair, TripleInfo> get_triple_infos_map(const std::vector<TriplePair>& triples, const bool mrr, sqlite::Database& db) {

    std::map<TriplePair, TripleInfo> triple_infos{};
    for (const auto& triple_pair : triples) {

        const Triple triple{triple_pair.stable_neg.get().sequence,
                            triple_pair.unstable.get().sequence,
                            triple_pair.stable_pos.get().sequence};

        const auto info = get_triple_info(triple, mrr, db);

        if (!(triple_pair == info.first)) {
            throw std::runtime_error("triple pairs do not match");
        }

        triple_infos.insert(info);
    }

    return triple_infos;
}
