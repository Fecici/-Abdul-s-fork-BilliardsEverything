#include "basic.hpp"

// Splitting an empty string should return an empty list
std::vector<std::string> split(const std::string& str, const std::string& delims) {

    std::vector<std::string> vec{};
    if (!str.empty()) {
        boost::split(vec, str, boost::is_any_of(delims));
    }

    return vec;
}

// Open a file with extra error handling
std::ifstream open_file_read(const std::string& path) {

    std::ifstream file{};
    // TODO what about eofbit?
    // Unfortunately, failbit causes problems when reading line-by-line
    // I really need a better way of dealing with this
    file.exceptions(std::ifstream::badbit);

    file.open(path);

    return file;
}

// Open a file with extra error handling
std::ofstream open_file_write(const std::string& path) {

    std::ofstream file{};
    // TODO what about eofbit?
    file.exceptions(std::ofstream::badbit | std::ofstream::failbit);

    file.open(path);

    return file;
}

std::string read_file(const std::string& path) {

    auto file = open_file_read(path);
    std::stringstream buff{};
    buff << file.rdbuf();

    return buff.str();
}

// It would be nice to have line-and-taken iterators I think
