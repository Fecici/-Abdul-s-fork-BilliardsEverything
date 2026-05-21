#pragma once

#include <string>
#include <vector>

#include "sqlite.hpp"

#include "cover/cover.hpp"

struct CoverUsedVisitor final : public boost::static_visitor<> {

    std::set<SinglePair> singles;
    std::set<TriplePair> triples;
    std::set<HalfTriplePair> half_triples;

    void operator()(const cover::Empty) {}

    void operator()(const cover::Single& single) {
        singles.insert(single.single_pair);
    }

    void operator()(const cover::Triple& triple) {
        triples.insert(triple.triple_pair);
    }

    void operator()(const cover::Divide& divide) {

        for (const auto& cover : divide.quarters.get()) {
            boost::apply_visitor(*this, cover);
        }
    }
};

template <typename T>
std::map<T, size_t> get_index_info(const std::set<T>& used) {

    std::map<T, size_t> index_map{};

    size_t i = 0;
    for (const auto& t : used) {
        index_map.emplace(t, i);
        ++i;
    }

    return index_map;
}

void union_covers(const std::string& merged_dir, const std::vector<std::string>& dirs, sqlite::Database& db);
