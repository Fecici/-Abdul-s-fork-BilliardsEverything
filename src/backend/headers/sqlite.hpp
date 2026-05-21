#pragma once

#include <iostream>
#include <mutex>
#include <queue>
#include <sqlite3.h>
#include <sstream>
#include <string>
#include <condition_variable>

// TODO statement caching would also be helpful here. In fact, very helpful.
// No, I think it is better to provide the ability to reuse statements, and
// then let the user deal with reusing things as necessary.

// TODO since sqlite only supports one writer at a time, so all writers need
// to be serialized. Because of this, we could have many threads blocking
// to write their results. It might be better to have some sort of queue where
// threads drop off their results to be written. That would be more of an application
// thing though, not wrapper thing

// We need to create two new classes: a binding class and a result class
// This adds type safety, because certain methods can only be called
// on those classes
// We also need to change to unique_ptr and shared_ptr, and give each
// statement a ptr to its connection for better error messages

// result codes are two types: error and non-error
// non-error are SQLITE_OK, SQLITE_ROW, SQLITE_DONE
// error are everything else. calling any of the error message
// apis is undefined if one of the above three is returned
// primary error codes and extended error codes.

// make it nice and simple right now
// when an error happens, we should at minimum try to close everything
// that may not succeed, and perhaps we should do something if that's
// the case, but we should at least try to close everything

// calling sqlite3_errcode(), sqlite3_extended_errcode(), or sqlite3_errmsg() is undefined
// if the last api call did not return an error code

// If SQLITE_MISUSE happens, then the above functions might not have a reliable
// result

// These do not include the extended error codes right now
inline std::string result_code_string(const int result_code) {
    switch (result_code) {
    case SQLITE_ABORT:
        return "SQLITE_ABORT";
    case SQLITE_AUTH:
        return "SQLITE_AUTH";
    case SQLITE_BUSY:
        return "SQLITE_BUSY";
    case SQLITE_CANTOPEN:
        return "SQLITE_CANTOPEN";
    case SQLITE_CONSTRAINT:
        return "SQLITE_CONSTRAINT";
    case SQLITE_CORRUPT:
        return "SQLITE_CORRUPT";
    case SQLITE_DONE:
        return "SQLITE_DONE";
    case SQLITE_EMPTY:
        return "SQLITE_EMPTY";
    case SQLITE_ERROR:
        return "SQLITE_ERROR";
    case SQLITE_FORMAT:
        return "SQLITE_FORMAT";
    case SQLITE_FULL:
        return "SQLITE_FULL";
    case SQLITE_INTERNAL:
        return "SQLITE_INTERNAL";
    case SQLITE_INTERRUPT:
        return "SQLITE_INTERRUPT";
    case SQLITE_IOERR:
        return "SQLITE_IOERR";
    case SQLITE_LOCKED:
        return "SQLITE_LOCKED";
    case SQLITE_MISMATCH:
        return "SQLITE_MISMATCH";
    case SQLITE_MISUSE:
        return "SQLITE_MISUSE";
    case SQLITE_NOLFS:
        return "SQLITE_NOLFS";
    case SQLITE_NOMEM:
        return "SQLITE_NOMEM";
    case SQLITE_NOTADB:
        return "SQLITE_NOTADB";
    case SQLITE_NOTFOUND:
        return "SQLITE_NOTFOUND";
    case SQLITE_NOTICE:
        return "SQLITE_NOTICE";
    case SQLITE_OK:
        return "SQLITE_OK";
    case SQLITE_PERM:
        return "SQLITE_PERM";
    case SQLITE_PROTOCOL:
        return "SQLITE_PROTOCOL";
    case SQLITE_RANGE:
        return "SQLITE_RANGE";
    case SQLITE_READONLY:
        return "SQLITE_READONLY";
    case SQLITE_ROW:
        return "SQLITE_ROW";
    case SQLITE_SCHEMA:
        return "SQLITE_SCHEMA";
    case SQLITE_TOOBIG:
        return "SQLITE_TOOBIG";
    case SQLITE_WARNING:
        return "SQLITE_WARNING";
    default:
        return "unknown result code: " + std::to_string(result_code);
    }
}

