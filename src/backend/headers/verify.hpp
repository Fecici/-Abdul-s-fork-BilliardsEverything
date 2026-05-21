#pragma once

#include "general.hpp"
#include "sqlite.hpp"

ClosedConvexPolygonQ parse_polygon(const std::string& str);

const char* getEmpties(const std::string& polygon_str, const std::string& singles_str, const std::string& triples_str,
    uint32_t digits, uint32_t max_depth, size_t empty, bool mrr, sqlite::ConnectionPool& pool, bool is_last_cycle);

const char* check_cover(const std::string& polygon_str, const std::string& stables_str, const std::string& triples_str,
                 uint32_t digits, uint32_t subdivide, size_t empty,
                 bool mrr, sqlite::ConnectionPool& pool);

const char* check_small_cover(const std::string& polygon_str, const std::string& singles_str, const std::string& triples_str,
                 uint32_t digits, uint32_t max_depth, size_t empty,
                 bool mrr, sqlite::ConnectionPool& pool, bool printInfo);
                
int32_t check_cover_duplicate_stables(const std::string& polygon_str, const std::string& stables_str, const std::string& triples_str,
                 const bool mrr, sqlite::ConnectionPool& pool, const bool show);

bool check_cover_half_duplicate_stables(const std::string& polygon_str, const std::string& singles_str, const std::string& triples_str,
                 const uint32_t digits, const uint32_t max_depth, const size_t empty,
                 const bool mrr, std::set<std::pair<CodeSequence, std::string>>& sequence_equations, sqlite::ConnectionPool& pool);

bool check_cover_all(const std::string& mrr_dir, sqlite::ConnectionPool& pool, const uint32_t extra_depth);

void restore(std::set<std::pair<CodeSequence, std::string>> sequence_equations, std::set<std::pair<CodeSequence, std::string>> sequence_init_angles,
            std::set<std::pair<CodeSequence, std::string>> sequence_points, sqlite::ConnectionPool& pool);

std::string update_initial_angles(CodeSequence code_sequence, sqlite::Database& db );
std::string update_points(CodeSequence code_sequence, sqlite::Database& db);
