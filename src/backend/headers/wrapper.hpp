#pragma once

#include "numbers.hpp"
#include "sqlite.hpp"

struct CCover {
    int32_t length;
    float64_t* coords;
    char* code_sequences;
};

struct CPicture {
    char* initial_angles;
    char* points;
    char* equations;
};

// We loose type safety anyway when going through this Java-C-C++ pipe,
// so it's ok I think just to have one struct that we map to. Also, I want to
// keep the C stuff as simple as possible, because I'm not sure what JNA is exactly
// up to when it does this
struct CInfo {
    char* initial_angles;
    char* points;
    char* equations;
    char* left_rights;
    char* code_seq_lr;
};
struct CInfoAll {
    char* initial_angles;
    char* points;
    char* equations;
    char* sinEquations;
    char* cosEquations;
    char* left_rights;
    char* code_seq_lr;
    char* vectorX;
    char* vectorY;


};



struct CString {
    char* string;
};

// JNA needs a C interface to the library
extern "C" {
void sqlite_error_logging();
void database_create(const char* const db_path);
void database_clear(const char* const db_path);

sqlite::ConnectionPool* create_connection_pool(const char* const db_path, const int32_t pool_size);
void destroy_connection_pool(const sqlite::ConnectionPool* const pool);

const char* cover_wrapper(const char* const poly_str,
const char* const codes_str, const char* const unstables_str,
const int32_t digits, const int32_t subdivide, const int32_t empty,
const int32_t mrr, sqlite::ConnectionPool* const pool);

int32_t cover_wrapper_duplicate_stables(const char* const poly_str,
const char* const codes_str, const char* const unstables_str,
const int32_t digits, const int32_t subdivide, const int32_t empty,
const int32_t mrr, sqlite::ConnectionPool* const pool, const bool show);

int32_t cover_wrapper_half_duplicate_stables(const char* const poly_str,
const char* const codes_str, const char* const unstables_str,
const int32_t digits, const int32_t subdivide, const int32_t empty,
const int32_t mrr, sqlite::ConnectionPool* const pool);

int32_t cover_wrapper_all(const char* const mrr_str, sqlite::ConnectionPool* const pool, const int32_t extra_depth);

int32_t save_to_database(const int32_t* const code_numbers_ptr,
   const int32_t code_numbers_len,
   sqlite::ConnectionPool* const pool);

int32_t delete_from_database(const int32_t* const code_numbers_ptr,
   const int32_t code_numbers_len,
   sqlite::ConnectionPool* const pool);
int32_t load_info_all(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len, CInfoAll* const cinfoAll, sqlite::ConnectionPool* const pool);

int32_t load_picture(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len,
                     CPicture* const cpicture, sqlite::ConnectionPool* const pool);

void cleanup_cpicture(const CPicture* const cpicture);

int32_t load_picture_lr(const int32_t* const base_code_numbers_ptr, const int32_t base_code_numbers_len,
  const int32_t* const code_numbers_ptr, const int32_t code_numbers_len,
  CPicture* const cpicture, sqlite::ConnectionPool* const pool);
int32_t load_picture_lr_expando(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len,
  CPicture* const cpicture, sqlite::ConnectionPool* const pool, const char* const lr);


int32_t load_info(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len,
                  CInfo* const cinfo, sqlite::ConnectionPool* const pool);

int32_t load_info_slope(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len, CInfoAll* const cinfoAll, sqlite::ConnectionPool* const pool);

void cleanup_cinfo(const CInfo* const cinfo);

int32_t merge_covers(const char* const merge_dir_ptr, const char* const cover_dirs_ptr, sqlite::ConnectionPool* const pool);

int32_t trim_cover(const char* const poly_str, const char* const in_dir, const char* const out_dir);

int32_t code_search_length(const int32_t code_type_int, const int32_t length_int, CString* const cstring, sqlite::ConnectionPool* const pool);

int32_t code_search_even_odd(const int32_t code_type_int, const char* const even_odd, CString* const cstring, sqlite::ConnectionPool* const pool);

int32_t bounding_polygon(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len, CString* const cstring, sqlite::ConnectionPool* const pool);

int32_t load_slope_info(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len, CInfoAll* const cinfoAll, sqlite::ConnectionPool* const pool);
int32_t load_all_equations(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len, CInfoAll* const cinfoAll, sqlite::ConnectionPool* const pool);

float64_t calculate_gradient(const char* const equation_cstr, float64_t x_value, float64_t y_value, bool from_database, CString* const cstring, CString* const cstring2);

void cleanup_string(const CString* const cstring);

const char* getNotFilledCoordinates(const char* poly_str,
    const char* codes_str, const char* unstables_str,
    int32_t digits, int32_t subdivide, int32_t empty,
    int32_t mrr, sqlite::ConnectionPool* pool, bool is_last_cycle);

const char* small_cover_wrapper(const char* poly_str,
    const char* codes_str, const char* unstables_str,
    int32_t digits, int32_t subdivide, int32_t empty,
    int32_t mrr, sqlite::ConnectionPool* pool, bool printInfo);


int vary_cs_cpp(const int32_t int_movesMin, const int32_t int_movesMax, const float64_t  db_xAngle, const float64_t  db_yAngle, CString* const result,const char* const reqTypes);

int vary_3_cpp(const int32_t int_movesMin, const int32_t int_movesMax, const float64_t  db_initPosition, const float64_t  db_xAngle, const float64_t  db_yAngle, CString* const result, const char* const);

int vary_4_cpp(const int32_t int_movesMin, const int32_t int_movesMax, const float64_t db_xAngle, const float64_t db_yAngle,CString* const result,const char* const reqTypes);

void backend_cancel();        // set flag = true
}




