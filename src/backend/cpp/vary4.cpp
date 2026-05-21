#include "vary4.hpp"

const float64_t OFFSET = 0.05;


/* this is a modifed version of Vary4.java
 * all funcion and class from java is implemented in c++
   but some of them no longer reference due to performence improvement reasons.
   Such as CodeSequence, ClassifiedCodeSequence, Utils.convert
   
    1. the process of creating ClassidedCodeSequence has been remove from here,
      this step will be done when wrapper recive information in Java.
	2. There will be seprate mulithread processing the gerenate codeSequence, and verify its code type
	3. There is a detacte the max code size,and computer memory size,limit number of submition,
	 code type check. avoid large amount of memory swap, 
   */


void iterateFireAway4(
    int32_t min, int32_t max, float64_t specMin, float64_t specMax, 
    SideSum& sideSum, TriangleBilliard4 billiard,
    std::vector<int32_t>& code,
    std::vector<std::vector<int32_t>>& codesFound, std::string reqType)
{
    cancel_flag().store(false,  std::memory_order_relaxed); 
    // store data in each level
    struct Frame {
        int32_t swapValue;
        TriangleBilliard4 cbilliard;
        bool leftTried = false;
        bool rightTried = false;
        bool goLeft = false;
    };

    std::vector<Frame> stack;
    int32_t depth = 0;

    stack.push_back(Frame{0,billiard,false,false,false});

    std::vector<CodeType> allowed = parse_code_types(reqType,stringToCodeType);




	try{
		

			while (!stack.empty()) {
                if (cancel_flag().load(std::memory_order_relaxed)) {
                    std::cout << "C++ Vary4 Cancel" << std::endl;
                    return ;}
                
                Frame& frame = stack.back();


				if (depth >= max) {
					code.pop_back();
					depth--;
					frame.goLeft? sideSum.sub(frame.swapValue) : sideSum.add(frame.swapValue);
					stack.pop_back();
					continue;
				}

				// float64_t specialAngle = frame.cbilliard.getSpecialAngle();

				if (!frame.leftTried && !frame.rightTried ) {

					if (depth > min) {
						if (std::abs(sideSum.sum()) < OFFSET && frame.cbilliard.side == 2 &&frame.cbilliard.orient == 1) {
							 
                            float64_t perfectAngle = std::atan2(frame.cbilliard.vertexA.y,frame.cbilliard.vertexA.x);

                            if (billiard.between(perfectAngle)) {

                                auto seq = convert(code);
                                if (seq){
                                    CodeType codeType = seq.get().codeType;
                                    if (is_code_type_in_list(codeType,allowed)) {
                                        codesFound.push_back(code);
                                    }
                                }
                            }
										
							
						}
                    }
					

					frame.leftTried = true;
                    boost::optional<TriangleBilliard4> temp = frame.cbilliard.getNext(true);
					if (temp){
                        TriangleBilliard4 newbilliard = temp.get();
						int32_t rightSwap = 3 - frame.cbilliard.side - newbilliard.side;
											
						sideSum.add(rightSwap);
						code.emplace_back(rightSwap);
						stack.push_back(Frame{
							rightSwap, newbilliard,
							false, false, true
						});
						depth++;
						continue;
					}
				}

				if (!frame.rightTried ) {
					frame.rightTried = true;

                    boost::optional<TriangleBilliard4> temp = frame.cbilliard.getNext(false);
					if (temp){
                        TriangleBilliard4 newbilliard = temp.get();
						int32_t leftSwap = 3 - frame.cbilliard.side - newbilliard.side;;

						sideSum.sub(leftSwap);
						code.emplace_back(leftSwap);
						stack.push_back(Frame{
							leftSwap, newbilliard,
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


	}catch (const std::exception& ex){
		std::cerr << "Exception caught: " << ex.what() << '\n';
	}
}


// this loop keep as recursion as pervious version to simplicty
// since it the limit 8000 depth to out of memory in Java
// in current comsumer computer 8000 cores not yet exist

std::vector<TriangleStart> makeStarts(
    TriangleBilliard4& billiard,
    int32_t depth,
    int32_t maxDepth,
    std::vector<int32_t>& code,
    SideSum& sideSum)
{
    if (depth >= maxDepth) {
        std::vector<int32_t> codeCopy = code;
        std::vector<std::tuple<TriangleBilliard4, std::vector<int32_t>, SideSum>> rec;
        rec.emplace_back(billiard, codeCopy, sideSum.copy());
        return rec;
    }

    std::vector<std::tuple<TriangleBilliard4, std::vector<int32_t>, SideSum>> starts;

    auto optLeft = billiard.getNext(true);
    if (optLeft) {
        TriangleBilliard4& leftBilliard = *optLeft;
        int32_t leftSwap = 3 - billiard.side - leftBilliard.side;

        sideSum.add(leftSwap);
        code.push_back(leftSwap);

        auto leftStarts = makeStarts(leftBilliard, depth + 1, maxDepth, code, sideSum);
        starts.insert(starts.end(), leftStarts.begin(), leftStarts.end());

        code.pop_back();
        sideSum.sub(leftSwap);
    }

    auto optRight = billiard.getNext(false);
    if (optRight) {
        TriangleBilliard4& rightBilliard = *optRight;
        int32_t rightSwap = 3 - billiard.side - rightBilliard.side;

        sideSum.sub(rightSwap);
        code.push_back(rightSwap);

        auto rightStarts = makeStarts(rightBilliard, depth + 1, maxDepth, code, sideSum);
        starts.insert(starts.end(), rightStarts.begin(), rightStarts.end());

        code.pop_back();
        sideSum.add(rightSwap);
    }

    return starts;
}

std::vector<TriangleStart> lazySort(std::vector<TriangleStart> array) {
    std::sort(array.begin(), array.end(), [](const TriangleStart& a, const TriangleStart& b) {
        return std::get<0>(a).interval() < std::get<0>(b).interval();
    });
    return array;
}


std::vector<std::vector<int32_t>> fireAway4(const int32_t movesMin, const int32_t movesMax,
		const float64_t xAngle, const float64_t yAngle,const std::string reqType) {

    unsigned int cores = std::thread::hardware_concurrency();

		
	TriangleBilliard4 startBilliard = TriangleBilliard4::create(xAngle, yAngle);
    SideSum sideSum = SideSum::create(xAngle, yAngle);
    std::vector<int32_t> startCode ;

    std::vector<TriangleStart> starts = makeStarts(startBilliard,movesMax, cores, startCode, sideSum);
    
    auto sortStarts = lazySort(std::move(starts));

     std::vector<std::vector<int32_t>> allCodes;
    std::mutex codesMutex;

    boost::asio::thread_pool pool(cores);
    std::atomic<int> inflight {0};

    // Optional: limit inflight to reduce memory pressure
    const int MAX_INFLIGHT = cores;

    for (const auto& T : sortStarts) {
        // Wait if too many inflight
        while (inflight >= MAX_INFLIGHT) {
            std::this_thread::sleep_for(std::chrono::microseconds(100));
        }

        inflight.fetch_add(1, std::memory_order_relaxed);

        boost::asio::post(pool, [=, &allCodes, &codesMutex, &inflight]() {
            try {
                std::vector<std::vector<int32_t>> codesf;
                SideSum localSum = std::get<2>(T);  // copy the SideSum
                std::vector<int32_t> localcode =std::get<1>(T);
                iterateFireAway4(movesMin,movesMax,0,cores,                
                localSum, // SideSum
                std::get<0>(T), // TriangleBilliard4
                localcode, // Code)
                codesf,reqType);


                {
                    std::lock_guard<std::mutex> lock(codesMutex);
                    allCodes.insert(allCodes.end(), codesf.begin(), codesf.end());
                }
            } catch (const std::exception& e) {
                std::cerr << "Task failed: " << e.what() << std::endl;
            }

            inflight.fetch_sub(1, std::memory_order_relaxed);
        });
    }

    pool.join();
    return allCodes;


}