inline std::string error_message(const int error_code) {
    const auto errcode = result_code_string(error_code); // name of error code
    const auto errstr = sqlite3_errstr(error_code);      // message of the error_code
    return errcode + " - " + std::string{errstr};
}

inline std::string error_message(const int error_code, sqlite3* const db) {
    const auto errmsg = sqlite3_errmsg(db); // actual error message
    return error_message(error_code) + " - " + std::string{errmsg};
}

inline void error_log_callback(void*, const int error_code, const char* const error_message) {
    fprintf(stderr, "(%d) %s\n", error_code, error_message);
}

namespace sqlite {

// IMPORTANT: each process should call this function *once*, *before* any connections are opened
// It is not threadsafe, but multiple processes can call it
inline void error_logging() {
    const auto result_code = sqlite3_config(SQLITE_CONFIG_LOG, error_log_callback, nullptr);
    if (result_code != SQLITE_OK) {
        throw std::runtime_error(error_message(result_code));
    }
}

// We could have two classes: an in class for binding and then an
// out class for execing.
// We also use a unique_ptr here too
class Statement final {

    friend class Database;

  public:
    // TODO use a unique_ptr here?
    sqlite3_stmt* stmt;

    explicit Statement(sqlite3_stmt* const stmt_)
        : stmt{stmt_} {}

    // TODO explicitly delete the other copy and move functions?

    Statement(Statement&& other)
        : stmt{other.stmt} {
        other.stmt = nullptr;
    }

    ~Statement() {
        const auto result_code = sqlite3_finalize(stmt);
        if (result_code != SQLITE_OK) {
            // We can't throw exceptions in a destructor, so this is
            // the next best thing
            std::cerr << error_message(result_code) << std::endl;
        }
    }

    void bind_value(const int index, const double val) {

        const auto result_code = sqlite3_bind_double(stmt, index, val);
        if (result_code != SQLITE_OK) {
            // TODO have a reference to the database so we can use error_message?
            throw std::runtime_error(error_message(result_code));
        }
    }

    void bind_value(const int index, const int64_t val) {

        const auto result_code = sqlite3_bind_int64(stmt, index, val);
        if (result_code != SQLITE_OK) {
            // TODO have a reference to the database so we can use error_message?
            throw std::runtime_error(error_message(result_code));
        }
    }

    void bind_value(const int index, const std::string& val) {

        const auto result_code = sqlite3_bind_text(stmt, index, val.c_str(), -1, SQLITE_TRANSIENT);
        if (result_code != SQLITE_OK) {
            // TODO have a reference to the database so we can use error_message?
            throw std::runtime_error(error_message(result_code));
        }
    }

    void bind_unpack(const int) {}

    template <typename T, typename... Args>
    void bind_unpack(const int index, const T& val, const Args&... args) {
        bind_value(index, val);
        bind_unpack(index + 1, args...);
    }

    // TODO rvalue && and stuff?
    template <typename... Args>
    Statement&& bind(const Args&... args) {

        const auto parameters = sqlite3_bind_parameter_count(stmt);

        if (sizeof...(args) != parameters) {
            std::ostringstream err{};
            err << "given " << sizeof...(args) << " parameters to bind, expected " << parameters;
            throw std::runtime_error(err.str());
        }

        // Bindings start with an index of 1
        bind_unpack(1, args...);

        return std::move(*this);
    }

    void get_value(const int col, double& val) {

        // TODO more helpful error message here
        if (sqlite3_column_type(stmt, col) != SQLITE_FLOAT) {
            throw std::runtime_error("attempt to access double for col that isn't double");
        }

        val = sqlite3_column_double(stmt, col);
    }

