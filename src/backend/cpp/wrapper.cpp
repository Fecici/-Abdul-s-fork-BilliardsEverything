
/*
Note: If you want to print the following stuffs, search for the labels to locate them and uncomment the printing line to print them

    1. stable or unstable information(leftrights,points, equations):Label 1,Label 2,Label 3,Label 4

 */
#include <boost/math/constants/constants.hpp>

#include "cmath"
#include "database.hpp"
#include "database/admin.hpp"
#include "database/serialize.hpp"
#include "database/deserialize.hpp"
#include "database.hpp"
#include "diff.hpp"

#include "equations.hpp"
#include "evalf.hpp"

#include "merge.hpp"
#include "parse.hpp"
#include "search.hpp"
#include "sqlite.hpp"
#include "trig_identities.hpp"
#include "trim.hpp"
#include "utils.hpp"
#include "verify.hpp"
#include "wrapper.hpp"
#include "vary_cs.hpp"
#include "vary3.hpp"
#include "vary4.hpp"
#include <exception>
#include <stdexcept>
#include <boost/optional/optional_io.hpp>

// Java <-> C++
// byte     int8_t
// short    int16_t
// int      int32_t
// long     int64_t
// float    float32_t
// double   float64_t
// boolean  bool
// The last boolean <-> bool isn't strictly part of the documentation, but I think it works,
// since bools can be coerced into C ints, which are then converted to Java booleans
// (0 is false and anything else is true)

static char* to_cstr(const std::string& str) {

    // + 1 for the nul character
    char* const c_str = new char[str.size() + 1];
    strcpy(c_str, str.c_str());

    return c_str;
}

namespace {

thread_local std::string backend_error_message;

void clear_backend_error() {
    backend_error_message.clear();
}

void remember_backend_error(const char* const operation, const std::exception& except) {
    backend_error_message = std::string(operation) + " failed: " + except.what();
    std::cerr << backend_error_message << std::endl;
}

void remember_unknown_backend_error(const char* const operation) {
    backend_error_message = std::string(operation) + " failed: unknown native exception";
    std::cerr << backend_error_message << std::endl;
}

}

const char* backend_last_error() {
    return backend_error_message.c_str();
}

void sqlite_error_logging() {
    sqlite::error_logging();
}

void backend_set_worker_threads(const int32_t worker_count) {
    clear_backend_error();
    try {
        if (worker_count <= 0) {
            throw std::invalid_argument("worker count must be positive");
        }

        // The Java launcher clamps the user argument against available CPUs.
        // The backend still validates the ABI boundary and then stores the
        // shared worker limit used by native Boost/TBB loops.
        billiards_set_worker_count(boost::numeric_cast<unsigned int>(worker_count));
    } catch (const std::exception& except) {
        remember_backend_error("backend_set_worker_threads", except);
    } catch (...) {
        remember_unknown_backend_error("backend_set_worker_threads");
    }
}

void database_create(const char* const db_path) {
    clear_backend_error();
    try {
        if (db_path == nullptr) {
            throw std::invalid_argument("database path is null");
        }
        database::create(db_path);
    } catch (const std::exception& except) {
        remember_backend_error("database_create", except);
    } catch (...) {
        remember_unknown_backend_error("database_create");
    }
}

void database_clear(const char* const db_path) {
    clear_backend_error();
    try {
        if (db_path == nullptr) {
            throw std::invalid_argument("database path is null");
        }
        database::clear(db_path);
    } catch (const std::exception& except) {
        remember_backend_error("database_clear", except);
    } catch (...) {
        remember_unknown_backend_error("database_clear");
    }
}

sqlite::ConnectionPool* create_connection_pool(const char* const db_path, const int32_t pool_size) {
    clear_backend_error();
    try {
        if (db_path == nullptr) {
            throw std::invalid_argument("database path is null");
        }
        if (pool_size <= 0) {
            throw std::invalid_argument("connection pool size must be positive");
        }

        const std::string path = db_path;

        const auto lambda = [&] {
            constexpr auto flags = sqlite::Open::Readwrite | sqlite::Open::Fullmutex;

            // TODO are there extra flags we want to open the database with,
            // or some other pragmas we want to execute?
            sqlite::Database db{path, flags};

            // A prepare only prepares the first statement in the string
            std::string journal_mode{};
            db.prepare("pragma journal_mode = wal;").bind().exec(journal_mode);

            if (journal_mode != "wal") {
                throw std::runtime_error("unable to set wal; journal mode = " + journal_mode);
            }

            db.prepare("pragma synchronous = full;").bind().exec();

            int64_t synchronous{};
            db.prepare("pragma synchronous;").bind().exec(synchronous);

            if (synchronous != 2) {
                throw std::runtime_error("unable to set full; synchronous = " + std::to_string(synchronous));
            }

            return db;
        };

        // This exported function is called from JNA. Never let C++
        // exceptions cross that ABI boundary; Java checks null and reads
        // backend_last_error() for the detailed reason.
        sqlite::ConnectionPool* pool = new sqlite::ConnectionPool{lambda, boost::numeric_cast<size_t>(pool_size)};

        return pool;
    } catch (const std::exception& except) {
        remember_backend_error("create_connection_pool", except);
        return nullptr;
    } catch (...) {
        remember_unknown_backend_error("create_connection_pool");
        return nullptr;
    }
}

void destroy_connection_pool(const sqlite::ConnectionPool* const pool) {
    clear_backend_error();
    try {
        if (pool == nullptr) {
            return;
        }

        std::cout << "Started with "<< pool->start_size() << " DB connections\n";
        std::cout << "Ending with " << pool->curr_size() << " DB connections" << std::endl;
        delete pool;
    } catch (const std::exception& except) {
        remember_backend_error("destroy_connection_pool", except);
    } catch (...) {
        remember_unknown_backend_error("destroy_connection_pool");
    }
}

// -1 failure
// 0 on not a cover
// 1 means a cover
int32_t cover_wrapper(const char* const poly_str,
                      const char* const codes_str, const char* const unstables_str,
                      const int32_t digits, const int32_t subdivide, const int32_t empty,
                      const int32_t mrr, sqlite::ConnectionPool* const pool, CString* const result) {

    try {

        const std::string poly{poly_str};
        const std::string codes{codes_str};
        const std::string unstables{unstables_str};

        const auto covered = check_cover(poly, codes, unstables, 
            boost::numeric_cast<uint32_t>(digits), 
            boost::numeric_cast<uint32_t>(subdivide), 
            boost::numeric_cast<size_t>(empty), mrr, *pool);

        result->string = to_cstr(covered);
        return 1;
    } catch (const std::runtime_error& except) {
        std::cerr << "calculation of cover failed with error:\n"
                  << except.what() << std::endl;
        return -1;
    }
}

