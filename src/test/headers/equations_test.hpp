#pragma once

#include <boost/optional/optional_io.hpp>

#include <equations.hpp>
#include <parse.hpp>
#include <utils.hpp>

#include <cstdlib>
#include <cstdint>
#include <iomanip>
#include <sstream>

static std::vector<CodeSequence> parse_file(const std::string& path) {

    std::vector<CodeSequence> code_seqs{};

    std::ifstream infile{path};
    if (!infile.is_open()) {
        throw std::runtime_error("missing MRR test fixture: " + path);
    }

    // This reads through all the lines of the file
    std::string line{};
    while (std::getline(infile, line)) {
        // When reading through a file:
        // - get a line
        // - trim the whitespace from both sides
        // - check if it is empty
        // - if not, continue to process
        boost::trim(line);

        if (line.empty()) {
            continue;
        } else if (line.find('s') != std::string::npos) {
            // Contains an s
            continue;
        } else if (line.find('S') != std::string::npos) {
            // Contains an S
        } else {
            const auto code_seq = parse_code_sequence(line);
            code_seqs.emplace_back(code_seq);
        }
    }

    return code_seqs;
}

BOOST_AUTO_TEST_CASE(test_calculate_empty) {

    const auto code_seqs = parse_file("src/test/resources/empty_codes_to_15.txt");
    BOOST_REQUIRE(!code_seqs.empty());

    for (const auto& code_seq : code_seqs) {
        const auto code_type = code_seq.type();
        if (is_stable(code_type)) {
            BOOST_TEST(!calculate_stable(code_seq, code_type));
        } else {
            BOOST_TEST(!calculate_unstable(code_seq, code_type));
        }
    }
}

BOOST_AUTO_TEST_CASE(test_calculate_nonempty) {

    const auto code_seqs = parse_file("src/test/resources/nonempty_codes_to_15.txt");
    BOOST_REQUIRE(!code_seqs.empty());

    for (const auto& code_seq : code_seqs) {
        const auto code_type = code_seq.type();
        if (is_stable(code_type)) {
            BOOST_TEST(calculate_stable(code_seq, code_type));
        } else {
            BOOST_TEST(calculate_unstable(code_seq, code_type));
        }
    }
}

static void check_same_stable_boundary(const Stable& lhs, const Stable& rhs) {
    BOOST_CHECK(lhs.initial_angles == rhs.initial_angles);
    BOOST_CHECK(lhs.equations == rhs.equations);
    BOOST_CHECK(lhs.left_rights == rhs.left_rights);
    BOOST_REQUIRE_EQUAL(lhs.points.size(), rhs.points.size());
    for (std::size_t i = 0; i < lhs.points.size(); ++i) {
        for (std::size_t axis = 0; axis < 2; ++axis) {
            BOOST_CHECK_EQUAL(boost::multiprecision::lower(lhs.points[i][axis]),
                              boost::multiprecision::lower(rhs.points[i][axis]));
            BOOST_CHECK_EQUAL(boost::multiprecision::upper(lhs.points[i][axis]),
                              boost::multiprecision::upper(rhs.points[i][axis]));
        }
    }
}

class WorkerCountRestore final {
  private:
    unsigned int original;

  public:
    WorkerCountRestore()
        : original{billiards_worker_count()} {
    }

    ~WorkerCountRestore() {
        billiards_set_worker_count(original);
    }
};

static CodeSequence reported_long_cs() {
    return parse_code_sequence(
        "1 5 20 2 4 2 16 4 26 4 16 4 26 5 1 34 1 5 26 4 16 4 26 4 16 2 4 2 20 5 1 29 "
        "6 34 6 29 1 5 22 5 1 29 6 34 6 28 4 14 2 8 2 12 2 8 2 16 4 24 5 1 31 6 32 6 31 "
        "1 5 24 5 1 31 6 32 6 31 1 5 24 5 1 31 6 32 6 31 1 5 24 5 1 31 6 32 6 31 1 5 24 4 16 "
        "2 8 2 12 2 8 2 14 4 28 6 34 6 29 1 5 22 5 1 29 6 34 6 29");
}

