#include "code_sequence.hpp"
#include "conversion.hpp"

static bool is_repeated(const std::vector<CodeNumber>& code_numbers, const size_t sub_length) {

    for (size_t i = sub_length; i < code_numbers.size(); ++i) {
        const auto sub_index = i % sub_length;
        if (code_numbers.at(i) != code_numbers.at(sub_index)) {
            return false;
        }
    }

    return true;
}

static XYZ next_angle(const XYZ prev, const XYZ curr, const CodeNumber number) {

    if (number % 2 == 0) {
        // even number
        return prev;
    } else {
        // odd number
        return other_angle(prev, curr);
    }
}

static bool is_legal(const std::vector<CodeNumber>& code_numbers, const size_t end) {

    // A code sequence is legal iff the code angles wrap around correctly at the beginning
    // and end.

    // Strictly speaking, code sequences of length one are allowed if they
    // consist of a single even number (odd ones are illegal).
    // However, all these sequences are trivially empty, so there is no point
    // in considering them.

    // We will have to update these as we go along
    auto prev = XYZ::X; // On the last number
    auto curr = XYZ::Y; // On the first number

    for (size_t i = 0; i < end; ++i) {

        const auto num = code_numbers.at(i);

        // The angle of the next number depends on the current number
        const auto next = next_angle(prev, curr, num);

        // Update prev and curr for the next loop iteration
        // Be very careful about the order you do this
        prev = curr;
        curr = next;
    }

    return prev == XYZ::X && curr == XYZ::Y;
}

// code_numbers must already be valid
// Return the end index of the smallest repeated legal subvector
static size_t smallest_index(const std::vector<CodeNumber>& code_numbers) {

    auto size = code_numbers.size();

    // Find the shortest repeated legal sublist,
    // where we look at the sublists code_numbers[0:i]
    for (size_t i = 2; i < size; ++i) {
        // if i divides the length of the code numbers
        if (size % i == 0) {
            if (is_repeated(code_numbers, i) && is_legal(code_numbers, i)) {
                return i;
            }
        }
    }

    // There are no repeaters, so just return the entire vector
    return size;
}

// If we're going to mutate a parameter, it's better to just
// do so in a meaningful way instead of returning the actual result
// WARNING: this method involves quite a bit of mutability. Be careful.
// It turns out this is a well known problem in computer science:
// https://en.wikipedia.org/wiki/Lexicographically_minimal_string_rotation
// If this ever becomes a performance bottleneck (say if we want to generate
// lists of all valid code sequences), then this might be useful
// We could also just concatenate the vector with itself
// See also Lyndon words
static void minimal_rotation(std::vector<CodeNumber>& code_numbers) {

    // This must be a copy, not a reference, since we mutate code_numbers
    auto min = code_numbers;

    const auto size = code_numbers.size();

    for (size_t i = 0; i < size; ++i) {
        // rotate left
        std::rotate(std::begin(code_numbers), std::next(std::begin(code_numbers)), std::end(code_numbers));

        if (code_numbers < min) {
            min = code_numbers;
        }
    }

    // After size rotations, the vector is now back to where it was before.
    // Now we reverse it, and do it again

    std::reverse(std::begin(code_numbers), std::end(code_numbers));

    for (size_t i = 0; i < size; ++i) {
        // rotate left
        std::rotate(std::begin(code_numbers), std::next(std::begin(code_numbers)), std::end(code_numbers));

        if (code_numbers < min) {
            min = code_numbers;
        }
    }

    code_numbers = min;
}

static void validate(const std::vector<CodeNumber>& code_numbers) {

    if (code_numbers.empty()) {
        throw std::runtime_error("CodeSequence: empty code numbers");
    }

        if (code_numbers.size()==0) {
        throw std::runtime_error("CodeSequence: empty code numbers");
    }

    const auto pos = [](const auto num) { return num > 0; };

    const auto all_pos = std::all_of(std::cbegin(code_numbers), std::cend(code_numbers), pos);

    if (!all_pos) {
        throw std::runtime_error("CodeSequence: non-positive numbers");
    }

    const auto legal = is_legal(code_numbers, code_numbers.size());

    if (!legal) {
        throw std::runtime_error("CodeSequence: illegal pattern");
    }

}

