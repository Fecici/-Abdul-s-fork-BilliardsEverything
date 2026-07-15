#pragma once

#include <boost/optional/optional_io.hpp>

#include <equations.hpp>
#include <parse.hpp>
#include <utils.hpp>

#include <cstdlib>

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
    const auto code_seq = parse_code_sequence(
        "1 5 20 2 4 2 16 4 26 4 16 4 26 5 1 34 1 5 26 4 16 4 26 4 16 2 4 2 20 5 1 29 "
        "6 34 6 29 1 5 22 5 1 29 6 34 6 28 4 14 2 8 2 12 2 8 2 16 4 24 5 1 31 6 32 6 31 "
        "1 5 24 5 1 31 6 32 6 31 1 5 24 5 1 31 6 32 6 31 1 5 24 5 1 31 6 32 6 31 1 5 24 4 16 "
        "2 8 2 12 2 8 2 14 4 28 6 34 6 29 1 5 22 5 1 29 6 34 6 29");
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
