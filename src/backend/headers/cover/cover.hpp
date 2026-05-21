#pragma once

#include "../code_sequence.hpp"
#include "../general.hpp"

struct CodePair final {

    CodeSequence sequence;
    InitialAngles angles;

    explicit CodePair(const CodeSequence& code_sequence_, const InitialAngles& initial_angles_)
        : sequence{code_sequence_}, angles{initial_angles_} {}

    friend bool operator==(const CodePair& lhs, const CodePair& rhs) {
        return std::tie(lhs.sequence, lhs.angles) == std::tie(rhs.sequence, rhs.angles);
    }

    friend bool operator<(const CodePair& lhs, const CodePair& rhs) {
        return std::tie(lhs.sequence, lhs.angles) < std::tie(rhs.sequence, rhs.angles);
    }

    friend std::ostream& operator<<(std::ostream& os, const CodePair& code_pair) {
        return os << code_pair.sequence << ", " << code_pair.angles;
    }
};

template <typename T>
class UniquePtr final {

  private:
    std::unique_ptr<T> ptr;

  public:
    template <typename... Args>
    explicit UniquePtr(Args&&... args) {
        ptr = std::make_unique<T>(std::forward<Args>(args)...);
    }

    T& get() {
        return *ptr;
    }

    const T& get() const {
        return *ptr;
    }

    friend bool operator==(const UniquePtr<T>& lhs, const UniquePtr<T>& rhs) {
        return lhs.get() == rhs.get();
    }

    friend bool operator<(const UniquePtr<T>& lhs, const UniquePtr<T>& rhs) {
        return lhs.get() < rhs.get();
    }

    friend std::ostream& operator<<(std::ostream& os, const UniquePtr<T>& p) {
        return os << p.get();
    }
};

template <typename T>
class SharedPtr final {

  private:
    std::shared_ptr<T> ptr;

  public:
    template <typename... Args>
    explicit SharedPtr(Args&&... args) {
        ptr = std::make_shared<T>(std::forward<Args>(args)...);
    }

    T& get() {
        return *ptr;
    }

    const T& get() const {
        return *ptr;
    }

    friend bool operator==(const SharedPtr<T>& lhs, const SharedPtr<T>& rhs) {
        return lhs.get() == rhs.get();
    }

    friend bool operator<(const SharedPtr<T>& lhs, const SharedPtr<T>& rhs) {
        return lhs.get() < rhs.get();
    }

    friend std::ostream& operator<<(std::ostream& os, const SharedPtr<T>& p) {
        return os << p.get();
    }
};

struct SinglePair final {

    SharedPtr<CodePair> stable;

    explicit SinglePair(const SharedPtr<CodePair>& stable_)
        : stable{stable_} {}

    friend bool operator==(const SinglePair& lhs, const SinglePair& rhs) {
        return lhs.stable == rhs.stable;
    }

    friend bool operator<(const SinglePair& lhs, const SinglePair& rhs) {
        return lhs.stable < rhs.stable;
    }

    friend std::ostream& operator<<(std::ostream& os, const SinglePair& single_pair) {
        return os << single_pair.stable;
    }
};

struct TriplePair final {

    SharedPtr<CodePair> stable_neg;
    SharedPtr<CodePair> unstable;
    SharedPtr<CodePair> stable_pos;

    explicit TriplePair(const SharedPtr<CodePair>& stable_neg_,
                        const SharedPtr<CodePair>& unstable_,
                        const SharedPtr<CodePair>& stable_pos_)
        : stable_neg{stable_neg_}, unstable{unstable_}, stable_pos{stable_pos_} {}

    friend bool operator==(const TriplePair& lhs, const TriplePair& rhs) {
        return std::tie(lhs.stable_neg, lhs.unstable, lhs.stable_pos) == std::tie(rhs.stable_neg, rhs.unstable, rhs.stable_pos);
    }

    friend bool operator<(const TriplePair& lhs, const TriplePair& rhs) {
        return std::tie(lhs.stable_neg, lhs.unstable, lhs.stable_pos) < std::tie(rhs.stable_neg, rhs.unstable, rhs.stable_pos);
    }

    friend std::ostream& operator<<(std::ostream& os, const TriplePair& triple_pair) {
        return os << triple_pair.stable_neg << "; " << triple_pair.unstable << "; " << triple_pair.stable_pos;
    }
};

struct HalfTriplePair final {

    SharedPtr<CodePair> stable_neg;
    SharedPtr<CodePair> unstable;

    explicit HalfTriplePair(const SharedPtr<CodePair>& stable_neg_,
                        const SharedPtr<CodePair>& unstable_)
        : stable_neg{stable_neg_}, unstable{unstable_} {}

    friend bool operator==(const HalfTriplePair& lhs, const HalfTriplePair& rhs) {
        return std::tie(lhs.stable_neg, lhs.unstable) == std::tie(rhs.stable_neg, rhs.unstable);
    }

    friend bool operator<(const HalfTriplePair& lhs, const HalfTriplePair& rhs) {
        return std::tie(lhs.stable_neg, lhs.unstable) < std::tie(rhs.stable_neg, rhs.unstable);
    }

    friend std::ostream& operator<<(std::ostream& os, const HalfTriplePair& triple_pair) {
        return os << triple_pair.stable_neg << "; " << triple_pair.unstable;
    }
};

template <typename T>
class Quarters final {

  private:
    std::array<T, 4> arr;

  public:
    using value_type = typename decltype(arr)::value_type;
    using iterator = typename decltype(arr)::iterator;
    using const_iterator = typename decltype(arr)::const_iterator;

    explicit Quarters(T ul, T ur, T ll, T lr)
        : arr{{std::move(ul), std::move(ur), std::move(ll), std::move(lr)}} {}

    iterator begin() {
        return std::begin(arr);
    }

    iterator end() {
        return std::end(arr);
    }

    const_iterator begin() const {
        return std::begin(arr);
    }

    const_iterator end() const {
        return std::end(arr);
    }

    template <size_t index>
    T& get() {
        return std::get<index>(arr);
    }

    template <size_t index>
    const T& get() const {
        return std::get<index>(arr);
    }

    T& at(const size_t i) {
        return arr.at(i);
    }

    const T& at(const size_t i) const {
        return arr.at(i);
    }
};

namespace cover {

struct Empty final {};

struct Single final {
    SinglePair single_pair;

    explicit Single(const SinglePair& single_pair_)
        : single_pair{single_pair_} {}
};

struct Triple final {
    TriplePair triple_pair;

    explicit Triple(const TriplePair& triple_pair_)
        : triple_pair{triple_pair_} {}
};

struct HalfTriple final {
    HalfTriplePair half_triple_pair;

    explicit HalfTriple(const HalfTriplePair& half_triple_pair_)
        : half_triple_pair{half_triple_pair_} {}
};

// We need to have a forward declaration here, since this is the recursive case
struct Divide;

using Cover = boost::variant<Empty, Single, Triple, Divide>;

struct Divide final {

    // TODO Why can't this be a unique ptr?
    SharedPtr<Quarters<Cover>> quarters;

    explicit Divide(Cover&& cover0, Cover&& cover1, Cover&& cover2, Cover&& cover3)
        : quarters{cover0, cover1, cover2, cover3} {}
};
}
