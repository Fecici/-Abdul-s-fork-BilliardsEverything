#pragma once

#include "code_sequence.hpp"
#include "general.hpp"

#include <boost/asio/thread_pool.hpp>
#include <boost/asio/post.hpp>
#include <algorithm>
#include <thread>

std::set<LinComArrZ<XYEta>> calculate_bounding_inequalities(const std::vector<CodeNumber>& code_numbers, const std::vector<XYZ>& code_angles);
