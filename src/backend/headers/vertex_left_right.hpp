#pragma once

#include <ostream>

struct Vertex final {
    // We use size_t here, because these numbers depend on the
    // size of the unfolding.
    size_t number;
    size_t branch;

    explicit Vertex(const size_t number_, const size_t branch_)
        : number{number_}, branch{branch_} {}

    friend bool operator==(const Vertex& lhs, const Vertex& rhs) {
        return std::tie(lhs.number, lhs.branch) == std::tie(rhs.number, rhs.branch);
    }

    friend bool operator<(const Vertex& lhs, const Vertex& rhs) {
        return std::tie(lhs.number, lhs.branch) < std::tie(rhs.number, rhs.branch);
    }

    friend std::ostream& operator<<(std::ostream& os, const Vertex& vertex) {
        return os << '(' << vertex.number << ", " << vertex.branch << ')';
    }
};

struct LeftRight final {
    Vertex left;
    Vertex right;

    explicit LeftRight(const Vertex& left_, const Vertex& right_)
        : left{left_}, right{right_} {}

    friend bool operator==(const LeftRight& lhs, const LeftRight& rhs) {
        return std::tie(lhs.left, lhs.right) == std::tie(rhs.left, rhs.right);
    }

    friend bool operator<(const LeftRight& lhs, const LeftRight& rhs) {
        return std::tie(lhs.left, lhs.right) < std::tie(rhs.left, rhs.right);
    }

    friend std::ostream& operator<<(std::ostream& os, const LeftRight& lr) {
        return os << '(' << lr.left << ", " << lr.right << ')';
    }
};
