#pragma once

#include <boost/optional/optional_io.hpp>

#include <equations.hpp>
#include <parse.hpp>

static std::vector<CodeSequence> parse_file(const std::string& path) {

    std::vector<CodeSequence> code_seqs{};

    std::ifstream infile{path};

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

    for (const auto& code_seq : code_seqs) {
        const auto code_type = code_seq.type();
        try {
            if (is_stable(code_type)) {
                BOOST_TEST(!calculate_stable(code_seq, code_type));
            } else {
                BOOST_TEST(!calculate_unstable(code_seq, code_type));
            }
        } catch (const std::runtime_error& e) {
            std::cout << "code sequence failed: " << code_seq << std::endl;
            std::cout << e.what() << std::endl;
        }
    }
}

BOOST_AUTO_TEST_CASE(test_calculate_nonempty) {

    const auto code_seqs = parse_file("src/test/resources/nonempty_codes_to_15.txt");

    for (const auto& code_seq : code_seqs) {
        const auto code_type = code_seq.type();
        try {
            if (is_stable(code_type)) {
                BOOST_TEST(calculate_stable(code_seq, code_type));
            } else {
                BOOST_TEST(calculate_unstable(code_seq, code_type));
            }
        } catch (const std::runtime_error& e) {
            std::cout << "code sequence failed: " << code_seq << std::endl;
            std::cout << e.what() << std::endl;
        }
    }
}
