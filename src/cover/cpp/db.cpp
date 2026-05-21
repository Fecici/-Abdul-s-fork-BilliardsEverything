
namespace database {

// TODO this is just the closure really
static ClosedSegmentQ bounding_segment(const OpenSegmentQ& seg) {

    return {seg.start(), seg.end()};
}

static ClosedRectangleQ bounding_rectangle(const OpenConvexPolygonQ& poly) {

    const auto x = project_x(poly);
    const auto y = project_y(poly);

    // The above x and y are open intervals, so we need to create new
    // closed ones
    return {{x.lower(), x.upper()}, {y.lower(), y.upper()}};
}

static PointQ center(const ClosedRectangleQ& rect) {
    return rect.center();
}

static Rational radius_x(const ClosedRectangleQ& rect) {
    return rect.width() / 2;
}

static Rational radius_y(const ClosedRectangleQ& rect) {
    return rect.height() / 2;
}

static PointQ center(const ClosedSegmentQ& seg) {
    return seg.midpoint();
}

static Rational radius_x(const ClosedSegmentQ& seg) {
    return abs(seg.start().x - seg.end().x) / 2;
}

static Rational radius_y(const ClosedSegmentQ& seg) {
    return abs(seg.start().y - seg.end().y) / 2;
}

// Old database code we don't use since we turned it off
#if 0
static void init_db(const std::string& path) {

    constexpr auto flags = sqlite::Open::Readwrite | sqlite::Open::Create;

    // TODO are there extra flags we want to open the database with,
    // or some other pragmas we want to execute?
    sqlite::Database db{path, flags};

    std::string locking_mode{};
    db.prepare("pragma locking_mode = exclusive;").bind().exec(locking_mode);
    if (locking_mode != "exclusive") {
        throw std::runtime_error("unable to set locking_mode; locking_mode = " + locking_mode);
    }

    // A prepare only prepares the first statement in the string
    std::string journal_mode{};
    db.prepare("pragma journal_mode = wal;").bind().exec(journal_mode);

    if (journal_mode != "wal") {
        throw std::runtime_error("unable to set wal; journal mode = " + journal_mode);
    }

    db.prepare("pragma synchronous = full;").bind().exec();

    int64_t synchronous{};
    db.prepare("pragma synchronous;").bind().exec(synchronous);

    if (synchronous != 2) {
        throw std::runtime_error("unable to set full; synchronous = " + std::to_string(synchronous));
    }

    const std::string schema = "create table if not exists %1%("
                               "code_sequence text check(typeof(code_sequence) = 'text'),"
                               "initial_angles text check(typeof(initial_angles) = 'text'),"
                               "points text check(typeof(points) = 'text'),"
                               "sin_equations text check(typeof(sin_equations) = 'text'),"
                               "cos_equations text check(typeof(cos_equations) = 'text'),"
                               "primary key (code_sequence, initial_angles)"
                               ");";

    const std::array<std::string, 5> tables{{"oso", "osno", "cs", "cns", "ons"}};

    for (const auto& table : tables) {
        const std::string sql = str(boost::format(schema) % table);
        db.prepare(sql).bind().exec();
    }
}

static bool in_database(const CodePair& code_pair, sqlite::Database& db) {

    const auto code_type = code_pair.sequence.type();

    const std::string sql = "select exists(select 1 from " + serialize(code_type) + " where code_sequence = ? and initial_angles = ?);";

    int64_t exists = 0;
    db.prepare(sql).bind(serialize(code_pair.sequence), serialize(code_pair.angles)).exec(exists);

    return exists;
}

static void save_code_info(const CodePair& code_pair, const CodeInfo& code_info, sqlite::Database& db) {

    const auto code_type = code_pair.sequence.type();

    const std::string sql = "insert into " + serialize(code_type) + "(code_sequence, initial_angles, points, sin_equations, cos_equations) values (?, ?, ?, ?, ?);";

    db.prepare(sql)
        .bind(
            serialize(code_pair.sequence),
            serialize(code_pair.angles),
            serialize(code_info.points),
            serialize(code_info.sin_equations),
            serialize(code_info.cos_equations))
        .exec();
}

static CodeInfo load_code_info(const CodePair& code_pair, sqlite::Database& db) {

    const auto code_type = code_pair.sequence.type();

    const std::string sql = "select points, sin_equations, cos_equations from " +
                            serialize(code_type) + " where code_sequence = ? and initial_angles = ?;";

    std::string points_str{};
    std::string sin_equations_str{};
    std::string cos_equations_str{};
    db.prepare(sql)
        .bind(serialize(code_pair.sequence), serialize(code_pair.angles))
        .exec(points_str, sin_equations_str, cos_equations_str);

    const auto points = deserialize<std::vector<PointQ>>(points_str);
    const auto sin_equations = deserialize<std::set<Equation<Sin>>>(sin_equations_str);
    const auto cos_equations = deserialize<std::set<Equation<Cos>>>(cos_equations_str);

    return CodeInfo{points, sin_equations, cos_equations};
}
#endif

static CodeInfo calculate_stable_code_info(const CodePair& code_pair, const CodeType& code_type) {

    const auto& code_sequence = code_pair.sequence;
    const auto& initial_angles = code_pair.angles;

    const auto code_angles = code_sequence.angles(initial_angles.first, initial_angles.second);

    // TODO try zipping the code numbers and code angles together. That might make the
    // calculations and such easier
    const auto& code_numbers = code_sequence.numbers();

    // TODO throw this in a calculate_polygon function
    const auto opt_polygon = calculate_bounding_polygon(code_numbers, code_angles);

    const auto get_vertices = [](const auto& rat_pair) {
        return rat_pair.point;
    };

    const auto vertices = falgo::transform(*opt_polygon, get_vertices);

    const OpenConvexPolygonQ polygon{vertices};

    const auto rect = bounding_rectangle(polygon);

    const auto c = center(rect);
    const auto rx = radius_x(rect);
    const auto ry = radius_y(rect);

    // TODO throw this in a calculate_curves function
    const Unfolding unfold{code_numbers, code_angles};

    // Some of the generated equations are duplicates, so make sure they are in a std::set
    // TODO we could do some sort of rigourous refine as the equations are generated. That
    // would be better than generate them all first and then filtering
    // Another issue is the integer size. Could we use int32_t to save space?
    Curves equations{};
    if (code_type == CodeType::OSO) {

        const auto code_angles_pi = falgo::transform(code_angles, xyz_to_xypi);

        const auto shooting_vector = shooting_vector_open(code_sequence, code_angles_pi);
        equations = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles, c, rx, ry);
        //equations = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles, polygon);

    } else if (code_type == CodeType::CS) {

        const auto code_angles_eta = falgo::transform(code_angles, xyz_to_xyeta);

        const auto shooting_vector = shooting_vector_closed(code_sequence, code_angles_eta);
        equations = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles, c, rx, ry);
        //equations = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles, polygon);

    } else if (code_type == CodeType::OSNO) {

        const auto shooting_vector = unfold.shooting_vector_general();
        equations = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles, c, rx, ry);
        //equations = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles, polygon);

    } else {
        std::ostringstream err{};
        err << "unstable code " << code_sequence << " of type "
            << code_type << " passed to calculate_stable_code_info";
        throw std::runtime_error(err.str());
    }

    return CodeInfo{vertices, equations.first, equations.second};
}