// -1 failure
// 0 on not a cover
// 1 means a cover
int32_t small_cover_wrapper(const char* const poly_str,
                      const char* const codes_str, const char* const unstables_str,
                      const int32_t digits, const int32_t subdivide, const int32_t empty,
                      const int32_t mrr, sqlite::ConnectionPool* const pool, const bool printInfo, CString* const result) {

    try {

        const std::string poly{poly_str};
        const std::string codes{codes_str};
        const std::string unstables{unstables_str};

        const auto covered = check_small_cover(poly, codes, unstables, boost::numeric_cast<uint32_t>(digits), boost::numeric_cast<uint32_t>(subdivide), boost::numeric_cast<size_t>(empty), mrr, *pool, printInfo);

        result->string = to_cstr(covered);
        return 1;
    } catch (const std::runtime_error& except) {
        std::cerr << "calculation of cover failed with error:\n"
                  << except.what() << std::endl;
        return -1;
    }
}

int32_t get_not_filled_coordinates(const char* const poly_str,
    const char* const codes_str, const char* const unstables_str,
    const int32_t digits, const int32_t subdivide, const int32_t empty,
    const int32_t mrr, sqlite::ConnectionPool* const pool, const bool is_last_cycle, CString* const result) {

    try {

        const std::string poly{poly_str};
        const std::string codes{codes_str};
        const std::string unstables{unstables_str};

        const auto empties = getEmpties(poly, codes, unstables,
            boost::numeric_cast<uint32_t>(digits), 
            boost::numeric_cast<uint32_t>(subdivide), 
            boost::numeric_cast<size_t>(empty), mrr, *pool, is_last_cycle);

        result->string = to_cstr(empties);
        return 1;

    } catch (const std::runtime_error& except) {
        std::cerr << "calculation of cover failed with error:\n"
                  << except.what() << std::endl;
        return -1;
    }
}

int32_t cover_wrapper_duplicate_stables(const char* const poly_str,
                      const char* const codes_str, const char* const unstables_str,
                      const int32_t digits, const int32_t subdivide, const int32_t empty,
                      const int32_t mrr, sqlite::ConnectionPool* const pool, const bool show) {

    // Zhao Yu Li, Jul 16, 2025.
    // Removed printing because this line is printed too many times.
    // std::cout << "Entered Cover Wrapper Duplicate Stables" << std::endl;

    try {

        const std::string poly{poly_str};
        const std::string codes{codes_str};
        const std::string unstables{unstables_str};

        return check_cover_duplicate_stables(poly, codes, unstables, mrr, *pool, show);

    } catch (const std::runtime_error& except) {
        std::cerr << "calculation of cover failed with error:\n"
                  << except.what() << std::endl;
        return -1;
    }
}

int32_t cover_wrapper_half_duplicate_stables(const char* const poly_str,
                      const char* const codes_str, const char* const unstables_str,
                      const int32_t digits, const int32_t subdivide, const int32_t empty,
                      const int32_t mrr, sqlite::ConnectionPool* const pool) {
    try {

        const std::string poly{poly_str};
        const std::string codes{codes_str};
        const std::string unstables{unstables_str};
        std::set<std::pair<CodeSequence, std::string>> sequence_equations{};
        std::set<std::pair<CodeSequence, std::string>> sequence_init_angles{};
        std::set<std::pair<CodeSequence, std::string>> sequence_points{};

        const auto covered = check_cover_half_duplicate_stables(poly, codes, unstables, 
            boost::numeric_cast<uint32_t>(digits), 
            boost::numeric_cast<uint32_t>(subdivide), 
            boost::numeric_cast<size_t>(empty), mrr, sequence_equations, *pool);
        if(covered){
            return 1;
        } else {
            return 0;
        }
    } catch (const std::runtime_error& except) {
        std::cerr << "calculation of cover failed with error:\n"
                  << except.what() << std::endl;
        return -1;
    }
}

int32_t cover_wrapper_all(const char* const mrr_str, sqlite::ConnectionPool* const pool, const int32_t extra_depth) {

    try {
        const std::string mrr_dir{mrr_str};

        const auto covered = check_cover_all(mrr_dir, *pool, boost::numeric_cast<uint32_t>(extra_depth));

        return covered;
    } catch (const std::runtime_error& except) {
        std::cerr << "calculation of all cover failed with error:\n"
                  << except.what() << std::endl;
        return -1;
    }
}

// true for saved and nonempty, false for empty
static bool save_to_database(const CodeSequence& code_sequence, const CodeType& code_type, sqlite::Database& db) {

    if (!database::in(code_sequence, code_type, db)) {

        if (is_stable(code_type)) {

            // not in it, try to calculate
            const auto stable = calculate_stable(code_sequence, code_type);
            //Label 3 uncomment the line below to see the stable
            //std::cout<< stable<<std::endl;

            // This doesn't check if it is unstable. It checks if we returned an empty optional
            if (!stable) {
                return false;
            }

            database::save(code_sequence, code_type, *stable, db);
        } else {

            // not in it, try to calculate
            const auto unstable = calculate_unstable(code_sequence, code_type);
            //Label 4 uncomment the line below to see the unstable
            //std::cout<< unstable<<std::endl;


            // This doesn't check if it is unstable. It checks if we returned an empty optional
            if (!unstable) {
                return false;
            }

            database::save(code_sequence, code_type, *unstable, db);
        }
    }

    return true;
}


// true for saved and nonempty, false for empty
static bool save_to_database(const CodeSequence& base_code_sequence, const std::vector<LeftRight>& left_rights, const CodeSequence& code_sequence, const CodeType& code_type, sqlite::Database& db) {

    if (!database::in(code_sequence, code_type, db)) {

        if (is_stable(code_type)) {

            // not in it, try to calculate
            const auto stable = calculate_stable(code_sequence, code_type, left_rights);

            //Label 1 uncomment the line below to see the stable
            //std::cout<< stable<<std::endl;

            //if (left_rights != stable->left_rights) {
            //    throw std::runtime_error(" left_rights mismatch before save and after calculation ");
                //return false;
            //}

            if (!stable) { // This doesn't check if it is unstable. It checks if we returned an empty optional
                return false;
            }

            database::save(base_code_sequence, code_sequence, code_type, *stable, db);
        } else {

            // not in it, try to calculate
            const auto unstable = calculate_unstable(code_sequence, code_type, left_rights);

            //Label 2 uncomment the line below to print the unstable
            //std::cout<< unstable<<std::endl;


            if (!unstable) {// This doesn't check if it is unstable. It checks if we returned an empty optional
                return false;
            }

            //std::vector<LeftRight> vector;
            //for (const auto& lr : unstable->left_rights){
            //    vector.push_back(lr);
            //}

            //if ( vector != left_rights)
            //{
            //       throw std::runtime_error(" left_rights mismatch before save and after calculation ");
            //}
            //else
            //{
                       //std::cout << code_sequence << std::endl;
            database::save(base_code_sequence, code_sequence, code_type, *unstable, db);
            //}
        }
    }

    return true;
}

