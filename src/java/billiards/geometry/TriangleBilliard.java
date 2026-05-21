// This is a triangle along with some billiard methods
// For when you need a trianglular billiard that is convenient to unfold

package billiards.geometry;

import org.apache.commons.math3.util.FastMath;

import billiards.viewer.Utils;

public class TriangleBilliard {

    // side and orient are the side the shot is coming from and the triangle's orientation
    // with respect to the original triangle in the unfolding. Side is either 0, 1, or 2,
    // and orient is either 1 or -1.
    public final int side;
    public final int orient;

    // vertices of the triangle
    public final Vector2 vertexA;
    public final Vector2 vertexB;
    public final Vector2 vertexC;

    private TriangleBilliard(final Vector2 vertexA, final Vector2 vertexB, final Vector2 vertexC,
                             final int side, final int orient) { //, final double aL, final double bL, final double cL
        this.vertexA = vertexA;
        this.vertexB = vertexB;
        this.vertexC = vertexC;
        this.side = side;
        this.orient = orient;
    }

    public static TriangleBilliard create(final double xAngle, final double yAngle, final double pos) {

        if (xAngle + yAngle >= Math.PI) {
            throw new RuntimeException("Error: Angles given to a TriangleBilliard sum to over pi radians");
        }

        final double baseWidth = Math.sin(xAngle + yAngle);

        final Vector2 vertexA = Vector2.create(-pos, 0);
        final Vector2 vertexB = Vector2.create(baseWidth - pos, 0);

        final double cx = Math.sin(yAngle) * Math.cos(xAngle) + vertexA.x;
        final double cy = Math.sin(yAngle) * Math.sin(xAngle) + vertexA.y;

        final Vector2 vertexC = Vector2.create(cx, cy);

        return new TriangleBilliard(vertexA, vertexB, vertexC, 2, 1);
    }

    // calculates the next unfolded triangle of this triangle
    public TriangleBilliard getNext(final boolean left) {

        final int newSide;
        final Vector2 newA;
        final Vector2 newB;
        final Vector2 newC;

        if (left) {
            newA = this.vertexA;
            newB = this.vertexC;
            newSide = Utils.modN(this.side + this.orient * 2, 3);
            newC = Vector2.reflect(this.vertexA, this.vertexC, this.vertexB);
        } else {
            newA = this.vertexC;
            newB = this.vertexB;
            newSide = Utils.modN(this.side + this.orient, 3);
            newC = Vector2.reflect(this.vertexB, this.vertexC, this.vertexA);
        }

        return new TriangleBilliard(
            newA, newB, newC, newSide, -1 * this.orient);
    }

    public double getSpecialAngle() {
        return FastMath.atan2(vertexC.y, vertexC.x);
    }

    @Override
    public String toString() {
        return this.side + "/" + this.orient +
            " (" + this.vertexA + ", " + this.vertexB + ", " + this.vertexC + ")";
    }
}
