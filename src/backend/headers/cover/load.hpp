#pragma once

#include "cover.hpp"

namespace cover {

ClosedRectangleQ load_square(const std::string& dir);

ClosedConvexPolygonQ load_polygon(const std::string& dir);

std::vector<SinglePair> load_singles(const std::string& dir);

std::vector<TriplePair> load_triples(const std::string& dir);



Cover load_cover(const std::string& dir, const std::vector<SinglePair>& singles, const std::vector<TriplePair>& triples);

uint32_t load_digits(const std::string& dir);
}