// -1 means error doing calculation
// 0 means empty set
// 1 means non empty set
int32_t save_to_database(const int32_t* const code_numbers_ptr,
                         const int32_t code_numbers_len,
                         sqlite::ConnectionPool* const pool) {

    clear_backend_error();

    const std::vector<CodeNumber> code_numbers{code_numbers_ptr, code_numbers_ptr + code_numbers_len};

    // We throw runtime_errors to indicate problems with the program. When this happens,
    // we want to handle it gracefully. We simply catch the error, print out an error
    // message, and return 2 to indicate the error
    // Errors that we don't throw, like a std::bad_alloc, we allow through, since those
    // are really bad errors
    try {

        const auto code_sequence = CodeSequence{code_numbers};

        const auto code_type = code_sequence.type();

        sqlite::PooledConnection conn{*pool};

        const auto in = save_to_database(code_sequence, code_type, conn.db);

        return in;

    } catch (const std::runtime_error& except) {
        remember_backend_error("save_to_database", except);

        return -1;
    }
}

static bool delete_from_database(const CodeSequence& code_sequence, const CodeType& code_type, sqlite::Database& db) {
    if (database::in(code_sequence, code_type, db)) {
        database::delete_from_db(code_sequence, code_type, db);
        return true;
    }
    else {
        //throw std::runtime_error("This code sequence does not exist in the database");
        return false;
    }
}

int32_t delete_from_database(const int32_t* const code_numbers_ptr,
                         const int32_t code_numbers_len,
                         sqlite::ConnectionPool* const pool) {

    const std::vector<CodeNumber> code_numbers{code_numbers_ptr, code_numbers_ptr + code_numbers_len};

    try {

        const auto code_sequence = CodeSequence{code_numbers};

        const auto code_type = code_sequence.type();

        sqlite::PooledConnection conn{*pool};

        const auto success = delete_from_database(code_sequence, code_type, conn.db);

        return success;

    } catch (const std::runtime_error& except) {
        std::cerr << "Deleting " << code_numbers << " failed with error:\n"
                  << except.what() << std::endl;
        return -1;
    }}


// true for saved and nonempty, false for empty
static bool save_to_database(const std::vector<LeftRight>& left_rights, const CodeSequence& code_sequence, const CodeType& code_type, sqlite::Database& db) {
    if (!database::in(code_sequence, code_type, db)) {
        if (is_stable(code_type)) {
            // not in it, try to calculate
            const auto stable = calculate_stable(code_sequence, code_type, left_rights);
            // This doesn't check if it is unstable. It checks if we returned an empty optional
            // std::cout << stable->left_rights << std::endl;
            if (!stable) {
                return false;
            }
            if (stable->left_rights != left_rights)
            {
                throw std::runtime_error("The pattern changed do it in the slow way!");
            }
            else{
            // DEBUG
                //std::cout << "save_to_database that prints something" << std::endl;
                //std::cout << code_sequence << std::endl;
                database::save(code_sequence, code_type, *stable, db);
            }

        } else {
            // not in it, try to calculate

            const auto unstable = calculate_unstable(code_sequence, code_type, left_rights);
            // This doesn't check if it is unstable. It checks if we returned an empty optional
            if (!unstable) {
                return false;
            }
            std::vector<LeftRight> vector;
            for (const auto& lr : unstable->left_rights){
                vector.push_back(lr);
            }

            if ( vector != left_rights)
                 {
                            throw std::runtime_error("The pattern changed do it in the slow way!");
                            return false;
                        }
                        else
                        {
                            //std::cout << code_sequence << std::endl;
                            database::save(code_sequence, code_type, *unstable, db);
                        }
        }
    }
    return true;
}

static void copy_to_cpicture(const Picture& picture, CPicture* const cpicture) {

    char* initial_angles = nullptr;
    char* points = nullptr;
    char* equations = nullptr;
    try {
        initial_angles = to_cstr(picture.initial_angles);
        points = to_cstr(picture.points);
        equations = to_cstr(picture.equations);
    } catch (...) {
        // Roll back partial native ownership if a later allocation fails.
        delete[] initial_angles;
        delete[] points;
        delete[] equations;
        throw;
    }

    cpicture->initial_angles = initial_angles;
    cpicture->points = points;
    cpicture->equations = equations;
}

// -1 means error doing calculation
// 0 means empty set
// 1 means non empty set
int32_t load_picture(const int32_t* const code_numbers_ptr,
                     const int32_t code_numbers_len,
                     CPicture* const cpicture,
                     sqlite::ConnectionPool* const pool) {

    clear_backend_error();

    const std::vector<CodeNumber> code_numbers{code_numbers_ptr, code_numbers_ptr + code_numbers_len};

    // We throw runtime_errors to indicate problems with the program. When this happens,
    // we want to handle it gracefully. We simply catch the error, print out an error
    // message, and return 2 to indicate the error
    // Errors that we don't throw, like a std::bad_alloc, we allow through, since those
    // are really bad errors
    try {

        const auto code_sequence = CodeSequence{code_numbers};

        const auto code_type = code_sequence.type();

        // Keep database ownership separate from the expensive geometry work.
        // AutoVary can run many storage jobs at once, and holding a pooled
        // SQLite connection while calculate_stable/calculate_unstable runs can
        // starve unrelated UI/database reads for seconds or minutes.
        bool already_in_db = false;
        {
            sqlite::PooledConnection conn{*pool};
            already_in_db = database::in(code_sequence, code_type, conn.db);
        }

        if (!already_in_db) {
            if (is_stable(code_type)) {
                const auto stable = calculate_stable(code_sequence, code_type);

                if (!stable) {
                    return 0;
                }

                sqlite::PooledConnection conn{*pool};
                database::save(code_sequence, code_type, *stable, conn.db);
            } else {
                const auto unstable = calculate_unstable(code_sequence, code_type);

                if (!unstable) {
                    return 0;
                }

                sqlite::PooledConnection conn{*pool};
                database::save(code_sequence, code_type, *unstable, conn.db);
            }
        }

        {
            sqlite::PooledConnection conn{*pool};
            const auto picture = database::load_picture(code_sequence, code_type, conn.db);
            copy_to_cpicture(picture, cpicture);
        }

        return 1;

    } catch (const std::runtime_error& except) {
        remember_backend_error("load_picture", except);

        return -1;
    }
}

void cleanup_cpicture(const CPicture* const cpicture) {
    delete[] cpicture->initial_angles;
    delete[] cpicture->points;
    delete[] cpicture->equations;
}

