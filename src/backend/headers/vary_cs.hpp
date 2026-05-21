#pragma once

#ifndef VARY_CS_HPP
#define VARY_CS_HPP

#include <unistd.h>   // for sysconf
#include <cstdint>
#include <cstdlib>
#include <iostream>
#include <array>
#include <string>
#include <sstream>
#include <list>
#include <vector>
#include <cmath> 
#include <stdexcept>
#include <algorithm>
#include <condition_variable>
#include <atomic>
#include <thread>
// #include "cancel_flag.hpp"




#include <boost/optional.hpp>
#include <boost/multiprecision/mpfr.hpp>
#include <boost/math/constants/constants.hpp>
#include <boost/thread/mutex.hpp>
#include <mutex>
#include <pthread.h>


#include <boost/asio.hpp>
#include <math/side_sum.hpp>
#include <numbers.hpp>
#include "code_sequence.hpp"
#include "triangle_billiard.hpp"
#include "utils.hpp"



#if defined(__APPLE__)
#include <sys/sysctl.h>
#endif

#if defined(__linux__)
#include <sys/sysinfo.h> 
#endif

#if defined(_WIN64)
#include <windows.h>
#endif

/*
Jul 31 2025 Marco Mai
detect the number of memory in the system
setting number of task allow to submit to the memory
*/
size_t get_total_physical_memory();
size_t get_stack_limit();
int compute_max_inflight(float usage_fraction, size_t per_task_bytes);

/*
Jul 31 2025 Marco Mai
transfer from Java
*/
void iterateFireAwayCS2(
    int32_t min, int32_t max, float64_t specMin, float64_t specMax,
    SideSum& initialSideSum, TriangleBilliard initialBilliard,
    std::vector<int32_t>& initialCode,
    std::vector<std::vector<int32_t>>& codesFound);


            // Entry point (Java fireAway equivalent)
std::vector<std::vector<int32_t>> fireAwayCS(const int32_t movesMin, const int32_t movesMax, const float64_t xAngle, const float64_t yAngle,const std::string reqType);

#endif
