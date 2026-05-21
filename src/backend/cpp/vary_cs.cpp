#include "vary_cs.hpp"

size_t get_total_physical_memory() {
#if defined(__APPLE__) || defined(__MACH__)
    int64_t mem;
    size_t len = sizeof(mem);
    if (sysctlbyname("hw.memsize", &mem, &len, nullptr, 0) == 0) {
        return static_cast<size_t>(mem);
    } else {
        std::cerr << "sysctlbyname failed\n";
        return 0;
    }
#elif defined(__linux__)
    long pages = sysconf(_SC_PHYS_PAGES);
    long page_size = sysconf(_SC_PAGE_SIZE);
    return static_cast<size_t>(pages) * static_cast<size_t>(page_size);
#elif defined(_WIN64)
    MEMORYSTATUSEX status;
    status.dwLength = sizeof(status);

    if (GlobalMemoryStatusEx(&status)) {
        // ullTotalPhys gives the total physical memory in bytes
        return status.ullTotalPhys;
    }

    return 0;
#else
    return 4L * 1024 * 1024 * 1024;  // Fallback to 4GB
#endif
}

int compute_max_inflight(float usage_fraction = 0.75f, size_t per_task_bytes = 128) {
    const size_t total_memory = get_total_physical_memory();
    const size_t usable_memory = static_cast<size_t>(total_memory * usage_fraction);
    return static_cast<int>(usable_memory / per_task_bytes);
}


const float64_t OFFSET = 0.0005;
const float64_t SMALLOFFSET = 0.0000000000005;


/* this is a modifed version of Vary3.java
 * all funcion and class from java is implemented in c++
   but some of them no longer reference due to performence improvement reasons.
   Such as CodeSequence, ClassifiedCodeSequence, Utils.convert
   
    1. the process of creating ClassidedCodeSequence has been remove from here,
      this step will be done when wrapper recive information in Java.
	2. There will be seprate mulithread processing the gerenate codeSequence, and verify its code type
	3. There is a detacte the max code size,and computer memory size,limit number of submition,
	 code type check. avoid large amount of memory swap, 
   */
