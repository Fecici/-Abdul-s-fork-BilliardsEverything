#if 0
// It is important that the CodePairs in the vector be unique
static std::map<CodePair, StableInfo> load_stable_infos(const std::vector<CodePair>& stables, const std::string& db_path) {

    std::mutex mut{}; // used for locking the stable_infos
    std::map<CodePair, StableInfo> stable_infos{};

    const auto lambda = [&](const tbb::blocked_range<size_t>& range) {
        // Each lambda is run in a separate thread, and so each thread
        // will get its own database connection.
        // In theory, we might only need multithreaded here, not serialized
        constexpr auto flags = sqlite::Open::Readwrite | sqlite::Open::Fullmutex;
        sqlite::Database db{db_path, flags};

        for (size_t i = range.begin(); i != range.end(); ++i) {

            const auto& stable = stables.at(i);

            const auto code_info = database::retrieve_code_info(stable, db);
            const auto stable_info = code_to_stable(code_info);

            // lock the mutex
            std::lock_guard<std::mutex> lock{mut};
            stable_infos.emplace(stable, stable_info);
            // it is destroyed at the end, so released
        }
    };

    tbb::parallel_for(tbb::blocked_range<size_t>{0, stables.size()}, lambda);

    return stable_infos;
}
#endif

// It is important that the CodePairs in the vector be unique
static std::map<SinglePair, StableInfo> load_stable_infos(const std::vector<SinglePair>& single_pairs) {

    std::map<SinglePair, StableInfo> stable_infos{};

    std::mutex mut{};
    const auto lambda = [&](const size_t i) {
        const auto& single_pair = single_pairs.at(i);

        const auto code_info = database::calculate_code_info(single_pair.stable.get());
        StableInfo stable_info{code_info};

        std::lock_guard<std::mutex> lock{mut};
        stable_infos.emplace(single_pair, std::move(stable_info));
    };

    tbb::parallel_for(size_t{0}, single_pairs.size(), lambda);

    return stable_infos;
}

// It is important that the TriplePairs in the vector be unique
#if 0
static std::map<TriplePair, TripleInfo> load_triple_infos(const std::vector<TriplePair>& triples, const std::string& db_path) {

    std::mutex mut{};
    std::map<TriplePair, TripleInfo> triple_infos{};

    const auto lambda = [&](const tbb::blocked_range<size_t>& range) {
        constexpr auto flags = sqlite::Open::Readwrite | sqlite::Open::Fullmutex;
        sqlite::Database db{db_path, flags};

        for (size_t i = range.begin(); i != range.end(); ++i) {

            const auto& triple = triples.at(i);

            auto stable_neg_code_info = database::retrieve_code_info(triple.stable_neg, db);
            const auto unstable_code_info = database::retrieve_code_info(triple.unstable, db);
            auto stable_pos_code_info = database::retrieve_code_info(triple.stable_pos, db);

            remove_factor(stable_neg_code_info, triple.unstable, stable_pos_code_info);

            const auto stable_neg_info = code_to_stable(stable_neg_code_info);
            const auto unstable_info = code_to_unstable(unstable_code_info);
            const auto stable_pos_info = code_to_stable(stable_pos_code_info);

            const TripleInfo triple_info{stable_neg_info, unstable_info, stable_pos_info};

            std::lock_guard<std::mutex> lock{mut};
            triple_infos.emplace(triple, triple_info);
        }
    };

    tbb::parallel_for(tbb::blocked_range<size_t>{0, triples.size()}, lambda);

    return triple_infos;
}
#endif

static std::map<TriplePair, TripleInfo> load_triple_infos(const std::vector<TriplePair>& triple_pairs) {

    std::map<TriplePair, TripleInfo> triple_infos{};

    std::mutex mut{};
    const auto lambda = [&](const size_t i) {
        const auto& triple_pair = triple_pairs.at(i);

        auto stable_neg_info = database::calculate_code_info(triple_pair.stable_neg.get());
        const auto unstable_info = database::calculate_code_info(triple_pair.unstable.get());
        auto stable_pos_info = database::calculate_code_info(triple_pair.stable_pos.get());

        remove_factor(stable_neg_info, triple_pair.unstable.get(), stable_pos_info);

        TripleInfo triple_info{StableInfo{stable_neg_info}, UnstableInfo{unstable_info}, StableInfo{stable_pos_info}};

        std::lock_guard<std::mutex> lock{mut};
        triple_infos.emplace(triple_pair, std::move(triple_info));
    };

    tbb::parallel_for(size_t{0}, triple_pairs.size(), lambda);

    return triple_infos;
}

// Again, we don't use these anymore
#if 0
static std::vector<CodePair> to_save(const std::vector<CodePair>& stables, const std::vector<TriplePair>& triples, sqlite::Database& db) {

    // Need to use a set because we might get some duplicates
    std::set<CodePair> pairs{};

    for (const auto& stable : stables) {
        if (!database::in_database(stable, db)) {
            pairs.insert(stable);
        }
    }

    for (const auto& triple : triples) {
        if (!database::in_database(triple.stable_neg, db)) {
            pairs.insert(triple.stable_neg);
        }

        if (!database::in_database(triple.unstable, db)) {
            pairs.insert(triple.unstable);
        }

        if (!database::in_database(triple.stable_pos, db)) {
            pairs.insert(triple.stable_pos);
        }
    }

    // Needs to be a vector to use tbb
    return {pairs.begin(), pairs.end()};
}

static void save_new(const std::vector<CodePair>& stables, const std::vector<TriplePair>& triples, sqlite::Database& db) {

    auto code_pairs = to_save(stables, triples, db);

    // By default, the code pairs are listed from smallest to largest order
    // Shuffling them might mix up the database access so it doesn't conflict
    auto rng = std::default_random_engine{};
    std::shuffle(code_pairs.begin(), code_pairs.end(), rng);

    std::mutex mut{};
    const auto lambda = [&](const size_t i) {
        const auto& code_pair = code_pairs.at(i);

        const auto code_info = database::calculate_code_info(code_pair);

        std::lock_guard<std::mutex> lock{mut};
        database::save_code_info(code_pair, code_info, db);
    };

    tbb::parallel_for(size_t{0}, code_pairs.size(), lambda);
}
#endif