int32_t load_picture_lr_expando(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len,
                        CPicture* const cpicture, sqlite::ConnectionPool* const pool, const char* const lr) {
    clear_backend_error();
    const std::vector<CodeNumber> code_numbers{code_numbers_ptr, code_numbers_ptr + code_numbers_len};

    // We throw runtime_errors to indicate problems with the program. When this happens,
    // we want to handle it gracefully. We simply catch the error, print out an error
    // message, and return 2 to indicate the error
    // Errors that we don't throw, like a std::bad_alloc, we allow through, since those
    // are really bad errors
    try {
        //const auto base_code_sequence = CodeSequence{base_code_numbers};
        //const auto base_code_type = base_code_sequence.type();

        const auto code_sequence = CodeSequence{code_numbers};
        const auto code_type = code_sequence.type();

        //const auto base_lr = database::load_left_rights(base_code_sequence, base_code_type, conn.db);
        //const std::vector<CodeNumber> c {1,9,10,8,12,10,12,10,12,8,10,9,1,13,8,6,2,2,4,8,8,12,11,1,11,8,8,6,8,6,8,8,12,10,12,10,13,1,11,14,10,12,10,12,8,10,10,14,10,10,8,12,10,12,10,14,11,1,13,10,12,10,12,9,1,14};
        //std::string str = "11 1 25 0\n47 2 25 0\n47 2 18 1\n40 0 18 1\n40 0 57 0\n40 0 52 2\n11 1 52 2";
        //const CodeSequence code= new CodeSequence(c);
        const std::string lr_string{lr};


            std::vector<LeftRight> left_rights{};
            const auto lines = split(lr_string, "\n");

            for (const auto& line : lines) {
                const auto nums = split(line, " ");
                // Label5
                //std::cout<< "lplr - nums" << nums << std::endl;

                if (nums.size() != 4) {
                    throw std::runtime_error("incorrect nums size in parse_left_rights");
                }

                const auto left_number = boost::lexical_cast<size_t>(nums.at(0));
                const auto left_branch = boost::lexical_cast<size_t>(nums.at(1));

                const auto right_number = boost::lexical_cast<size_t>(nums.at(2));
                const auto right_branch = boost::lexical_cast<size_t>(nums.at(3));

                left_rights.emplace_back(Vertex{left_number, left_branch}, Vertex{right_number, right_branch});
            }

        // The parsed left/right pattern is pure input data; only the quick
        // existence check, save, and final picture load need a SQLite
        // connection. The expensive classification step runs connection-free.
        bool already_in_db = false;
        {
            sqlite::PooledConnection conn{*pool};
            already_in_db = database::in(code_sequence, code_type, conn.db);
        }

        if (!already_in_db) {
            if (is_stable(code_type)) {
                const auto stable = calculate_stable(code_sequence, code_type, left_rights);

                if (!stable) {
                    return 0;
                }
                if (stable->left_rights != left_rights) {
                    throw std::runtime_error("The pattern changed do it in the slow way!");
                }

                sqlite::PooledConnection conn{*pool};
                database::save(code_sequence, code_type, *stable, conn.db);
            } else {
                const auto unstable = calculate_unstable(code_sequence, code_type, left_rights);

                if (!unstable) {
                    return 0;
                }

                std::vector<LeftRight> calculated_left_rights;
                for (const auto& lr_entry : unstable->left_rights) {
                    calculated_left_rights.push_back(lr_entry);
                }
                if (calculated_left_rights != left_rights) {
                    throw std::runtime_error("The pattern changed do it in the slow way!");
                }

                sqlite::PooledConnection conn{*pool};
                database::save(code_sequence, code_type, *unstable, conn.db);
            }
        }

        {
            sqlite::PooledConnection conn{*pool};
            const auto picture = database::load_picture(code_sequence, code_type, conn.db);
            copy_to_cpicture(picture, cpicture);
        }

        return 1;

    } catch (const std::runtime_error& except) {
        remember_backend_error("load_picture_lr_expando", except);
        return -1;
    }
}

// -1 means error doing calculation
// 0 means empty set
// 1 means non empty set
int32_t load_picture_lr(const int32_t* const base_code_numbers_ptr, const int32_t base_code_numbers_len,
                        const int32_t* const code_numbers_ptr, const int32_t code_numbers_len,
                        CPicture* const cpicture, sqlite::ConnectionPool* const pool) {
    clear_backend_error();
    const std::vector<CodeNumber> base_code_numbers{base_code_numbers_ptr,
                                                    base_code_numbers_ptr + base_code_numbers_len};
    const std::vector<CodeNumber> code_numbers{code_numbers_ptr, code_numbers_ptr + code_numbers_len};

    // We throw runtime_errors to indicate problems with the program. When this happens,
    // we want to handle it gracefully. We simply catch the error, print out an error
    // message, and return 2 to indicate the error
    // Errors that we don't throw, like a std::bad_alloc, we allow through, since those
    // are really bad errors
    try {
        const auto base_code_sequence = CodeSequence{base_code_numbers};
        const auto base_code_type = base_code_sequence.type();

        const auto code_sequence = CodeSequence{code_numbers};
        const auto code_type = code_sequence.type();

        std::vector<LeftRight> base_lr;
        bool already_in_db = false;
        {
            sqlite::PooledConnection conn{*pool};
            base_lr = database::load_left_rights(base_code_sequence, base_code_type, conn.db);
            already_in_db = database::in(code_sequence, code_type, conn.db);
        }

        //const std::vector<CodeNumber> c {1,9,10,8,12,10,12,10,12,8,10,9,1,13,8,6,2,2,4,8,8,12,11,1,11,8,8,6,8,6,8,8,12,10,12,10,13,1,11,14,10,12,10,12,8,10,10,14,10,10,8,12,10,12,10,14,11,1,13,10,12,10,12,9,1,14};
        //std::string str = "11 1 25 0\n47 2 25 0\n47 2 18 1\n40 0 18 1\n40 0 57 0\n40 0 52 2\n11 1 52 2";
        //const CodeSequence code= new CodeSequence(c);
        //std::cout<<"sdasdasd"<<lr<<std::endl;

        // Avoid holding the pooled connection while computing the derived
        // left/right picture. The loaded base_lr is immutable input for the
        // calculation; saving and verification reacquire short-lived handles.
        if (!already_in_db) {
            if (is_stable(code_type)) {
                const auto stable = calculate_stable(code_sequence, code_type, base_lr);

                if (!stable) {
                    return 0;
                }

                sqlite::PooledConnection conn{*pool};
                database::save(base_code_sequence, code_sequence, code_type, *stable, conn.db);
            } else {
                const auto unstable = calculate_unstable(code_sequence, code_type, base_lr);

                if (!unstable) {
                    return 0;
                }

                sqlite::PooledConnection conn{*pool};
                database::save(base_code_sequence, code_sequence, code_type, *unstable, conn.db);
            }
        }

        {
            sqlite::PooledConnection conn{*pool};

            const auto lr = database::load_left_rights(code_sequence, code_type, conn.db);

            if (base_lr != lr) {
                std::ostringstream err{};
                err << "left rights mismatch for " << code_sequence << '\n'
                    << "given: " << lr << '\n'
                    << "expected: " << base_lr;
                throw std::runtime_error(err.str());
            }

            const auto picture = database::load_picture(code_sequence, code_type, conn.db);
            copy_to_cpicture(picture, cpicture);
        }

        return 1;

    } catch (const std::runtime_error& except) {
        remember_backend_error("load_picture_lr", except);
        return -1;
    }
}

