#include <classified_code_sequence.hpp>

/*
Jul 31 2025 Marco Mai
transfer from Java
*/
boost::variant<InvalidCodeSequence,ClassifiedCodeSequence> ClassifiedCodeSequence::create(std::vector<int>& dirtyCodeNumbers) {
    boost::variant<InvalidCodeSequence,CodeSequence> either = CodeSequence::create(dirtyCodeNumbers);



    if (boost::get<CodeSequence>(&either)) {
        const CodeSequence& codeSequence = *boost::get<CodeSequence>(&either);
        ClassifiedCodeSequence classCodeSeq(codeSequence);
        return classCodeSeq;
    } else {
        const InvalidCodeSequence& invalid = *boost::get<InvalidCodeSequence>(&either);
        return invalid;  // assuming return type can handle both
    }
}



/**
 * Zhao Yu Li, May 06, 2025.
 * Calculates the odd-even pattern of a code sequence. The odd-even pattern is used to distinguished two code
 * sequence when they are of the same type and have the same code length. The odd-even pattern is stored as an
 * array of string, where each character in the string can be either an "O(dd)" or an "E(ven)". It is important to
 * note that using a String or StringBuilder to store the entire pattern is memory intensive, as each element can
 * take only two values, and each element is eight bytes. A more efficient way is to use a single bit, but the code
 * sequence can have up to a thousand numbers, and we don't have a integer type that big.
 * e.g. 1 1 368 1 1 739 <==> "OOEOOO"
 * @param codeNumbers The list of numbers that constitutes the code sequence
 * @return The odd-even pattern
 */
std::string ClassifiedCodeSequence::calculateOddEvenPattern(const std::vector<int>& codeNumbers) {
    std::string pattern;
    for (int n : codeNumbers) {
        pattern += (n % 2 == 0) ? 'E' : 'O';
    }
    return pattern;
}

CodeType ClassifiedCodeSequence::calculateCodeType(std::vector<int>& codeNumbers) {
    bool odd = isOdd(codeNumbers);
    bool closed = isClosed(codeNumbers);
    bool stable = isStable(codeNumbers);

    if (!closed && stable && odd) return CodeType::OSO;
    if (!closed && stable && !odd) return CodeType::OSNO;
    if (!closed && !stable) return CodeType::ONS;
    if (closed && stable) return CodeType::CS;
    if (closed && !stable) return CodeType::CNS;
    throw std::runtime_error("Cannot be classified");
}

long ClassifiedCodeSequence::calculateCodeSum(const std::vector<int>& codeNumbers) {
    long sum = 0;
    for (int n : codeNumbers) sum += n;
    return sum;
}

bool ClassifiedCodeSequence::isStableCodeType(CodeType codeType) {
    switch (codeType) {
        case CodeType::OSO:
        case CodeType::OSNO:
        case CodeType::CS:
            return true;
        case CodeType::ONS:
        case CodeType::CNS:
            return false;
        default:
            throw std::runtime_error("Unknown code type");
    }
}

/**
 * A code sequence is *odd* if the sum of its numbers is odd.
 *
 * Note that the sum of a code sequence is odd iff its length is odd. This is
 * because this property holds for all the legal patterns, and all legal code
 * sequences must be some combination of those patterns.
 */
bool ClassifiedCodeSequence::isOdd(const std::vector<int>& codeNumbers) {
    return codeNumbers.size() % 2 != 0;
}

/**
 * A code sequence is *closed* if it looks something like this:
 *
 * __ E ~~~~~ E ---
 *
 * where the E's are arbitrary even numbers, a = ~~~~~, b = ---__, and a == b.reverse().
 *
 * For example, this is a sum 28 closed sequence:
 *
 * 1 2 1 3 3 1 3 4 3 1 3 3
 * _ E ~ ~ ~ ~ ~ E - - - -
 *
 * a = 1 3 3 1 3
 * b = 3 1 3 3 1
 *
 * A code sequence that is not closed is called *open*.
 *
 * Note that all closed have even length (and so also even sum).
 *
 * This function determines if the given code sequence is closed.
 */
