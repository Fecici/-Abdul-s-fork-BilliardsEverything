#ifndef TRIANGLE_BILLIARD_HPP
#define TRIANGLE_BILLIARD_HPP

#include <boost/multiprecision/mpfr.hpp>
#include <cstdint>
#include <boost/cstdfloat.hpp>
#include "utils.hpp"

#include <cmath>
#include <string>
#include <iostream>
#include <cmath>
#include <stdexcept>
#include <string>
#include <sstream>


/*
Jul 31 2025 Marco Mai
transfer from Java
*/
//===== Vector2 class =====
class Vector2D {
public:
    float64_t x;
    float64_t y;

    Vector2D(float64_t x_, float64_t y_);

    Vector2D() : x(0.0), y(0.0) {} // Add this!

    static Vector2D create(float64_t x, float64_t y);

    Vector2D sub(Vector2D& v);
    Vector2D add(Vector2D& v);
    Vector2D scale(float64_t s);
    Vector2D perp(Vector2D v);
    float64_t cross(Vector2D& v, Vector2D& w);
    float64_t norm();
    bool equals(Vector2D& other) const;

    static float64_t dot(Vector2D& a, Vector2D& b);
    static Vector2D reflect(Vector2D& l1, Vector2D& l2, Vector2D& reflectMe);


    std::string to_string() const;
};

/*
Jul 31 2025 Marco Mai
transfer from Java
*/
// ===== TriangleBilliard class =====
class TriangleBilliard {
public:
    int32_t side;
    int32_t orient;

    Vector2D vertexA;
    Vector2D vertexB;
    Vector2D vertexC;

    TriangleBilliard(Vector2D& vertexA, Vector2D& vertexB, Vector2D& vertexC,
                     int32_t side, int32_t orient);

    static TriangleBilliard create(float64_t xAngle, float64_t yAngle, float64_t pos);

    TriangleBilliard getNext(bool left) ;
    void getNext2(bool left);
    void getNextReverse(bool left);

    bool equals(TriangleBilliard &other) const;

    float64_t getSpecialAngle();

    std::string to_string() const;
};

#endif // TRIANGLE_BILLIARD_HPP