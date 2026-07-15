#pragma once

#include <array>
#include <string>
#include <sstream>
#include <list>
#include <vector>
#include <cmath> 
#include <cstdint>
#include <algorithm>
#include <cstdlib>
#include <boost/cstdfloat.hpp>
#include <stdexcept>
#include <atomic>
#include <thread>
#include <limits>
#include <memory>
#include <mutex>

#include <tbb/global_control.h>

#include <unordered_map>

#include <boost/optional.hpp>
#include "classified_code_sequence.hpp"
#include "code_sequence.hpp"

inline std::atomic<bool>& cancel_flag() {
    static std::atomic<bool> f{false};
    return f;
}

inline std::atomic<unsigned int>& configured_worker_count() {
    static std::atomic<unsigned int> worker_count{0};
    return worker_count;
}

inline std::mutex& tbb_worker_control_mutex() {
    static std::mutex mutex;
    return mutex;
}

inline std::unique_ptr<tbb::global_control>& tbb_worker_control() {
    static std::unique_ptr<tbb::global_control> control;
    return control;
}

inline void billiards_set_worker_count(const unsigned int worker_count) {
    if (worker_count == 0) {
        throw std::invalid_argument("worker count must be positive");
    }

    // Boost pools read configured_worker_count for each operation. TBB uses a
    // process-wide scheduler, so keep a global_control alive for the process to
    // make --threads cap parallel_for work such as MRR corner classification.
    std::lock_guard<std::mutex> lock{tbb_worker_control_mutex()};
    tbb_worker_control().reset(new tbb::global_control{
        tbb::global_control::max_allowed_parallelism,
        static_cast<std::size_t>(worker_count)
    });
    configured_worker_count().store(worker_count, std::memory_order_relaxed);
}

inline unsigned int billiards_worker_count(
        unsigned int max_workers = std::numeric_limits<unsigned int>::max()) {
    const unsigned int reported = std::thread::hardware_concurrency();
    const unsigned int hardware_workers = reported == 0 ? 1 : reported;
    unsigned int workers = std::max(1u, hardware_workers / 2);

    // Java parses the release-facing --threads argument before the backend is
    // used and calls backend_set_worker_threads(). That gives Java executors
    // and native Boost/TBB pools one shared worker limit instead of requiring
    // users to set both a JVM property and a native environment variable.
    const unsigned int configured_workers = configured_worker_count().load(std::memory_order_relaxed);
    if (configured_workers > 0) {
        workers = configured_workers;
    } else if (const char* raw = std::getenv("BILLIARDS_NATIVE_THREADS")) {
        // Keep the old native-only environment override for scripts and
        // profiling sessions that start backend tests without Java calling
        // backend_set_worker_threads().
        char* end = nullptr;
        const long requested = std::strtol(raw, &end, 10);
        if (end != raw && requested > 0) {
            workers = static_cast<unsigned int>(requested);
        }
    }

    return std::max(1u, std::min(workers, std::max(1u, max_workers)));
}

inline std::size_t billiards_task_count(const std::size_t item_count, const unsigned int workers) {
    if (item_count == 0) {
        return 0;
    }

    return std::min<std::size_t>(item_count, std::max(1u, workers));
}

inline std::size_t billiards_block_size(const std::size_t item_count, const std::size_t task_count) {
    return task_count == 0 ? 0 : (item_count + task_count - 1) / task_count;
}

static std::unordered_map<std::string, CodeType> stringToCodeType = {
    {"oso", CodeType::OSO},
    {"osno", CodeType::OSNO},
    {"ons", CodeType::ONS},
    {"cs", CodeType::CS},
    {"cns", CodeType::CNS}
};

// std::unordered_set<CodeType> parse_code_type_set(const std::string& input);
std::string to_lower(const std::string& str);

std::vector<CodeType> parse_code_types(const std::string& input,
                                       const std::unordered_map<std::string, CodeType>& lookup) ;

bool is_code_type_in_list(CodeType code, const std::vector<CodeType>& allowed);

boost::optional<ClassifiedCodeSequence> convert(const std::vector<int>& codeList);

boost::optional<CodeType> getCodeType(std::vector<int32_t>& codeList);

int32_t modN(int32_t x, int32_t n);
