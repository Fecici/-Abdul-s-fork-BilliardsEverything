#include <mutex>

#include <boost/format.hpp>
#include <tbb/parallel_invoke.h>
#include <tbb/tbb.h>

#include "bounding_region.hpp"
#include "common.hpp"
#include "conversion.hpp"
#include "cover/load.hpp"
#include "cover/save.hpp"
#include "shooting_vectors.hpp"
#include "unfolding.hpp"

#include "db.cpp"
#include "load.cpp"

int main(const int argc, const char* const argv[]) {

    if (argc != 4) {
        std::cout << "Need to provide exactly three arguments" << std::endl;
        return EXIT_FAILURE;
    }

    const std::string mrr_dir{argv[1]};
    const std::string all_dir{argv[2]};
    const auto extra_depth = boost::lexical_cast<uint32_t>(argv[3]);

    const auto load = [&](std::map<SinglePair, StableInfo>& stable_infos,
                          std::map<TriplePair, TripleInfo>& triple_infos,
                          const std::vector<SinglePair>& mrr_stables,
                          const std::vector<TriplePair>& mrr_triples) {
    // Database is turned off
#if 0
        sqlite::error_logging();

        database::init_db(db_path);

        constexpr auto flags = sqlite::Open::Readwrite | sqlite::Open::Fullmutex;
        sqlite::Database db{db_path, flags};

        save_new(mrr_stables, mrr_triples, db);
#endif
        // Need to use a set because we might get some duplicates
        std::set<SinglePair> set_stables{};
        for (const auto& stable_pair : mrr_stables) {
            set_stables.insert(stable_pair);
        }

        for (const auto& triple_pair : mrr_triples) {
            set_stables.insert(SinglePair{triple_pair.stable_neg});
            set_stables.insert(SinglePair{triple_pair.stable_pos});
        }

        // Reverse the order of the code pairs to start loading the largest ones first
        std::vector<SinglePair> new_stables{set_stables.begin(), set_stables.end()};
        falgo::reverse(new_stables);

        stable_infos = load_stable_infos(new_stables);
        triple_infos = load_triple_infos(mrr_triples);
    };

    cover_square_all(mrr_dir, all_dir, load, extra_depth);
}