bool ClassifiedCodeSequence::isClosed(std::vector<int>& codeNumbers) {
    // Odd code sequences are never closed
    if (isOdd(codeNumbers)) return false;

    int length = codeNumbers.size();
    int halfLength = length / 2;

    // Iterate over codeNumbers in intervals of halfLength, checking
    // to see if we find two even numbers.
    for (int i = 0; i < halfLength; ++i) {
        int first = codeNumbers[i];
        int second = codeNumbers[i + halfLength];

        bool firstEven = first % 2 == 0;
        bool secondEven = second % 2 == 0;

        if (firstEven && secondEven) {
            // Now check if the two lists between the first and second integers are
            // reverses of each other.
            // We could have __ E ~~~~~ E ---, for example, and then
            // a = ~~~~~
            // b = ---__ (and then we reverse it)
            // final IntList a = codeNumbers.subList(i + 1, i + halfLength);
            std::vector<int> a = CodeSequence::subList(codeNumbers, i + 1, i + halfLength);
            // std::vector<int> a(codeNumbers.begin() + i + 1, codeNumbers.begin() + i + halfLength);
            std::vector<int> b;
            // b.insert(b.end(), codeNumbers.begin() + i + halfLength + 1, codeNumbers.end());
            // b.insert(b.end(), codeNumbers.begin(), codeNumbers.begin() + i);
            auto tmp1 = CodeSequence::subList(codeNumbers, i + halfLength + 1, length);
            b.insert(b.end(), tmp1.begin(), tmp1.end());

            auto tmp2 = CodeSequence::subList(codeNumbers, 0, i);
            b.insert(b.end(), tmp2.begin(), tmp2.end());

            std::reverse(b.begin(), b.end());
            if (a == b) return true;
        }
    }
    return false;
}

/**
 * Determine if the code sequence is stable.
 */
// TODO replace this method with the constraint method
bool ClassifiedCodeSequence::isStable(const std::vector<int>& codeNumbers) {
    // odd sequences are stable
    if (codeNumbers.size() % 2 != 0) return true;

    std::vector<XYZ> codeAngles;
    // we always start off with the first two
    codeAngles.push_back(XYZ::X);
    codeAngles.push_back(XYZ::Y);

    // the code sequence is already constructed, so we know it is valid
    for (size_t i = 1; i < codeNumbers.size() - 1; ++i) {
        int number = codeNumbers[i];
        if (number % 2 == 0) {
            // even number
            // second last one
            XYZ prev = codeAngles[codeAngles.size() - 2];
            codeAngles.push_back(prev);
        } else {
            // odd number
            XYZ one = codeAngles[codeAngles.size() - 1];
            XYZ two = codeAngles[codeAngles.size() - 2];
            XYZ other = otherAngle(one, two); // the function in under xyz.cpp
            codeAngles.push_back(other);
        }
    }

    // use long just to be safe
    // init everything to zero
    std::map<XYZ, long> coeffs =  {{XYZ::X, 0L},{XYZ::Y, 0L},{XYZ::Z, 0L}};

    for (size_t i = 0; i < codeNumbers.size(); ++i) {
        int codeNumber = codeNumbers[i];
        XYZ codeAngle = codeAngles[i];

        long& oldCoeff = coeffs[codeAngle];

        long newCoeff;
        // we need to take the alternating sum
        if (i % 2 == 0) {
            newCoeff = oldCoeff + codeNumber;
        } else {
            newCoeff = oldCoeff - codeNumber;
        }

        coeffs[codeAngle] = newCoeff;
    } 

    return (coeffs[XYZ::X] == 0) && (coeffs[XYZ::Y] == 0) && (coeffs[XYZ::Z] == 0);
}

int ClassifiedCodeSequence::length() const { return codeSequence->code_numbers.size(); }

std::string ClassifiedCodeSequence::toString() const {return codeSequence->toString(); }

bool ClassifiedCodeSequence::operator==(ClassifiedCodeSequence& other) const {
    return *codeSequence == *(other.codeSequence);
}

bool ClassifiedCodeSequence::equals(ClassifiedCodeSequence other) {
    // Strictly speaking, we should return false if obj is not an instance of CodeSequence, but
    // I don't care. Comparing equality with an object of a different type is always a bug and
    // has bitten me before.
    return this->codeSequence->equals(*other.codeSequence);
}

bool ClassifiedCodeSequence::operator<(const ClassifiedCodeSequence& other) const {
    return *codeSequence < *(other.codeSequence);
}

int ClassifiedCodeSequence::compareTo(const ClassifiedCodeSequence other) {
    return compare(*(this->codeSequence),*other.codeSequence);
}

