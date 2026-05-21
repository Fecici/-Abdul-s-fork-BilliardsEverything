#if defined(_WIN64)
#define _USE_MATH_DEFINES
#endif

#include "triangle_billiard4.hpp"
#include <cmath>
#include <stdexcept>
#include <sstream>

TriangleBilliard4::TriangleBilliard4(Vector2D& A, Vector2D& B, Vector2D& C, int32_t s, int32_t o)
    : vertexA(A), vertexB(B), vertexC(C), side(s), orient(o), specMin(0), specMax(M_PI) {
    lefts.push_back(vertexA);
    rights.push_back(vertexB);
}

TriangleBilliard4::TriangleBilliard4(Vector2D& A, Vector2D& B, Vector2D& C,
                                     int32_t s, int32_t o, std::vector<Vector2D>& L, std::vector<Vector2D>& R)
    : vertexA(A), vertexB(B), vertexC(C), side(s), orient(o), lefts(L), rights(R) {
    Vector2D specMinV = rights.back().sub(lefts.front());
    specMin = atan3(specMinV.y, specMinV.x, false);
    Vector2D specMaxV = lefts.back().sub(rights.front());
    specMax = atan3(specMaxV.y, specMaxV.x, true);
}

TriangleBilliard4 TriangleBilliard4::create(float64_t xAngle, float64_t yAngle) {
    if (xAngle + yAngle >= M_PI) {
        throw std::runtime_error("Angles given to TriangleBilliard sum to over pi radians");
    }

    float64_t baseWidth = std::sin(xAngle + yAngle);
    Vector2D vertexA(0, 0);
    Vector2D vertexB(baseWidth, 0);

    float64_t cx = std::sin(yAngle) * std::cos(xAngle);
    float64_t cy = std::sin(yAngle) * std::sin(xAngle);
    Vector2D vertexC(cx, cy);

    return TriangleBilliard4(vertexA, vertexB, vertexC, 2, 1);
}

boost::optional<TriangleBilliard4> TriangleBilliard4::getNext(bool left) {
    std::vector<Vector2D> tempL = lefts;
    std::vector<Vector2D> tempR = rights;

    Vector2D direc1 = vertexC;
    direc1.sub(tempR.front());
    float64_t newAngle1 = atan3(direc1.y, direc1.x, false);

    Vector2D direc2 = vertexC;
    direc2.sub(tempL.front());
    float64_t newAngle2 = atan3(direc2.y, direc2.x, true);

    if (left) {
        if (newAngle1 >= specMax) return boost::none;
        if (newAngle2 > specMin) {
            tempR.push_back(vertexC);
            tempL = reconfigure(true, tempL, tempR);
        }
    } else {
        if (newAngle2 <= specMin) return boost::none;
        if (newAngle1 < specMax) {
            tempL.push_back(vertexC);
            tempR = reconfigure(false, tempL, tempR);
        }
    }

    int32_t newSide;
    Vector2D newA, newB, newC;

    if (left) {
        newA = vertexA;
        newB = vertexC;
        newSide = mod3(side + orient * 2);
        newC = Vector2D::reflect(vertexA, vertexC, vertexB);
    } else {
        newA = vertexC;
        newB = vertexB;
        newSide = mod3(side + orient);
        newC = Vector2D::reflect(vertexB, vertexC, vertexA);
    }

    return TriangleBilliard4(newA, newB, newC, newSide, -orient, tempL, tempR);
}

float64_t TriangleBilliard4::getSpecialAngle() const {
    return std::atan2(vertexC.y, vertexC.x);
}

bool TriangleBilliard4::between(float64_t perfectAngle) const {
    return specMax > perfectAngle && perfectAngle > specMin;
}

float64_t TriangleBilliard4::interval() const {
    return specMax - specMin;
}

std::string TriangleBilliard4::toString() const {

    std::string str = std::to_string(side) + "/" + std::to_string(orient) + " ";
    str += "(" + vertexA.to_string() + ", " + vertexB.to_string() + ", " + vertexC.to_string() + ")";
    str += "\nL: ";
    for (const auto& v : lefts) {
        str += v.to_string() + " ";
    }
    str += "\nR: ";
    for (const auto& v : rights) {
        str += v.to_string() + " ";
    }
    return str;
}

std::vector<Vector2D> TriangleBilliard4::reconfigure(bool left, std::vector<Vector2D>& L, std::vector<Vector2D>& R) {
    if (left) {
        float64_t specMin = 0;
        int32_t index = 0;
        Vector2D end = R.back();
        for (int32_t i = 0; i < static_cast<int32_t>(L.size()); ++i) {
            Vector2D direc = end;
            direc.sub(L[i]);
            float64_t result = std::abs(std::atan2(direc.y, direc.x));
            if (std::abs(specMin) < result) {
                specMin = result;
                index = i;
            }
        }
        return std::vector<Vector2D>(L.begin() + index, L.end());
    } else {
        float64_t specMax = M_PI;
        int32_t index = 0;
        Vector2D end = L.back();
        for (int32_t i = 0; i < static_cast<int32_t>(R.size()); ++i) {
            Vector2D direc = end;
            direc.sub(L[i]);
            float64_t result = std::abs(std::atan2(direc.y, direc.x));
            if (std::abs(specMax) > result) {
                specMax = result;
                index = i;
            }
        }
        return std::vector<Vector2D>(R.begin() + index, R.end());
    }
}

float64_t TriangleBilliard4::atan3(float64_t y, float64_t x, bool left) {
    float64_t result = std::atan2(y, x);
    if (result < 0) {
        result = left ? 0 : M_PI;
    }
    return result;
}

int32_t TriangleBilliard4::mod3(int32_t value) {
    while (value >= 3) value -= 3;
    while (value < 0) value += 3;
    return value;
}