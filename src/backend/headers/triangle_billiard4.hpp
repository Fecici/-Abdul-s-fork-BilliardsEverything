#pragma once


#include "triangle_billiard.hpp"



class TriangleBilliard4 {
public:
    int32_t side;
    int32_t orient;
    Vector2D vertexA;
    Vector2D vertexB;
    Vector2D vertexC;

    TriangleBilliard4(Vector2D& A, Vector2D& B, Vector2D& C, int32_t s, int32_t o);

    TriangleBilliard4(Vector2D& A, Vector2D& B, Vector2D& C,
                      int s, int o, std::vector<Vector2D>& L, std::vector<Vector2D>& R);

    static TriangleBilliard4 create(float64_t xAngle, float64_t yAngle);

    boost::optional<TriangleBilliard4> getNext(bool left) ;

    float64_t getSpecialAngle() const;

    bool between(float64_t perfectAngle) const;

    float64_t interval() const;

    std::string toString() const;

private:




    std::vector<Vector2D> lefts;
    std::vector<Vector2D> rights;

    float64_t specMin;
    float64_t specMax;

    static std::vector<Vector2D> reconfigure(bool left, std::vector<Vector2D>& L, std::vector<Vector2D>& R);

    static float64_t atan3( float64_t y,  float64_t x, bool left);

    static int32_t mod3(int32_t value);
};