// Works for Stable and Unstable
static void copy_to_cinfoAll(const CodeInfo& info, CInfoAll* const cinfoAll) {
    char* sin_equations = nullptr;
    char* cos_equations = nullptr;
    char* initial_angles = nullptr;
    char* points = nullptr;
    char* equations = nullptr;
    char* left_rights = nullptr;
    char* code_seq_lr = nullptr;
    char* vector_x = nullptr;
    char* vector_y = nullptr;
    try {
        sin_equations = to_cstr(database::serialize(info.sin_equations));
        cos_equations = to_cstr(database::serialize(info.cos_equations));
        initial_angles = to_cstr("");
        points = to_cstr("");
        equations = to_cstr("");
        left_rights = to_cstr("");
        code_seq_lr = to_cstr("");
        vector_x = to_cstr("");
        vector_y = to_cstr("");
    } catch (...) {
        // CInfoAll crosses the JNA boundary; Java only owns fields after every
        // allocation succeeds, so C++ must clean up any partial result here.
        delete[] sin_equations;
        delete[] cos_equations;
        delete[] initial_angles;
        delete[] points;
        delete[] equations;
        delete[] left_rights;
        delete[] code_seq_lr;
        delete[] vector_x;
        delete[] vector_y;
        throw;
    }

    cinfoAll->sinEquations = sin_equations;
    cinfoAll->cosEquations = cos_equations;
    cinfoAll->initial_angles = initial_angles;
    cinfoAll->points = points;
    cinfoAll->equations = equations;
    cinfoAll->left_rights = left_rights;
    cinfoAll->code_seq_lr = code_seq_lr;
    cinfoAll->vectorX = vector_x;
    cinfoAll->vectorY = vector_y;
}

int32_t load_all_equations(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len, CInfoAll* const cinfoAll, sqlite::ConnectionPool* const pool){
        const std::vector<CodeNumber> code_numbers{code_numbers_ptr, code_numbers_ptr + code_numbers_len};
    try{

        const auto code_sequence = CodeSequence{code_numbers};
        const auto code_type = code_sequence.type();

        sqlite::PooledConnection conn{*pool};

        Info info = database::load_info(code_sequence, code_type, conn.db);
        InitialAngles initial_angles = database::deserialize<InitialAngles>(info.initial_angles);
        CodeInfo infoAll;
        if(code_sequence.is_stable()){
           infoAll = calculate_stable_all_info(code_sequence,initial_angles);
        }
        else{
          infoAll = calculate_unstable_all_info(code_sequence,initial_angles);
        }
        copy_to_cinfoAll( infoAll , cinfoAll );
        return 1;
    }
    catch (const std::runtime_error& except) {
        std::cerr << code_numbers << " failed with error:\n"
                      << except.what() << std::endl;
        return -1;
    }
}



int32_t load_info_all(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len, CInfoAll* const cinfoAll, sqlite::ConnectionPool* const pool){
    const std::vector<CodeNumber> code_numbers{code_numbers_ptr, code_numbers_ptr + code_numbers_len};
    try{

        const auto code_sequence = CodeSequence{code_numbers};
        const auto code_type = code_sequence.type();

        sqlite::PooledConnection conn{*pool};
        Info info = database::load_info(code_sequence, code_type, conn.db);
        InitialAngles initial_angles = database::deserialize<InitialAngles>(info.initial_angles);

        //std::cout << "4" << std::endl;
        const auto infoAll = calculate_all_info(code_sequence, initial_angles);
        //std::cout << "5" << std::endl;
        copy_to_cinfoAll( infoAll , cinfoAll );
        //std::cout << "6" << std::endl;
        return 1;
    }
    catch (const std::runtime_error& except) {
        std::cerr << code_numbers << " failed with error:\n"
                      << except.what() << std::endl;
        return -1;
    }
}


// Works for Stable and Unstable
static void copy_to_cinfo(const Info& info, CInfo* const cinfo) {

    char* initial_angles = nullptr;
    char* points = nullptr;
    char* equations = nullptr;
    char* left_rights = nullptr;
    char* code_seq_lr = nullptr;
    try {
        initial_angles = to_cstr(info.initial_angles);
        points = to_cstr(info.points);
        equations = to_cstr(info.equations);
        left_rights = to_cstr(info.left_rights);
        code_seq_lr = to_cstr(info.code_seq_lr);
    } catch (...) {
        delete[] initial_angles;
        delete[] points;
        delete[] equations;
        delete[] left_rights;
        delete[] code_seq_lr;
        throw;
    }

    cinfo->initial_angles = initial_angles;
    cinfo->points = points;
    cinfo->equations = equations;
    cinfo->left_rights = left_rights;
    cinfo->code_seq_lr = code_seq_lr;
}


// -1 means error doing calculation
// 0 means empty set
// 1 means non empty set
int32_t load_info(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len, CInfo* const cinfo, sqlite::ConnectionPool* const pool) {

    clear_backend_error();

    const std::vector<CodeNumber> code_numbers{code_numbers_ptr, code_numbers_ptr + code_numbers_len};

    // We throw runtime_errors to indicate problems with the program. When this happens,
    // we want to handle it gracefully. We simply catch the error, print out an error
    // message, and return 2 to indicate the error
    // Errors that we don't throw, like a std::bad_alloc, we allow through, since those
    // are really bad errors
    try {

        const auto code_sequence = CodeSequence{code_numbers};

        const auto code_type = code_sequence.type();

        sqlite::PooledConnection conn{*pool};

        const auto in = save_to_database(code_sequence, code_type, conn.db);

        if (in) {
            const auto info = database::load_info(code_sequence, code_type, conn.db);
            copy_to_cinfo(info, cinfo);
        }

        return in;

    } catch (const std::runtime_error& except) {
        remember_backend_error("load_info", except);

        return -1;
    }
}

static void copy_to_cinfoAll_2(const CodeInfo& info, CInfoAll* const cinfoAll) {
    char* sin_equations = nullptr;
    char* cos_equations = nullptr;
    char* initial_angles = nullptr;
    char* points = nullptr;
    char* equations = nullptr;
    char* left_rights = nullptr;
    char* code_seq_lr = nullptr;
    char* vector_x = nullptr;
    char* vector_y = nullptr;
    try {
        sin_equations = to_cstr("");
        cos_equations = to_cstr("");
        initial_angles = to_cstr("");
        points = to_cstr("");
        equations = to_cstr("");
        left_rights = to_cstr("");
        code_seq_lr = to_cstr("");
        vector_x = to_cstr(database::serialize(info.sin_equations));
        vector_y = to_cstr(database::serialize(info.cos_equations));
    } catch (...) {
        delete[] sin_equations;
        delete[] cos_equations;
        delete[] initial_angles;
        delete[] points;
        delete[] equations;
        delete[] left_rights;
        delete[] code_seq_lr;
        delete[] vector_x;
        delete[] vector_y;
        throw;
    }

    cinfoAll->sinEquations = sin_equations;
    cinfoAll->cosEquations = cos_equations;
    cinfoAll->initial_angles = initial_angles;
    cinfoAll->points = points;
    cinfoAll->equations = equations;
    cinfoAll->left_rights = left_rights;
    cinfoAll->code_seq_lr = code_seq_lr;
    cinfoAll->vectorX = vector_x;
    cinfoAll->vectorY = vector_y;
}