static uint64_t stable_boundary_hash(const Stable& stable) {
    // The benchmark compares this digest across processes and worker counts.
    // Include interval endpoints and boundary provenance, not only median
    // points, so a faster but representation-invalid MRR cannot pass.
    std::ostringstream normalized;
    normalized << std::setprecision(50) << stable.initial_angles << '\n';
    for (const auto& point : stable.points) {
        for (std::size_t axis = 0; axis < 2; ++axis) {
            normalized << boost::multiprecision::lower(point[axis]) << ','
                       << boost::multiprecision::upper(point[axis]) << ';';
        }
    }
    normalized << '\n';
    for (const auto& equation : stable.equations) {
        normalized << equation << '\n';
    }
    for (const auto& left_right : stable.left_rights) {
        normalized << left_right << '\n';
    }

    // FNV-1a is intentionally simple and stable; this is a regression digest,
    // not a cryptographic authenticity check.
    uint64_t hash = UINT64_C(14695981039346656037);
    for (const unsigned char value : normalized.str()) {
        hash ^= value;
        hash *= UINT64_C(1099511628211);
    }
    return hash;
}

BOOST_AUTO_TEST_CASE(test_mrr_worker_count_is_deterministic_and_caps_tbb) {
    WorkerCountRestore restore;
    const auto code_seq = parse_code_sequence("1 3 3");
    const auto code_type = code_seq.type();

    billiards_set_worker_count(1);
    const auto single_worker = calculate_stable(code_seq, code_type);
    BOOST_REQUIRE(single_worker);

    billiards_set_worker_count(2);
    BOOST_CHECK_EQUAL(billiards_worker_count(), 2u);
    BOOST_CHECK_LE(tbb::global_control::active_value(tbb::global_control::max_allowed_parallelism), 2u);
    const auto two_workers = calculate_stable(code_seq, code_type);
    BOOST_REQUIRE(two_workers);

    check_same_stable_boundary(*single_worker, *two_workers);
}

BOOST_AUTO_TEST_CASE(test_reported_long_cs_mrr_regression) {
    if (std::getenv("BILLIARDS_RUN_SLOW_TESTS") == nullptr) {
        BOOST_TEST_MESSAGE("Skipping long MRR regression; run testBackendSlow to enable it");
        return;
    }

    WorkerCountRestore restore;
    const auto code_seq = reported_long_cs();
    const auto code_type = code_seq.type();
    BOOST_REQUIRE(code_type == CodeType::CS);

    billiards_set_worker_count(1);
    const auto single_worker = calculate_stable(code_seq, code_type);
    BOOST_REQUIRE(single_worker);

    billiards_set_worker_count(4);
    const auto four_workers = calculate_stable(code_seq, code_type);
    BOOST_REQUIRE(four_workers);

    check_same_stable_boundary(*single_worker, *four_workers);
}

BOOST_AUTO_TEST_CASE(benchmark_reported_long_cs_mrr) {
    if (std::getenv("BILLIARDS_RUN_BENCHMARKS") == nullptr) {
        BOOST_TEST_MESSAGE("Skipping benchmark workload; use tools/benchmark/run-benchmarks.ps1");
        return;
    }

    const char* const raw_workers = std::getenv("BILLIARDS_BENCHMARK_WORKER");
    if (raw_workers == nullptr) {
        BOOST_FAIL("BILLIARDS_BENCHMARK_WORKER is required for the benchmark workload");
    }

    char* end = nullptr;
    const long parsed_workers = std::strtol(raw_workers, &end, 10);
    if (end == raw_workers || *end != '\0' || parsed_workers <= 0) {
        BOOST_FAIL("BILLIARDS_BENCHMARK_WORKER must be a positive integer");
    }

    WorkerCountRestore restore;
    billiards_set_worker_count(static_cast<unsigned int>(parsed_workers));
    const auto code_seq = reported_long_cs();
    const auto stable = calculate_stable(code_seq, code_seq.type());
    BOOST_REQUIRE(stable);

    std::cout << "BENCH_RESULT hash=" << std::hex << stable_boundary_hash(*stable) << std::dec
              << " points=" << stable->points.size()
              << " equations=" << stable->equations.size()
              << " workers=" << billiards_worker_count() << std::endl;
}
