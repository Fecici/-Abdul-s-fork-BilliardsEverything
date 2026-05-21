#pragma once

#include <string>

namespace database {

void create(const std::string& db_path);

void clear(const std::string& db_path);
}