static CodeInfo calculate_unstable_code_info(const CodePair& code_pair, const CodeType code_type) {

    const auto& code_sequence = code_pair.sequence;
    const auto& initial_angles = code_pair.angles;

    const auto code_angles = code_sequence.angles(initial_angles.first, initial_angles.second);

    // TODO try zipping the code numbers and code angles together. That might make the
    // calculations and such easier
    const auto& code_numbers = code_sequence.numbers();

    const auto constraint = code_sequence.constraint(initial_angles.first, initial_angles.second);

    // TODO throw this in a calculate_bounding_line_segment function
    const auto rational_segment = calculate_bounding_line_segment(code_numbers, code_angles, constraint);
    std::cout << "rational_segment ";
    std::cout << rational_segment << std::endl;

    const std::vector<PointQ> segment{rational_segment->point0, rational_segment->point1};

    const OpenSegmentQ seg{segment.at(0), segment.at(1)};

    const auto closed_seg = bounding_segment(seg);
    //std::cout << "closed_segment ";
    //std::cout << closed_seg << std::endl;
    const auto c = center(closed_seg);
    const auto rx = radius_x(closed_seg);
    const auto ry = radius_y(closed_seg);

    const Unfolding unfold{code_numbers, code_angles};

    // Some of the generated equations are duplicates, so make sure they are in a std::set
    // TODO we could do some sort of rigourous refine as the equations are generated. That
    // would be better than generate them all first and then filtering
    Curves equations{};

    if (code_type == CodeType::CNS) {

        // TODO throw this in a calculate_curves function
        const auto code_angles_eta = falgo::transform(code_angles, xyz_to_xyeta);

        const auto shooting_vector = shooting_vector_closed(code_sequence, code_angles_eta);
        equations = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles, c, rx, ry);

    } else if (code_type == CodeType::ONS) {

        const auto shooting_vector = unfold.shooting_vector_general();
        equations = unfold.generate_curves(shooting_vector.first, shooting_vector.second, initial_angles, c, rx, ry);

    } else {
        std::ostringstream err{};
        err << "stable code " << code_sequence << " of type "
            << code_type << " passed to calculate_unstable_code_info";
        throw std::runtime_error(err.str());
    }

    return CodeInfo{segment, equations.first, equations.second};
}

static CodeInfo calculate_code_info(const CodePair& code_pair) {
    const auto code_type = code_pair.sequence.type();
    if (is_stable(code_type)) {
        return calculate_stable_code_info(code_pair, code_type);
    } else {
        return calculate_unstable_code_info(code_pair, code_type);
    }
}

// These are no longer used with the new parallelism
#if 0
static void save_code_pair(const CodePair& code_pair, sqlite::Database& db) {

    if (!in_database(code_pair, db)) {
        std::cout << '.'; // Indicates finding new equation
        const auto code_info = calculate_code_info(code_pair);
        save_code_info(code_pair, code_info, db);
    }
}

static void save_singles(const std::set<CodePair>& code_pairs, sqlite::Database& db) {

    for (const auto& code_pair : code_pairs) {
        save_code_pair(code_pair, db);
    }
}

static void save_triples(const std::vector<TriplePair>& triples, sqlite::Database& db) {

    for (const auto& triple : triples) {
        save_code_pair(triple.stable_neg, db);
        save_code_pair(triple.unstable, db);
        save_code_pair(triple.stable_pos, db);
    }
}

static CodeInfo retrieve_code_info(const CodePair& code_pair, sqlite::Database& db) {

    if (in_database(code_pair, db)) {
        return load_code_info(code_pair, db);
    } else {
        // Not in database, so we calculate it, save it to the database, then return it
        const auto code_info = calculate_code_info(code_pair);
        save_code_info(code_pair, code_info, db);
        return code_info;
    }
}
#endif
}