int32_t load_slope_info(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len, CInfoAll* const cinfoAll, sqlite::ConnectionPool* const pool) {
    const std::vector<CodeNumber> code_numbers{code_numbers_ptr, code_numbers_ptr + code_numbers_len};
    try{

        const auto code_sequence = CodeSequence{code_numbers};
        const auto code_type = code_sequence.type();

        sqlite::PooledConnection conn{*pool};
        Info info = database::load_info(code_sequence, code_type, conn.db);
        InitialAngles initial_angles = database::deserialize<InitialAngles>(info.initial_angles);

        const auto AllVector = calculate_all_vector(code_sequence, initial_angles);
        copy_to_cinfoAll_2(AllVector, cinfoAll);
        return 1;
    }
    catch (const std::runtime_error& except) {
        std::cerr << code_numbers << " failed with error:\n"
                      << except.what() << std::endl;
        return -1;
    }
}


void cleanup_cinfo(const CInfo* const cinfo) {
    delete[] cinfo->initial_angles;
    delete[] cinfo->points;
    delete[] cinfo->equations;
    delete[] cinfo->left_rights;
    delete[] cinfo->code_seq_lr;
}

void cleanup_cinfo_all(const CInfoAll* const cinfoAll) {
    delete[] cinfoAll->initial_angles;
    delete[] cinfoAll->points;
    delete[] cinfoAll->equations;
    delete[] cinfoAll->sinEquations;
    delete[] cinfoAll->cosEquations;
    delete[] cinfoAll->left_rights;
    delete[] cinfoAll->code_seq_lr;
    delete[] cinfoAll->vectorX;
    delete[] cinfoAll->vectorY;
}

// 0 is failure,
// any non-zero is success
int32_t merge_covers(const char* const merge_dir_ptr, const char* const cover_dirs_ptr, sqlite::ConnectionPool* const pool) {

    try {
        const std::string merge_dir{merge_dir_ptr};

        const auto cover_dirs = split(cover_dirs_ptr, "\n");

        sqlite::PooledConnection conn{*pool};

        union_covers(merge_dir, cover_dirs, conn.db);

        return 1;
    } catch (const std::runtime_error& except) {
        std::cerr << "Merging of covers failed with error:\n"
                  << except.what() << std::endl;

        return 0;
    }
}

int32_t code_search_length(const int32_t code_type_int, const int32_t length_int, CString* const cstring, sqlite::ConnectionPool* const pool) {

    try {
        // Hurdur, this can cause errors if you're not careful!
        const auto code_type = static_cast<CodeType>(code_type_int);
        const auto length = boost::numeric_cast<size_t>(length_int);

        sqlite::PooledConnection conn{*pool};

        const auto str = code_search(code_type, length, conn.db);

        cstring->string = to_cstr(str);

        return 1;

    } catch (const std::runtime_error& except) {
        std::cerr << "Searching for codes failed with error:\n"
                  << except.what() << std::endl;

        return -1;
    }
}

int32_t code_search_even_odd(const int32_t code_type_int, const char* const even_odd, CString* const cstring, sqlite::ConnectionPool* const pool) {

    try {
        // Hurdur, this can cause errors if you're not careful!
        const auto code_type = static_cast<CodeType>(code_type_int);

        sqlite::PooledConnection conn{*pool};

        const auto str = code_search(code_type, even_odd, conn.db);

        cstring->string = to_cstr(str);

        return 1;

    } catch (const std::runtime_error& except) {
        std::cerr << "Searching for codes failed with error:\n"
                  << except.what() << std::endl;

        return -1;
    }
}

int32_t trim_cover(const char* const poly_str, const char* const in_dir, const char* const out_dir) {

    try {
        const auto polygon = parse_polygon(poly_str);

        trim_cover(polygon, in_dir, out_dir);

        return 1;

    } catch (const std::runtime_error& except) {
        std::cerr << "Trimming cover failed with error:\n"
                  << except.what() << std::endl;
        return -1;
    }
}

static std::string bounding_polygon(const CodeSequence& code_seq, sqlite::Database& db) {

    const auto code_type = code_seq.type();

    const std::string sql = "select initial_angles from " + database::serialize(code_type) + " where code_sequence = ?;";

    std::string initial_angles_str{};
    db.prepare(sql).bind(database::serialize(code_seq)).exec(initial_angles_str);

    const auto initial_angles = parse_initial_angles(initial_angles_str);

    const auto points = bounding_polygon(code_seq, initial_angles);

    bool first = true;
    std::ostringstream oss{};
    for (const auto& point : points) {

        if (!first) {
            oss << '\n';
        }

        const auto x = static_cast<Float>(point.x) * boost::math::constants::half_pi<Float>();
        const auto y = static_cast<Float>(point.y) * boost::math::constants::half_pi<Float>();

        oss << x << ' ' << y;

        first = false;
    }

    return oss.str();
}

int32_t bounding_polygon(const int32_t* const code_numbers_ptr, const int32_t code_numbers_len, CString* const cstring, sqlite::ConnectionPool* const pool) {

    try {

        const std::vector<CodeNumber> code_numbers{code_numbers_ptr, code_numbers_ptr + code_numbers_len};

        const CodeSequence code_seq{code_numbers};

        sqlite::PooledConnection conn{*pool};

        const auto str = bounding_polygon(code_seq, conn.db);

        cstring->string = to_cstr(str);

        return 1;

    } catch (const std::runtime_error& except) {
        std::cerr << "Bounding polygon failed with error:\n"
                  << except.what() << std::endl;
        return -1;
    }
}


/**
 * The following 4 functions were written to calculate the gradient formula derived by George
 */
template <template <typename> class T>
static Equation<T> parse_equation_database(const std::string& equation_str) {
    const auto sub = equation_str.substr(4);
    const auto eq = database::deserialize<Equation<T>>(sub);
    return eq;
}

void replaceAll(std::string& str, const std::string& from, const std::string& to) {
    if(from.empty())
        return;
    size_t start_pos = str.find(from, 0);
    while(start_pos != std::string::npos) {
        str.replace(start_pos, from.length(), to);
        start_pos = str.find(from, start_pos + to.length());
    }
}

void parse_term(std::string& coeff, std::stringstream& buffer) {
    if (buffer.str() == "") {
        coeff = "1";
    }
    else if (buffer.str() == "-") {
        coeff = "-1";
    }
    else {
        coeff = buffer.str();
    }
    buffer.str("");
}

