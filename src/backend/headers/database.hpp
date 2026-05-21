#pragma once

#include "common.hpp"
#include "cover/cover.hpp"
#include "general.hpp"
#include "sqlite.hpp"

std::vector<PointQ> bounding_line_segment(const CodeSequence& code_sequence, const InitialAngles& initial_angles);



std::vector<PointQ> bounding_polygon(const CodeSequence& code_sequence, const InitialAngles& initial_angles);

std::vector<std::pair<SinglePair, StableInfo>> get_single_infos(const std::set<CodeSequence>& code_seqs, const bool mrr, sqlite::Database& db);

std::vector<std::pair<TriplePair, TripleInfo>> get_triple_infos(const std::set<Triple>& triples, const bool mrr, sqlite::Database& db);
std::pair<bool, bool> get_triple_infos_duplicate_stables(const std::set<Triple>& triples, const bool mrr, sqlite::Database& db, const bool show);
bool get_triple_infos_half_duplicate_stables(const std::set<HalfTriple>& half_triples, const bool mrr, sqlite::Database& db);
std::pair<InitialAngles, CodeInfo> get_stable_info(const CodeSequence& code_sequence, const bool mrr, sqlite::Database& db);

std::map<SinglePair, StableInfo> get_single_infos_map(const std::vector<CodePair>& code_seqs, const bool mrr, sqlite::Database& db);

std::map<TriplePair, TripleInfo> get_triple_infos_map(const std::vector<TriplePair>& triples, const bool mrr, sqlite::Database& db);

CodeInfo calculate_stable_all_info(const CodeSequence& code_sequence, const InitialAngles& initial_angles);
CodeInfo calculate_unstable_all_info(const CodeSequence& code_sequence, const InitialAngles& initial_angles);
CodeInfo calculate_all_info(const CodeSequence& code_sequence, const InitialAngles& initial_angles);
CodeInfo calculate_all_vector(const CodeSequence& code_sequence, const InitialAngles& initial_angles);
