#include "math/xyz.hpp"
#include "code_type.hpp"
#include "code_sequence.hpp"
#include "invalid_code_sequence.hpp"
#include "math/symbols.hpp"
#include "math/xyz.hpp"


#include <vector>
#include <string>
#include <stdexcept>
#include <map>
#include <memory>
#include <algorithm>



#include <boost/variant.hpp>
#include <string>
#include <iostream>

// using Either = boost::variant<std::string, int>; // Left = string, Right = int






class ClassifiedCodeSequence {
public:
    std::shared_ptr<CodeSequence> codeSequence;
    long codeLength;
    long codeSum;
    CodeType codeType;
    bool stable;
    std::string oddEvenPattern;

    explicit ClassifiedCodeSequence(CodeSequence codeSequence) {
        this->codeSequence = std::make_shared<CodeSequence>(codeSequence);
        this->codeLength = codeSequence.code_numbers.size();
        this->codeSum = calculateCodeSum(codeSequence.code_numbers);
        this->codeType = calculateCodeType(codeSequence.code_numbers);
        this->stable = isStableCodeType(codeType);
        this->oddEvenPattern = calculateOddEvenPattern(codeSequence.code_numbers);
    }

    static boost::variant<InvalidCodeSequence,ClassifiedCodeSequence> create(std::vector<int>& dirtyCodeNumbers);



    static std::string calculateOddEvenPattern(const std::vector<int>& codeNumbers);

    static CodeType calculateCodeType(std::vector<int>& codeNumbers);

    static long calculateCodeSum(const std::vector<int>& codeNumbers);

    static bool isStableCodeType(CodeType codeType);

    static bool isOdd(const std::vector<int>& codeNumbers);

    static bool isClosed(std::vector<int>& codeNumbers);

    static bool isStable(const std::vector<int>& codeNumbers);
    int length() const ;

    std::string toString() const;

    bool operator==(ClassifiedCodeSequence& other) const ;

    bool equals(ClassifiedCodeSequence other) ;

    bool operator<(const ClassifiedCodeSequence& other) const ;

    int compareTo(const ClassifiedCodeSequence other) ;

    // std::size_t hash() const ;
};