template <template <typename> class T>
static Equation<T> parse_equation_info(const std::string& equation_str) {
    std::string eq_str = equation_str;
    std::vector<std::string> terms{};
    std::ostringstream oss{};
    replaceAll(eq_str, "cos", "s");
    replaceAll(eq_str, "sin", "s");
    replaceAll(eq_str, "(", "");
    replaceAll(eq_str, "+", "");
    std::string tmp = eq_str.substr(0, eq_str.length() - 1);
    boost::algorithm::split(terms, tmp, boost::is_any_of(")"));
    for(const auto term : terms) {
        //std::cout << term << std::endl;
        std::stringstream buffer{""};
        std::string coeff{""};
        std::string x_coeff{""};
        std::string y_coeff{""};
        for (const auto token : term) {
            if (token == 's') {
                parse_term(coeff, buffer);
                continue;
            }
            else if (token == 'x') {
                parse_term(x_coeff, buffer);
                continue;
            }
            else if (token == 'y') {
                parse_term(y_coeff, buffer);
                continue;
            }
            else {
                buffer << token;
            }
        }
        oss << coeff << " ";
        if (x_coeff == "") {
            oss << "0 ";
        }
        else {
            oss << x_coeff << " ";
        }
        if (y_coeff == "") {
            oss << "0 ";
        }
        else {
            oss << y_coeff << " ";
        }
    }
    //const auto eq = database::deserialize<Equation<T>>("1 1 1");
    const auto eq = database::deserialize<Equation<T>>(oss.str().substr(0, oss.str().length() - 1));
    //std::cout << eq << std::endl;
    //std::cout << oss.str() << std::endl;
    return eq;
}
/*
template <template <typename> class T>
//template <typename T>
static int32_t get_bound(const Equation<T> equation) {
    int32_t sum = 0;
    for (const auto& kv : equation) {
        const auto& arg = kv.first.arg;
        const auto x_coeff = arg.coeff<XY::X>();
        const auto y_coeff = arg.coeff<XY::Y>();
        sum += std::abs(kv.second) * (std::abs(x_coeff) + std::abs(y_coeff));
    }
    return sum;
}*/

template <template <typename> class T>
static float64_t equation_stuff_first_only(const Equation<T> equation, float64_t x_value, float64_t y_value, std::ostringstream& oss, bool is_p) {

    const auto first_derivative_x = diff<XY::X>(equation);
    const auto first_derivative_y = diff<XY::Y>(equation);
    float64_t f_x = evalf<float64_t>(first_derivative_x, x_value, y_value);
    float64_t f_y = evalf<float64_t>(first_derivative_y, x_value, y_value);
    oss << "\nFirst Derivative: " << "\n";
    if(is_p){
      oss << bound<float64_t>(first_derivative_x) <<" ,P_x(x, y) = " << first_derivative_x << " = " << f_x << "\n";
      oss << bound<float64_t>(first_derivative_y) <<" ,P_y(x, y) = " << first_derivative_y << " = " << f_y << "\n";
    }else{
      oss << bound<float64_t>(first_derivative_x) <<" ,Q_x(x, y) = " << first_derivative_x << " = " << f_x << "\n";
      oss << bound<float64_t>(first_derivative_y) <<" ,Q_y(x, y) = " << first_derivative_y << " = " << f_y << "\n";
    }
    oss << "sum is "<< bound<float64_t>(first_derivative_y)+bound<float64_t>(first_derivative_y) << "\n";
    return bound<float64_t>(first_derivative_y)+bound<float64_t>(first_derivative_y);
}

template <template <typename> class T>
static std::string equation_stuff(const Equation<T> equation, float64_t x_value, float64_t y_value, std::ostringstream& oss,bool is_sin) {
    std::ostringstream oss2{};
    std::ostringstream oss3{};
    std::ostringstream oss4{};

    float64_t f = evalf<float64_t>(equation, x_value, y_value);
    oss << "Original Function: " << "\n";
    oss << bound<float64_t>(equation) <<" ,F(x, y) = " << equation << " = " << f << "\n";

    const auto first_derivative_x = diff<XY::X>(equation);
    const auto first_derivative_y = diff<XY::Y>(equation);
    float64_t f_x = evalf<float64_t>(first_derivative_x, x_value, y_value);
    float64_t f_y = evalf<float64_t>(first_derivative_y, x_value, y_value);
    oss << "\nFirst Derivative: " << "\n";

    oss << bound<float64_t>(first_derivative_x) <<" ,F_x(x, y) = " << first_derivative_x << " = " << f_x << "\n";
    oss << bound<float64_t>(first_derivative_y) <<" ,F_y(x, y) = " << first_derivative_y << " = " << f_y << "\n";

    const auto second_derivative_xx = diff<XY::X>(first_derivative_x);
    const auto second_derivative_xy = diff<XY::Y>(first_derivative_x);
    const auto second_derivative_yy = diff<XY::Y>(first_derivative_y);
    float64_t f_xx = evalf<float64_t>(second_derivative_xx, x_value, y_value);
    float64_t f_xy = evalf<float64_t>(second_derivative_xy, x_value, y_value);
    float64_t f_yy = evalf<float64_t>(second_derivative_yy, x_value, y_value);
    oss << "\nSecond Derivative: " << "\n";
    oss << bound<float64_t>(second_derivative_xx) <<" ,F_xx(x, y) = " << second_derivative_xx << " = " << f_xx << "\n";
    oss <<bound<float64_t>(second_derivative_xy)<< " ,F_xy(x, y) = " << second_derivative_xy << " = " << f_xy << "\n";
    oss <<bound<float64_t>(second_derivative_yy)<< " ,F_yy(x, y) = " << second_derivative_yy << " = " << f_yy << "\n";

        float64_t dy_dx_first = -(f_x/f_y);
        float64_t p_xy = -((f_xx * f_y * f_y - 2 * f_xy * f_x * f_y + f_yy * f_x * f_x));
        float64_t q_xy = f_y * f_y * f_y;
        float64_t dy_dx_second = p_xy / q_xy;
        float64_t abs_p_xy = std::abs(p_xy);
        float64_t abs_q_xy = std::abs(q_xy);
    oss << "\n";
    oss << "dy/dx = " << dy_dx_first << "\n";
    oss << "d^2y/dx^2 = " << dy_dx_second << "\n";
    const auto F_y_square = multiply_square(first_derivative_y);
    const auto F_y_cubic = multiply_cubic(first_derivative_y);
    const auto F_x_square = multiply_square(first_derivative_x);
    //to get inter1
    const auto inter1 = multiply_lin_com(second_derivative_xx, F_y_square);
    //to get inter3
    const auto inter2 = multiply_lin_com(second_derivative_xy,first_derivative_x);
    const auto inter3 = multiply_lin_com(inter2,first_derivative_y);

    //to get inter5
    const auto inter4 = multiply_lin_com(second_derivative_yy,F_x_square);
    const auto inter5 = get_final_result_formula(inter1,inter3,inter4);



    oss << "d^2y/dx^2 = " <<"\n";
    //oss <<"numerator: "<< "-(" << inter1 << ")+("<< inter4 <<")+("<< inter4 <<")-("<<inter5<<")"<<"="<< p_xy <<"\n";
    oss<<"numerator: "<< inter5<<"\n";
    oss <<"denominator: "<< F_y_cubic << "=" << q_xy << "\n";
    //oss << "bound for the numerator: " << bound<float64_t>(inter1) + bound<float64_t>(inter4) + bound<float64_t>(inter5) << "\n";
    oss << "bound for the numerator: " << bound<float64_t>(inter5) << "\n";
    oss << "bound for the denominator: " << bound<float64_t>(F_y_cubic) << "\n";
    //oss2 << inter5 ;

    //std::string numer = oss2.str();
    //oss3 << F_y_cubic;
    //std::string denom = oss3.str();
    float64_t sum;
    float64_t sum2;
    /*if(inter1==0&&inter2==0&&inter3==0&&inter4==0){

    }*/
    //if (numer!="0"||demon!="0"){
        /*if (numer.find("sin") != std::string::npos) {
               oss << "for numerator:";
               //const auto equation = parse_equation_info<Sin>(numer);
               sum = equation_stuff_first_only(equation, x_value, y_value, oss,true);
           } else if (numer.find("cos") != std::string::npos) {
              oss << "for numerator:";
               //const auto equation = parse_equation_info<Cos>(numer);

           } else {
               throw std::runtime_error("unable to parse equation: " + numer);
           }*/
           oss << "for numerator:";
           sum = equation_stuff_first_only(inter5, x_value, y_value, oss,true);
           float64_t r1 = abs_p_xy / std::abs(sum);
           oss << "r1 = "<< r1 << "\n";
           oss << "for denominator:";
           sum2 = equation_stuff_first_only(F_y_cubic, x_value, y_value, oss,false);
           float64_t r2 = abs_q_xy / std::abs(sum2);
           oss << "r2 = "<< r2 << "\n";
           /*
           if (denom.find("sin") != std::string::npos) {
              oss << "for denominator:";
               const auto equation = parse_equation_info<Sin>(denom);
                sum2 = equation_stuff_first_only(equation, x_value, y_value, oss,false);
           } else if (denom.find("cos") != std::string::npos) {
                oss << "for denominator:";
               const auto equation = parse_equation_info<Cos>(denom);
               sum2 = equation_stuff_first_only(equation, x_value, y_value, oss, false);
           } else {
               throw std::runtime_error("unable to parse equation: " + denom);
           }*/



           if( r2 < r1 ){
                oss << "min is " << r2 << "\n";
                std::ostringstream oss5{};
                oss5 << r2;
                return oss5.str();
           }
           else{
                oss << "min is " << r1 << "\n";
                 std::ostringstream oss6{};
                 oss6 << r1;
                 return oss6.str();
           }

    //}

}