    void get_value(const int col, int64_t& val) {

        if (sqlite3_column_type(stmt, col) != SQLITE_INTEGER) {
            throw std::runtime_error("attempt to access integer for col that isn't integer");
        }

        val = sqlite3_column_int64(stmt, col);
    }

    void get_value(const int col, std::string& val) {

        if (sqlite3_column_type(stmt, col) != SQLITE_TEXT) {
            throw std::runtime_error("attempt to access text that isn't text");
        }

        const auto text = sqlite3_column_text(stmt, col);

        if (text == nullptr) {
            // This isn't a conversion from null to text, so probably an error
            throw std::runtime_error("null pointer returned from column_text");
        }

        // It is safest to call this after the call to text
        const auto bytes = sqlite3_column_bytes(stmt, col);

        // The returned string is zero-terminated, but getting
        // the bytes and passing that should be faster

        // Use the iterator constructor. Very nice
        val = {text, text + bytes};
    }

    void column_unpack(const int) {}

    template <typename T, typename... Args>
    void column_unpack(const int index, T& val, Args&... args) {
        get_value(index, val);
        column_unpack(index + 1, args...);
    }

    // It is important that these are passed by reference so we can mutate them
    // This function expects that a result returns a single column
    template <typename... Args>
    void exec(Args&... args) {

        const auto columns = sqlite3_column_count(stmt);

        if (sizeof...(args) != columns) {
            std::ostringstream err{};
            err << "given " << sizeof...(args) << " columns to retrieve, expected " << columns;
            throw std::runtime_error(err.str());
        }

        // There are values to return, so step once to retrieve them
        if (columns != 0) {

            // Try stepping the statement until it's not busy
            // TODO is there a better way of dealing with this?
            // If the statement occurs within a transaction and is not a commit,
            // then this needs to be rolled back. As of now, we can just retry
            auto result_code = SQLITE_BUSY;
            while (result_code == SQLITE_BUSY) {
                result_code = sqlite3_step(stmt);
            }

            // We didn't get a value
            if (result_code == SQLITE_DONE) {
                throw std::runtime_error("error: no rows retrieved");
            } else if (result_code != SQLITE_ROW) {
                throw std::runtime_error(error_message(result_code));
            }

            // now we get all our values
            column_unpack(0, args...);
        }

        // We assume there is only one value to retrieve, so just step once more to
        // make sure there are no more rows left
        auto result_code = SQLITE_BUSY;
        while (result_code == SQLITE_BUSY) {
            result_code = sqlite3_step(stmt);
        }

        if (result_code == SQLITE_ROW) {
            throw std::runtime_error("error: more than one row retrieved");
        } else if (result_code != SQLITE_DONE) {
            throw std::runtime_error(error_message(result_code));
        }
    }

    template <typename... Args>
    bool step(Args&... args) {

        // Do a column check on each step. Only needs to be done once really
        const auto columns = sqlite3_column_count(stmt);

        if (sizeof...(args) != columns) {
            std::ostringstream err{};
            err << "given " << sizeof...(args) << " columns to retrieve, expected " << columns;
            throw std::runtime_error(err.str());
        }

        // Step the statement
        auto result_code = SQLITE_BUSY;
        while (result_code == SQLITE_BUSY) {
            result_code = sqlite3_step(stmt);
        }

        if (result_code == SQLITE_DONE) {
            // Finished, nothing left to extract
            return false;
        } else if (result_code == SQLITE_ROW) {
            // Extract a single row of results
            column_unpack(0, args...);
            return true;
        } else {
            throw std::runtime_error(error_message(result_code));
        }
    }
};

enum class Open : int {
    Readonly = SQLITE_OPEN_READONLY,
    Readwrite = SQLITE_OPEN_READWRITE,
    Create = SQLITE_OPEN_CREATE,
    Uri = SQLITE_OPEN_URI,
    Memory = SQLITE_OPEN_MEMORY,
    Nomutex = SQLITE_OPEN_NOMUTEX,
    Fullmutex = SQLITE_OPEN_FULLMUTEX,
    Sharedcache = SQLITE_OPEN_SHAREDCACHE,
    Privatecache = SQLITE_OPEN_PRIVATECACHE,
};

constexpr Open operator|(const Open a, const Open b) {
    return static_cast<Open>(static_cast<int>(a) | static_cast<int>(b));
}

// This class has the semantics of a unique_ptr
// It is simpler this way
// TODO this should really be called Connection
class Database final {