CodeSequence::CodeSequence(const std::vector<CodeNumber>& code_numbers_) {
    validate(code_numbers_);
    auto index = smallest_index(code_numbers_);

    for (size_t i = 0; i < index; ++i) {
        code_numbers.emplace_back(code_numbers_.at(i));
    }

    minimal_rotation(code_numbers);
}

CodeSequence::const_iterator CodeSequence::begin() const {
    return std::begin(code_numbers);
}

CodeSequence::const_iterator CodeSequence::end() const {
    return std::end(code_numbers);
}

bool is_palindrome(const std::vector<CodeNumber>& code_numbers, size_t i) {

    size_t length = code_numbers.size();
    size_t half_length = length / 2;

    // i is the index of E1
    // we start at E2, because it wraps around correctly when using %
    // j starts at E2 - 1, and goes <- through l1
    // k starts at E2 + 1, and goes -> and wraps around through l2
    size_t j = i + half_length - 1;
    size_t k = (i + half_length + 1) % length;

    // j and k meet at E1
    while (j != k) {
        if (code_numbers.at(j) != code_numbers.at(k)) {
            return false;
        }

        j -= 1;
        k = (k + 1) % length;
    }

    return true;
}

boost::optional<size_t> CodeSequence::closed_index() const {

    // The sum of an odd sequence is odd, and then doubling it makes it even
    // (with a single factor of 2). Perpendicular sequences always have a sum
    // that is a multiple of 4, so odd sequences are never perpendicular
    // (but what if you double it again?)
    if (is_odd()) {
        return boost::none;
    }

    auto length = code_numbers.size();
    auto half_length = length / 2;

    // return the index of E1
    for (size_t i = 0; i < half_length; ++i) {
        auto first = code_numbers.at(i);
        auto second = code_numbers.at(i + half_length);

        auto first_even = (first % 2 == 0);
        auto second_even = (second % 2 == 0);

        if (first_even && second_even) {
            // Now check if the two lists between the first and second integers are
            // reverses of each other
            // so we could have __ E1 ~~~~~ E2 ---, for example, and then
            // l1 = ~~~~~
            // l2 = ---__
            // need l1 == l2.reverse()

            // check if l1 == l2.reverse()
            if (is_palindrome(code_numbers, i)) {
                return i;
            }
        }
    }

    // Not a closed sequence, so none
    return boost::none;
}

LinComArrZ<XYEta> CodeSequence::constraint(XYZ first, XYZ second) const {

    // all odd sequences are stable
    if (is_odd()) {
        // Return zero
        return LinComArrZ<XYEta>{};
    }

    // even means add
    // odd means sub
    LinComArrZ<XYEta> constraint{};
    constraint.add(code_numbers.at(0), xyz_to_xyeta(first));
    constraint.sub(code_numbers.at(1), xyz_to_xyeta(second));

    // We will have to update these as we go along
    auto prev_prev = first;
    auto prev = second;

    for (size_t i = 2; i < code_numbers.size(); ++i) {

        // Which angle this is depends on the previous number
        auto prev_number = code_numbers.at(i - 1);

        auto current = next_angle(prev_prev, prev, prev_number);

        auto current_number = code_numbers.at(i);

        if (i % 2 == 0) {
            // i is even, so add
            constraint.add(current_number, xyz_to_xyeta(current));
        } else {
            // i is odd, so subtract
            constraint.sub(current_number, xyz_to_xyeta(current));
        }

        // Update prev_prev and prev for the next loop iteration
        // Be very careful about the order you do this
        prev_prev = prev;
        prev = current;
    }

    constraint.divide_content();
    constraint.divide_unit();

    return constraint;
}