float64_t calculate_gradient(const char* const equation_cstr, float64_t x_value, float64_t y_value, bool from_database, CString* const cstring, CString* const cstring2) {
    try {
        std::string equation_str {equation_cstr};
        std::ostringstream oss {};
        std::string min_r;
        if (from_database) {
            if (boost::starts_with(equation_str, "sin")) {
                const auto equation = parse_equation_database<Sin>(equation_str);
                min_r = equation_stuff(equation, x_value, y_value, oss,true);
            } else if (boost::starts_with(equation_str, "cos")) {
                const auto equation = parse_equation_database<Cos>(equation_str);
                min_r = equation_stuff(equation, x_value, y_value, oss,false);
             } else {
                throw std::runtime_error("unable to parse equation: " + equation_str);
            }
        } else {
            if (equation_str.find("sin") != std::string::npos) {
               const auto equation = parse_equation_info<Sin>(equation_str);
               min_r = equation_stuff(equation, x_value, y_value, oss,true);

            } else if (equation_str.find("cos") != std::string::npos) {
               const auto equation = parse_equation_info<Cos>(equation_str);
               min_r = equation_stuff(equation, x_value, y_value, oss,false);
             } else {
                throw std::runtime_error("unable to parse equation: " + equation_str);
            }
        }
        cstring->string = to_cstr(oss.str());
        cstring2->string = to_cstr(min_r);
        return 1;
    } catch (const std::runtime_error& except) {
        std::cerr << "calculating grdient failed : " << except.what() << std::endl;
        return -1;
    }
}

void cleanup_string(const CString* const cstring) {
    if (cstring != nullptr) {
        delete[] cstring->string;
    }
}

/* jul 31 2025 Marco Mai
 * backend connection to VaryCS
*/
int vary_cs_cpp(const int32_t int_movesMin, const int32_t int_movesMax, const float64_t int_xAngle, const float64_t int_yAngle,CString* const result,const char* const reqTypes){
    try {
        std::string selectedTypes = std::string(reqTypes);
        std::vector<std::vector<int32_t>> founded_codes = fireAwayCS(int_movesMin,int_movesMax,int_xAngle,int_yAngle,selectedTypes);

        std::string buffer;
        size_t estimated_size = founded_codes.size() * 32; // adjust as needed
        buffer.reserve(estimated_size);  // Reserve space to avoid reallocations

        char temp[20];  // buffer for each int (enough for 64-bit integers)

        for (const auto& row : founded_codes) {
            for (int value : row) {
                int len = std::snprintf(temp, sizeof(temp), "%d ", value);
                buffer.append(temp, len);
            }
            buffer.push_back('\n');
        }

        result->string = to_cstr(std::move(buffer)); 
        return 1;
    } catch (const std::runtime_error& except) {
        std::cerr << "vary cs failed : " << except.what() << std::endl;
        return -1;
    }
}

/* jul 31 2025 Marco Mai
 * backend connection to Vary3
*/
int vary_3_cpp(const int32_t int_movesMin, const int32_t int_movesMax,const float64_t db_initPosition, const float64_t db_xAngle, const float64_t db_yAngle,CString* const result,const char* const reqTypes){
    try {
        std::string selectedTypes = std::string(reqTypes);
        std::vector<std::vector<int32_t>> founded_codes = fireAway3(int_movesMin,int_movesMax, db_xAngle,db_yAngle,db_initPosition,selectedTypes);
        
        // std::vector<std::vector<int>> to string
        std::ostringstream oss;
        for (const auto& row : founded_codes) {
            for (int value : row) {
                oss << value << ' ';
            }
            oss << '\n';
        }
            // return boost::none;
        result->string = to_cstr(oss.str());
        return 1;
    } catch (const std::runtime_error& except) {
        std::cerr << "vary 3 failed : " << except.what() << std::endl;
        return -1;
    }
}


/* Aug 05 2025 Marco Mai
 * backend connection to Vary4
*/
int vary_4_cpp(const int32_t int_movesMin, const int32_t int_movesMax, const float64_t db_xAngle, const float64_t db_yAngle,CString* const result,const char* const reqTypes){
    try {
        std::string selectedTypes = std::string(reqTypes);
        std::vector<std::vector<int32_t>> founded_codes = fireAway4(int_movesMin,int_movesMax, db_xAngle,db_yAngle,selectedTypes);
        
        // std::vector<std::vector<int>> to string
        std::ostringstream oss;
        for (const auto& row : founded_codes) {
            for (int value : row) {
                oss << value << ' ';
            }
            oss << '\n';
        }
            // return boost::none;
        result->string = to_cstr(oss.str());
        return 1;
    } catch (const std::runtime_error& except) {
        std::cerr << "vary 3 failed : " << except.what() << std::endl;
        return -1;
    }


}

// cancel
void backend_cancel()       { 
    cancel_flag().store(true,  std::memory_order_relaxed);  
}
