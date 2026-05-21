#pragma once

#include "code_type.hpp"
#include "sqlite.hpp"

std::string code_search(const CodeType code_type, const size_t length, sqlite::Database& db);
std::string code_search(const CodeType code_type, const std::string& even_odd, sqlite::Database& db);