  public:
    // TODO make this private
    sqlite3* db;

    explicit Database(const std::string& db_path, const Open flags) {

        db = nullptr;
        const auto result_code = sqlite3_open_v2(db_path.c_str(), &db, static_cast<int>(flags), nullptr);
        if (result_code != SQLITE_OK) {
            //sqlite3_close(db);
            throw std::runtime_error(error_message(result_code, db));
        }
    }

    // need to delete the copy constructor and assignment
    // we probably need move so we can return from functions

    // can use a single operator= to define copy assingment and move
    // assignment using the copy-and-swap idiom
    // (and swap)

    //explicit Database(const Database&) = delete;
    //void operator=(const Database&) = delete;

    // Move and copy constructors should not be explicit

    Database(Database&& other)
        : db{other.db} {
        // sqlite3_close is a nop on nullptrs
        other.db = nullptr;
    }

    ~Database() {
        const auto result_code = sqlite3_close(db);
        if (result_code != SQLITE_OK) {
            std::cerr << error_message(result_code, db) << std::endl;
        }
    }

    Statement prepare(const std::string& sql) {

        sqlite3_stmt* stmt = nullptr;
        const auto result_code = sqlite3_prepare_v2(db, sql.c_str(), -1, &stmt, nullptr);

        if (result_code != SQLITE_OK) {
            throw std::runtime_error(error_message(result_code, db));
        }

        return Statement{stmt};
    }
};

// Another idea is using a lockfree queue, but that would require using
// sqlite* instead of Databases I think. Or, we could use indices with a
// vector, which I think is simpler

class ConnectionPool final {

    friend class PooledConnection;

  private:
    size_t pool_size;
    std::mutex mut;
    std::queue<Database> connections;
    std::condition_variable cv;

    // Take ownership
    Database borrow() {
//        std::lock_guard<std::mutex> lock{mut};

        // TODO should we block until a connection becomes available?
        // In this case we should be careful that all existing connections
        // are returned, or we could get starvation. By default, connections
        // are returned upon throwing an exception. Of course, they are not
        // returned if an abort/exit/or segfault is generated, but in that
        // case the entire application will close, so that's not a problem
        // We need to have that connection not returned => process closes
        std::unique_lock<std::mutex> lock(mut);

        // Block until a connection becomes available
        cv.wait(lock, [this]() {
            return !connections.empty();
        });

        auto db = std::move(connections.front());
        connections.pop();
        return db;
    }

    // Return ownership
    void unborrow(Database db) {
        std::lock_guard<std::mutex> lock{mut};
        connections.push(std::move(db));
        cv.notify_one();
    }

  public:
    // Create a pool
    // TODO should we also have a default value?
    template <typename Func>
    explicit ConnectionPool(const Func& func, const size_t pool_size) : pool_size{pool_size} {

        for (size_t i = 0; i < this->pool_size; ++i) {
            connections.push(func());
        }
    }

    size_t curr_size() const {
        return connections.size();
    }

    size_t start_size() const {
        return this->pool_size;
    }
};

class PooledConnection final {
  private:
    ConnectionPool& pool;

  public:
    Database db;

    explicit PooledConnection(ConnectionPool& pool_)
        : pool{pool_}, db{pool_.borrow()} {}

    ~PooledConnection() {
        pool.unborrow(std::move(db));
        //std::cout << "Connection returned" << std::endl;
    }
};

// we can return by value, which in this case will be moving

// We know how many inputs and how many outputs a statement has
// we don't know what the number of rows are, but we assume 0 or 1
}