boost::variant<InvalidCodeSequence, CodeSequence> CodeSequence::create(const std::vector<int32_t>&dirtyCodeNumbers){
    try{
        CodeSequence codeSequence(dirtyCodeNumbers);
        return codeSequence;
    } catch (const std::runtime_error& e){
        std::string msg = e.what();  // convert to std::string for easy search
        if (msg.find("empty") != std::string::npos) {
            return InvalidCodeSequence::EMPTY;
        } else if (msg.find("non-positive")){
            return InvalidCodeSequence::NEGATIVE_OR_ZERO_NUMBERS;
        } else if (msg.find("illegal")){
            return InvalidCodeSequence::ILLEGAL_PATTERN;
        } else {
            std::cout << msg << std::endl;
            // throw std::runtime_error("Unknown InvalidCodeSequence");
            return InvalidCodeSequence::NONE;
        }

        
    }
    
    
    
    // auto invalid =  CodeSequence::validate(dirtyCodeNumbers);

    // if (invalid.has_value()) {
    //     // Error case (left)
    //     return invalid.value(); // or *invalid
    // } else {
    //     // Success (right)
    //     CodeSequence codeSequence(dirtyCodeNumbers);
    //     return codeSequence;
    // }
}

/**
 * Check if a code sequence is valid.
 *
 * A *code sequence* is an `IntList`. It is *valid* if
 *   - it is nonempty
 *   - all its numbers are greater than zero
 *   - it is some combination of the legal patterns
 *
 * Note that there are two possible sources of ambiguity in the ordering of a code sequence.
 * First, every
*/
// boost::optional<InvalidCodeSequence> CodeSequence::validate(const std::vector<int32_t>& dirtyCodeNumbers){
//     // Must be nonempty

//     if (dirtyCodeNumbers.empty()) {
//         return InvalidCodeSequence::EMPTY;
//     }

//     // All numbers must be strictly positive
//     bool allPos = std::all_of(
//         dirtyCodeNumbers.begin(),
//         dirtyCodeNumbers.end(),
//         [](int32_t num) { return num > 0; }
//     );
//     if (!allPos) {
//         return InvalidCodeSequence::NEGATIVE_OR_ZERO_NUMBERS;
//     }

//     // Check if it is a combination of the legal patterns
//     bool legal = isLegal(dirtyCodeNumbers);
//     if (!legal) {
//         return InvalidCodeSequence::ILLEGAL_PATTERN;
//     }


//     // Otherwise, code sequence is valid
//     return boost::none;
// }

bool CodeSequence::isLegal(const std::vector<int32_t>& codeNumbers) {

    XYZ prev = XYZ::X;
    XYZ curr = XYZ::Y;

    int size = codeNumbers.size();

    for (int i = 0; i < size; ++i) {
        int num = codeNumbers[i];

        XYZ next = next_angle(prev, curr, num);

        prev = curr;
        curr = next;
    }

    return prev == XYZ::X && curr == XYZ::Y;
}

/*
Jul 31 2025 Marco Mai
transfer from Java
*/
void CodeSequence::rotateLeft(std::vector<int32_t>& list){

        if (!list.empty()) {
            int32_t first = list[0];
            for (size_t i = 1; i < list.size(); i++) {
                list[i - 1] = list[i];
            }
            list[list.size() - 1] = first;
        }
    // c++ build in rotate method
    // if (!list.empty()){
    //     std::rotate(list.begin(), list.begin() + 1, list.end());
    // }
}

/*
Jul 31 2025 Marco Mai
transfer from Java
*/
std::vector<int32_t> CodeSequence::subList(std::vector<int32_t>& list, int32_t start, int32_t end) {
    std::vector<int32_t> subList;
    subList.reserve(end - start);  // Optional: reserve capacity

    for (int32_t i = start; i < end; ++i) {
        subList.emplace_back(list[i]);
    }

    return subList;
}

