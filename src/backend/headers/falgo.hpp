#pragma once

#include <algorithm>
#include <numeric>
#include <vector>

#include <boost/optional.hpp>
#include <boost/range/irange.hpp>

// There are a lot of nice algorithms in <algorithm>
// However, they all operate on iterators, not containers,
// and using iterators opens up a whole load of undefined
// behaviour trouble. So, we provide some wrappers around
// the algorithms in <algorithm> that operate on containers
// instead, and are nicer in general
// Plus, when passing two iterators, how do you know they
// even point to the same container? You don't! That is
// why you provide some safe wrappers around these
// algorithms
// https://github.com/Dobiasd/FunctionalPlus/ is awesome
// we use that for inspiration

// Huh, all of this may be replaced by Boost.Range
// You can write a functional algorithm using a pair of iterators,
// or by using a range, which encapsulates those two. In think
// the latter is better. A range is similar to an iterable in other languages
namespace falgo {

// For an empty container, std::begin(xs) == std::end(xs)

// We should be able to implement everything in here using
// just the stuff in <iterator>, no other member function

// C++17 adds some nice functions to <iterator>, like size and
// empty, so we'll use those when C++17 comes out.

// boost range has some nice stuff
// use this any time you want to iterate over an integer range
// no more for (int i = start; i < upper, ++i) nonsense
template <typename Integer>
boost::integer_range<Integer> range(const Integer upper) {
    return boost::irange(static_cast<Integer>(0), upper);
}

template <typename Integer>
boost::integer_range<Integer> range(const Integer lower, const Integer upper) {
    return boost::irange(lower, upper);
}

template <typename Integer>
boost::integer_range<Integer> range(const Integer lower, const Integer upper, const Integer step) {
    return boost::irange(lower, upper, step);
}

// The versions of front() and back() in std::vector have undefined behaviour
// if the vector is empty. These are safe bounds checked versions.
// TODO this is mostly an academic exercise, but create wrappers around the
// STL containers that have the Rust API. They will not have undefined behaviour
// (bounds checking is done by default), and will be less confusing ([] inserts
// if it can't be found).
// Perhaps base them off of Boost.Container, since that doesn't contain things
// like std::vector<bool> and other surprises
template <typename T>
typename std::vector<T>::reference front(std::vector<T>& vec) {
    return vec.at(0);
}

template <typename T>
typename std::vector<T>::const_reference front(const std::vector<T>& vec) {
    return vec.at(0);
}

template <typename T>
boost::optional<typename std::vector<T>::const_iterator> find(const std::vector<T>& vec, const T& value) {

    const auto loc = std::find(cbegin(vec), cend(vec), value);
    if (loc == cend(vec)) {
        return boost::none;
    } else {
        return loc;
    }
}

template <typename T>
void unique(std::vector<T>& vec) {
    sort(begin(vec), end(vec));
    vec.erase(unique(begin(vec), end(vec)), end(vec));
}

// Trivial base case
template <typename T>
void append(std::vector<T>&) {}

template <typename T, typename... Args>
void append(std::vector<T>& a, const std::vector<T>& b, const Args&... args) {
    // a and b cannot be the same vector
    a.insert(a.end(), b.cbegin(), b.cend());
    append(a, args...);
}

template <typename T>
typename std::vector<T>::reference back(std::vector<T>& vec) {
    return vec.at(vec.size() - 1);
}

template <typename T>
typename std::vector<T>::const_reference back(const std::vector<T>& vec) {
    return vec.at(vec.size() - 1);
}

template <typename T, typename S>
std::vector<std::pair<T, S>> zip(const std::vector<T>& first, const std::vector<T>& second) {

    const auto size = std::min(first.size(), second.size());

    std::vector<std::pair<T, S>> result;
    for (size_t i = 0; i < size; ++i) {
        result.emplace_back(first.at(i), second.at(i));
    }

    return result;
}

template <typename Container>
bool prev_permutation(Container& cont) {
    return std::prev_permutation(std::begin(cont), std::end(cont));
}

template <typename Container>
bool next_permutation(Container& cont) {
    return std::next_permutation(std::begin(cont), std::end(cont));
}

template <typename Container>
void rotate_left(Container& cont) {
    if (std::begin(cont) != std::end(cont)) {
        std::rotate(std::begin(cont), std::next(std::begin(cont)), std::end(cont));
    }
}

template <typename Container, typename Func>
bool all_of(const Container& cont, const Func& func) {
    return std::all_of(cbegin(cont), cend(cont), func);
}

template <typename Container>
void sort(Container& cont) {
    std::sort(std::begin(cont), std::end(cont));
}

template <typename Container, typename Comp>
void sort(Container& cont, const Comp& comp) {
    std::sort(std::begin(cont), std::end(cont), comp);
}

template <typename Container>
void reverse(Container& cont) {
    std::reverse(std::begin(cont), std::end(cont));
}

template <typename Container>
typename Container::value_type sum(const Container& cont) {
    return std::accumulate(std::cbegin(cont), std::cend(cont), typename Container::value_type{0});
}

template <typename T>
const T& min(const T& t) {
    return t;
}

template <typename T, typename... Ts>
const T& min(const T& t, const Ts&... ts) {
    return std::min(t, min(ts...));
}

template <typename T>
const T& max(const T& t) {
    return t;
}

template <typename T, typename... Ts>
const T& max(const T& t, const Ts&... ts) {
    return std::max(t, max(ts...));
}

// Repeat the given function n times
template <typename Func>
void repeat_n(const Func& func, const std::uintmax_t n) {
    for (std::uintmax_t i = 0; i < n; ++i) {
        func();
    }
}

template <typename Container, typename Func>
void for_each(const Container& cont, const Func& func) {
    std::for_each(std::begin(cont), std::end(cont), func);
}

template <typename T, size_t N, typename Func>
auto transform(const std::array<T, N>& arr, const Func& func) {

    // Everything is left uninitialized
    std::array<decltype(func(arr.at(0))), N> result;
    std::transform(std::cbegin(arr), std::cend(arr), std::begin(result), func);

    return result;
}

template <typename T, typename Func>
auto transform(const std::vector<T>& vec, const Func& func) {

    // We don't initialize it with the number of elements, because that
    // default constructs the elements, which may not be default constructible.
    std::vector<decltype(func(vec.at(0)))> result{};
    std::transform(std::cbegin(vec), std::cend(vec), std::back_inserter(result), func);

    return result;
}

// This is a variadiac template version of fold
// Normal fold takes the func, an initial accum,
// and a list of to fold. However, in this case
// we simply provide a list of things to fold, so there
// is no distinction between the initial accum and the
// rest of the list
// This may no longer be necessary in C++17 though, which
// comes with fold expressions
// Or not. Fold expressions only work with operators, not
// general function objects.
// Though there is also the comma operator

// based on https://github.com/Morwenn/cpp-fold/blob/master/include/cpp-fold/fold/lfold.h

// Things are a little different in C++ than Haskell, because we pass
// the type of the function class rather than the function itself
// Not ideal, but whatever
#if 0
template <typename BinaryFunction, typename First>
decltype(auto) foldl(First&& first) {
    return first;
}

template <typename BinaryFunction, typename First, typename Second>
decltype(auto) foldl(First&& first, Second&& second) {
    // universal reference for perfect forwarding
    return BinaryFunction{}(
        std::forward<First>(first),
        std::forward<Second>(second));
}

template <typename BinaryFunction, typename First, typename Second, typename... Rest>
decltype(auto) foldl(First&& first, Second&& second, Rest&&... rest) {
    // universal reference for perfect forwarding
    return foldl<BinaryFunction>(
        BinaryFunction{}(std::forward<First>(first), std::forward<Second>(second)),
        std::forward<Rest>(rest)...);
}

template <typename BinaryFunction, typename First>
decltype(auto) foldr(First&& first) {
    return first;
}

template <typename BinaryFunction, typename First, typename Second>
decltype(auto) foldr(First&& first, Second&& second) {
    // universal reference for perfect forwarding
    return BinaryFunction{}(
        std::forward<First>(first),
        std::forward<Second>(second));
}

template <typename BinaryFunction, typename First, typename Second, typename... Rest>
decltype(auto) foldr(First&& first, Second&& second, Rest&&... rest) {
    // universal reference for perfect forwarding
    return BinaryFunction{}(std::forward<First>(first),
        foldl<BinaryFunction>(std::forward<Second>(second),
            std::forward<Rest>(rest)...));
}

template <typename Func, typename T>
const T& foldr(const Func&, const T& first) {
    return first;
}

template <typename Func, typename T, typename... S>
const T& foldr(const Func& func, const T& first, const S&... rest) {
    // Use auto& just to be safe, since auto will make
    // a copy
    const auto& foldr_rest = foldr(func, rest...);
    const auto& func_result = func(first, foldr_rest);
    return func_result;
}

// Takes a function f, and accumulator, and a container, and applies
// it to the thing. See functional plus for an example
// We can't recurse on these data structures, so there we go
template <typename Func, typename Acc, typename Container>
Acc foldl(const Func& f, Acc& acc, const Container& cont) {
    for (const auto& elem : cont) {
        acc = f(acc, elem);
    }
    return acc;
}

// TODO for fun sometime, let's implement fold using
// variadic templates. But not now. I think we could
// implement min using that. Hmmm,
template <typename T>
const T& min(const T& first) {
    return first;
}

template <typename T, typename... S>
const T& min(const T& first, const S&... rest) {
    // Use auto& just to be safe, since auto will make
    // a copy
    const auto& min_rest = min(rest...);
    // std::less is the way to go, since it provides a
    // total ordering on pointers, while operator< might not
    const auto less_than = std::less<T>{}(first, min_rest);
    return less_than ? first : min_rest;
}
#endif

template <typename Container>
void min_rotation(Container& cont) {

    auto min_cont = cont;

    const auto func = [&] {
        rotate_left(cont);
        min_cont = min(min_cont, cont);
    };

    repeat_n(func, cont.size());

    cont = min_cont;
}

template <typename Container>
std::vector<std::pair<std::size_t, typename Container::value_type>> enumerate(const Container& cont) {
    std::vector<std::pair<std::size_t, typename Container::value_type>> vec;

    std::size_t i = 0;
    for (auto it = std::cbegin(cont); it != std::cend(cont); ++it) {
        vec.emplace_back(i, *it);
        ++i;
    }

    return vec;
}

// How do you specify a range of elements?
// A generator, or something?
// One way is to specify a function to call, and then the number
// of times to call it
// Hmmm, I don't like this function very much.
// It's not as elegant or simple as I would like
template <typename Val, typename Gen, typename Size>
void min_range(Val& min, const Gen& gen, const Size& n) {

    for (Size i = 0; i < n; ++i) {

        const auto value = gen();
        if (value < min) {
            min = value;
        }
    }
}
} // namespace falgo
