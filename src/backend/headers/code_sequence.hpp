#pragma once

#include "code_type.hpp"
#include "general.hpp"

#include <vector>
#include <algorithm>
#include "invalid_code_sequence.hpp"

// A fully canonicalized code sequence. No classification done here
// This is essentially a newtype around a std::vector<CodeNumber>
// We do this because we want the newtype to distinguish
// - this code sequence has been validated and is in standard form
// - to prevent mutability. This class must remain constant to prevent modification
class CodeSequence final {
  private:

  public:
    std::vector<CodeNumber> code_numbers;
    using value_type = typename decltype(code_numbers)::value_type;
    using const_iterator = typename decltype(code_numbers)::const_iterator;

    // Don't call any non-static functions within the constructor,
    // because they might access fields that haven't been initialzed yet.
    explicit CodeSequence(const std::vector<CodeNumber>& code_numbers_);

    const_iterator begin() const;

    const_iterator end() const;

    boost::optional<size_t> closed_index() const;

    LinComArrZ<XYEta> constraint(XYZ first, XYZ second) const;

    // The angles you choose for first and second determine which sextant the region ends up in.
    // If a region is symmetrical across a sextant line, say x = y, then that means the equations are
    // unchanged when you swap x <-> y
    // The standard sextant is the one where x <= y <= z.
    std::vector<XYZ> angles(XYZ first, XYZ second) const;

    size_t length() const;

    CodeNumber sum() const;

    bool is_odd() const;

    bool is_closed() const;

    bool is_stable() const;

    CodeType type() const;

    CodeNumber number(size_t i) const;

    //next3 function translated from CodeSequence.java
    static boost::variant<InvalidCodeSequence, CodeSequence> create(const std::vector<int32_t>&dirtyCodeNumbers);

    // static boost::optional<InvalidCodeSequence> validate(const std::vector<int32_t>& dirtyCodeNumbers);

    static bool isLegal(const std::vector<int32_t>& codeNumbers);

    static void rotateLeft(std::vector<int32_t>& list);

    static std::vector<CodeNumber> subList(std::vector<int32_t>& list, int32_t start, int32_t end);

    // For compatability right now, mostly
    // There are some rules we should observe when it comes to references
    // and safety. We don't want whatever the reference points to to go
    // out of scope or moved around, because then the reference is messed up
    const std::vector<CodeNumber>& numbers() const ;

    std::string toString() const;

    bool equals(CodeSequence& other) const ;

    
};

bool is_palindrome(const std::vector<CodeNumber>& code_numbers, size_t i);

// Impose a total order on the code sequences
int compare(const CodeSequence& lhs, const CodeSequence& rhs);

bool operator==(const CodeSequence& lhs, const CodeSequence& rhs);

bool operator<(const CodeSequence& lhs, const CodeSequence& rhs);

std::ostream& operator<<(std::ostream& os, const CodeSequence& code_seq);
