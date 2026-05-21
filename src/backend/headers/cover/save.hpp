#pragma once

#include "cover.hpp"

namespace cover {

void save_square(const std::string& dir, const ClosedRectangleQ& square);

void save_polygon(const std::string& dir, const ClosedConvexPolygonQ& polygon);

void save_singles(const std::string& dir, const std::map<SinglePair, size_t>& singles);

void save_triples(const std::string& dir, const std::map<TriplePair, size_t>& triples);

void save_cover(const std::string& dir, const cover::Cover& cover,
                const std::map<SinglePair, size_t>& singles,
                const std::map<TriplePair, size_t>& triples);

void save_digits(const std::string& dir, const uint32_t digits);
}