void iterateFireAwayCS2(
    int32_t min, int32_t max, float64_t specMin, float64_t specMax,
    SideSum& sideSum, TriangleBilliard billiard,
    std::vector<int32_t>& code,
    std::vector<std::vector<int32_t>>& codesFound, std::string reqType)
{
    cancel_flag().store(false,  std::memory_order_relaxed); 
    // store data in each level
    struct Frame {
        float64_t specMin;
        float64_t specMax;
        int32_t swapValue;
        TriangleBilliard cbilliard;
        bool leftTried = false;
        bool rightTried = false;
        bool goLeft = false;
    };

    std::vector<Frame> stack;
    stack.reserve(max*2);
    int32_t depth = 0;
    std::vector<CodeType> allowed = parse_code_types(reqType,stringToCodeType);

    stack.push_back(Frame{specMin, specMax,0,billiard,false,false,false});

    // parallel code verify
    std::atomic<int> inflight{0};
    // setting limit for submition to the memory
    float usage = 0.4;
    if (max >30000){usage = 0.04;}
    else if (max >25000){usage=0.08;}
    else if (max >15000){usage=0.1;}
    else if (max >10000){usage=0.2;}
    else if (max >6000){usage = 0.3;};
    const int MAX_INFLIGHT = compute_max_inflight(usage, 16384);  // tune based on memory
    unsigned int cores = std::thread::hardware_concurrency();
    std::mutex codesFoundMutex;

    try{


            boost::asio::thread_pool pool(cores); 
            while (!stack.empty()) {
                Frame& frame = stack.back();

                if (cancel_flag().load(std::memory_order_relaxed)) {
                    std::cout << "C++ VaryCS Canceling" << std::endl;
                    pool.stop();
                    pool.join();
                    
                    std::cout << "Canceled" << std::endl;
                    return ;}

                if (depth >= max) {
                    code.pop_back();
                    depth--;
                    frame.goLeft? sideSum.sub(frame.swapValue) : sideSum.add(frame.swapValue);
                    stack.pop_back();
                    continue;
                }

                // float64_t specialAngle = frame.cbilliard.vertexC.x;

                if (!frame.leftTried && !frame.rightTried ) {
                    if (depth > min && std::abs(sideSum.sum()) < OFFSET) {
                        if ((frame.specMax - frame.specMin) > SMALLOFFSET) {
                            std::vector<int32_t> code2;
                            code2.insert(code2.end(), code.begin(), code.end());
                            code2.insert(code2.end(), code.rbegin(), code.rend());

                            // detect if hitting limit, if yes wait
                            while (inflight >= MAX_INFLIGHT) {
                                std::this_thread::sleep_for(std::chrono::microseconds(100));  // or use condition_variable
                            }
                            inflight.fetch_add(1, std::memory_order_relaxed);
                            // type check
                            boost::asio::post(pool, [=, &codesFound, &inflight, &codesFoundMutex] {
                                // std::vector<int> intVec(code2.begin(), code2.end());
                                auto seq = convert(code2);
                                if (seq){
                                    CodeType codeType = seq.get().codeType;
                                    if (codeType == CodeType::CS) {
                                        std::lock_guard<std::mutex> lock(codesFoundMutex);
                                        codesFound.push_back(code2);
                                    }
                                }
                                inflight.fetch_sub(1, std::memory_order_relaxed);
                            });
                        }
                    }

                    frame.leftTried = true;

                    if (frame.specMin < frame.cbilliard.vertexC.x){
                        int32_t leftSwap = 3 - frame.cbilliard.side;
                        
                        TriangleBilliard newbilliard = frame.cbilliard.getNext(true);
                        leftSwap = leftSwap - newbilliard.side;
                        
                        sideSum.add(leftSwap);
                        code.emplace_back(leftSwap);
                        stack.push_back(Frame{
                            frame.specMin,
                            std::min(frame.cbilliard.vertexC.x, frame.specMax),
                            leftSwap, newbilliard,
                            false, false, true
                        });
                        depth++;
                        continue;
                    }
                }

                if (!frame.rightTried ) {
                    frame.rightTried = true;

                    if (frame.specMax > frame.cbilliard.vertexC.x){
                        int32_t rightSwap = 3 - frame.cbilliard.side;

                        TriangleBilliard newbilliard = frame.cbilliard.getNext(false);
                        rightSwap = rightSwap - newbilliard.side;

                        sideSum.sub(rightSwap);
                        code.emplace_back(rightSwap);
                        stack.push_back(Frame{
                            std::max(frame.cbilliard.vertexC.x, frame.specMin),
                            frame.specMax,
                            rightSwap, newbilliard,
                            false,false,false
                        });
                        depth++;
                        continue;
                    }
                }


                // Both directions done — backtrack
                if (!code.empty()) code.pop_back();  // safeguard
                depth--;
                frame.goLeft? sideSum.sub(frame.swapValue) : sideSum.add(frame.swapValue);
                stack.pop_back();
                
            }
            // all recursion done, waiting for result of foundcode
            pool.join();

     	}catch (const std::exception& ex){
		std::cerr << "Exception caught: " << ex.what() << '\n';
	}

}





std::vector<std::vector<int32_t>> fireAwayCS(const int32_t movesMin, const int32_t movesMax,
		const float64_t xAngle, const float64_t yAngle,const std::string reqType) {



	std::vector<std::vector<int32_t>> foundCodes;
	TriangleBilliard billiard = TriangleBilliard::create(xAngle, yAngle, 0);
	SideSum sideSum = SideSum::create(xAngle, yAngle);
	std::vector<int32_t> code ;

    iterateFireAwayCS2(movesMin/2, movesMax/2, 0, billiard.vertexB.x, sideSum, billiard, code, foundCodes, reqType);


	return foundCodes;
}