/*
Jul 31 2025 Marco Mai
transfer from Java
*/
std::vector<XYZ> CodeSequence::angles(XYZ first, XYZ second) const {

    // We start off with these two
    std::vector<XYZ> code_angles{first, second};

    for (size_t i = 2; i < code_numbers.size(); ++i) {

        auto prev_number = code_numbers.at(i - 1);

        if (prev_number % 2 == 0) {
            // even number
            // second last one
            auto current = code_angles.at(i - 2);
            code_angles.push_back(current);
        } else {
            // odd number
            auto prev_prev = code_angles.at(i - 2);
            auto prev = code_angles.at(i - 1);
            auto current = other_angle(prev_prev, prev);
            code_angles.push_back(current);
        }
    }

    return code_angles;
}

size_t CodeSequence::length() const {
    return code_numbers.size();
}

CodeNumber CodeSequence::sum() const {

    CodeNumber s = 0;

    for (const auto num : code_numbers) {
        s += num;
    }

    return s;
}

// The sum and length of the code sequence have the same parity
bool CodeSequence::is_odd() const{
    return code_numbers.size() % 2 != 0;
}

bool CodeSequence::is_closed() const{
    return static_cast<bool>(closed_index());
}

bool CodeSequence::is_stable() const{
    // The angles we choose here are arbitrary
    return constraint(XYZ::X, XYZ::Y).is_zero();
}

CodeType CodeSequence::type() const {

    auto odd = is_odd();
    auto closed = is_closed();
    auto stable = is_stable();

    if (!closed && stable && odd) {
        return CodeType::OSO;
    } else if (!closed && stable && !odd) {
        return CodeType::OSNO;
    } else if (!closed && !stable) {
        return CodeType::ONS;
    } else if (closed && stable) {
        return CodeType::CS;
    } else if (closed && !stable) {
        return CodeType::CNS;
    } else {
        std::ostringstream err{};
        err << code_numbers << " cannot be given a code type";
        throw std::runtime_error(err.str());
    }
}

CodeNumber CodeSequence::number(size_t i) const {
    return code_numbers.at(i);
}

const std::vector<CodeNumber>& CodeSequence::numbers() const {
    return code_numbers; // Assuming your member is called codeNumbers
}

std::string CodeSequence::toString() const {
    std::ostringstream oss;
    for (size_t i = 0; i < code_numbers.size(); ++i) {
        if (i > 0) oss << " ";
        oss << code_numbers[i];
    }
    return oss.str();
}

bool CodeSequence::equals(CodeSequence& other) const {
    return code_numbers == other.code_numbers;
}

int compare(const CodeSequence& lhs, const CodeSequence& rhs) {

    auto lhs_size = lhs.length();
    auto rhs_size = rhs.length();

    auto size_compare = compare(lhs_size, rhs_size);

    if (size_compare != 0) {
        return size_compare;
    } else {
        // Same size, so compare elements one by one
        for (size_t i = 0; i < lhs_size; ++i) {
            auto lhs_elem = lhs.number(i);
            auto rhs_elem = rhs.number(i);

            auto elem_compare = compare(lhs_elem, rhs_elem);

            if (elem_compare != 0) {
                return elem_compare;
            }

            // Else they are equal, so continue to the next element
        }

        // All elements are identical, so they are equal
        return 0;
    }
}

bool  operator==(const CodeSequence& lhs, const CodeSequence& rhs) {
    return compare(lhs, rhs) == 0;
}

bool operator<(const CodeSequence& lhs, const CodeSequence& rhs) {
    return compare(lhs, rhs) < 0;
}

std::ostream& operator<<(std::ostream& os, const CodeSequence& code_seq) {

    bool first = true;

    for (auto num : code_seq) {

        if (!first) {
            os << ' ';
        }

        os << num;

        first = false;
    }

    return os;
}
