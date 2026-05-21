
#include "triangle_billiard.hpp"


/*
Jul 31 2025 Marco Mai
transfer from Java
*/
// ===== Vector2D class (minimal for this context) =====
Vector2D::Vector2D(float64_t x_, float64_t y_) : x(x_), y(y_) {}

Vector2D Vector2D::create(float64_t x, float64_t y) {
    return Vector2D(x, y);
}

Vector2D Vector2D::sub(Vector2D& v){
    // return Vector2D(this->x-v.x, this->y-v.y);
    float64_t newX = this->x-v.x;
    float64_t newY = this->y-v.y;
    return Vector2D(newX, newY);
}

Vector2D Vector2D::add(Vector2D& v){
    // return Vector2D(this->x+v.x, this->y+v.y);
    float64_t newX = this->x+v.x;
    float64_t newY = this->y+v.y;
    return Vector2D(newX, newY);
    // return Vector2D(x + v.x, y + v.y);
}

Vector2D Vector2D::scale(float64_t s){
    float64_t newX = s * this->x;
    float64_t newY = s * this->y;
    return Vector2D(newX,newY);
    // return Vector2D(s * x, s * y);
}

float64_t Vector2D::norm(){
    return std::hypot(x, y);
}

float64_t Vector2D::dot(Vector2D& v, Vector2D& w) {
    return v.x * w.x + v.y * w.y;
    //return std::fma(v.x, w.x, v.y * w.y);
}

float64_t Vector2D::cross(Vector2D& v, Vector2D& w) {
    return v.x * w.y - v.y * w.x;
    //return std::fma(v.x, w.y, -v.y * w.x);

}

Vector2D Vector2D::reflect(Vector2D& l1, Vector2D& l2, Vector2D& reflectMe) {
    Vector2D simple = reflectMe.sub(l2);
    Vector2D refLine = l1.sub(l2);
    Vector2D normal = refLine.scale(1.0 / refLine.norm());

    float64_t scale =  Vector2D::dot(simple, normal) * 2.0;
    return ((normal.scale(scale)).sub(simple)).add(l2);

}

std::string Vector2D::to_string() const {
    return "(" + std::to_string(x) + ", " + std::to_string(y) + ")";
}
bool Vector2D::equals(Vector2D& other) const {
    return std::abs(this->x - other.x) < 1e9 && std::abs(this->y - other.y) < 1e12;
}





/*
Jul 31 2025 Marco Mai
transfer from Java
*/
// ===== TriangleBilliard class =====
TriangleBilliard::TriangleBilliard(Vector2D& vertexA, Vector2D& vertexB, Vector2D& vertexC,
                                   int32_t side, int32_t orient)
    : side(side), orient(orient), vertexA(vertexA), vertexB(vertexB), vertexC(vertexC) {}

TriangleBilliard TriangleBilliard::create(float64_t xAngle, float64_t yAngle, float64_t pos) {
    float64_t pi = boost::math::constants::pi<double>();
    if (xAngle + yAngle >= pi) {
        throw std::runtime_error("Error: Angles given to a TriangleBilliard sum to over pi radians");
    }

    float64_t baseWidth = std::sin(xAngle + yAngle);

    Vector2D vertexA = Vector2D::create(-pos, 0);
    Vector2D vertexB = Vector2D::create(baseWidth - pos, 0);

    // std::fma (a*b)+c
    float64_t cx = std::fma(std::sin(yAngle), std::cos(xAngle), vertexA.x);
    float64_t cy = std::fma(std::sin(yAngle), std::sin(xAngle), vertexA.y);

    Vector2D vertexC = Vector2D::create(cx, cy);

    return TriangleBilliard(vertexA, vertexB, vertexC, 2, 1);
}


/// @brief this function is to test the reverseabilty of get Next
/// @param left 
void TriangleBilliard::getNext2(bool left) {
    int32_t newSide;
    Vector2D newA, newB, newC;

    if (left) {
        newA = vertexA;
        newB = vertexC;
        newSide = modN(side + orient * 2, 3);

        newC = Vector2D::reflect(vertexA, vertexC, vertexB);
    } else {
        newA = vertexC;
        newB = vertexB;
        newSide = modN(side + orient, 3);
        newC = Vector2D::reflect(vertexB, vertexC, vertexA);
    }


    vertexA = newA;
    vertexB = newB;
    vertexC = newC;
    side = newSide;
    orient = -orient;
}

TriangleBilliard TriangleBilliard::getNext(bool left) {
    int32_t newSide;
    Vector2D newA, newB, newC;

    if (left) {
        newA = vertexA;
        newB = vertexC;
        newSide = modN(side + orient * 2, 3);

        newC = Vector2D::reflect(vertexA, vertexC, vertexB);
    } else {
        newA = vertexC;
        newB = vertexB;
        newSide = modN(side + orient, 3);
        newC = Vector2D::reflect(vertexB, vertexC, vertexA);
    }
    return TriangleBilliard(newA, newB, newC, newSide, -1* orient);
}

/// @brief this function is to test the reverseabilty of get Next
/// @param left 
void TriangleBilliard::getNextReverse(bool left) {
    Vector2D oldA, oldB, oldC;
    int32_t oldSide;

    if (left) {
        // Forward: A,B,C → A,C,R(B)
        // Reverse: A,C,R(B) → A,B,C
        // reverse of getNext(true)
        oldA = vertexA;
        oldC = vertexB;
        oldB = Vector2D::reflect(vertexA, vertexB, vertexC);  // undo reflect(vertexA, vertexC, vertexB)
        oldSide = modN(side + orient * 2, 3);
    } else {
        // Forward: A,B,C → C,B,R(A)
        // Reverse: C,B,R(A) → A,B,C
        // reverse of getNext(false)
        oldB = vertexB;
        oldC = vertexA;
        oldA = Vector2D::reflect(vertexB, vertexA, vertexC);  // undo reflect(vertexB, vertexC, vertexA)
        oldSide = modN(side + orient, 3);
    }

    this->vertexA = oldA;
    this->vertexB = oldB;
    this->vertexC = oldC;
    this->side = oldSide;
    this->orient = -orient;
    // return TriangleBilliard(oldA, oldB, oldC, oldSide, oldOrient);
}

float64_t TriangleBilliard::getSpecialAngle() {
    return std::atan2(static_cast<float64_t>(vertexC.y), static_cast<float64_t>(vertexC.x));
}

bool TriangleBilliard::equals(TriangleBilliard &other) const {
    float eps = 1e9;
    bool sideCompare = std::abs(side -other.side) <eps;
    bool orientCompare =  std::abs(orient -other.orient) <eps;
    bool VaComapre = vertexA.equals(other.vertexA);
    bool VbComapre = vertexB.equals(other.vertexB);
    bool VcComapre = vertexC.equals(other.vertexC);
    std::cout << sideCompare << orientCompare << VaComapre << VbComapre << VcComapre << std::endl;
    return sideCompare && orientCompare && VaComapre && VbComapre && VcComapre ;
}


std::string TriangleBilliard::to_string() const {
    std::ostringstream oss;
    oss << side << "/" << orient
        << " (" << vertexA.to_string() << ", "
        << vertexB.to_string() << ", "
        << vertexC.to_string() << ")";
    return oss.str();
}



