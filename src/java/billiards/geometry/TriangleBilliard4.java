// This is a triangle along with some billiard methods
// For when you need a trianglular billiard that is convenient to unfold
// TriangleBilliard4 includes a 'trail' of previous vertices, for the 
// purposes of keeping track of the specMin/Max

package billiards.geometry;

import java.util.Optional;

import org.apache.commons.math3.util.FastMath;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

public class TriangleBilliard4 {

    // side and orient are the side the shot is coming from and the triangle's orientation
    // with respect to the original triangle in the unfolding. Side is either 0, 1, or 2,
    // and orient is either 1 or -1.
    public final int side;
    public final int orient;

    // vertices of the triangle
    public final Vector2 vertexA;
    public final Vector2 vertexB;
    public final Vector2 vertexC;
    
    // 'trail' of previous vertices
	private final MutableList<Vector2> lefts = new FastList<>();
	private final MutableList<Vector2> rights = new FastList<>();
	
	private final double specMin;
	private final double specMax;

	// This is used when using getNext
    private TriangleBilliard4(final Vector2 vertexA, final Vector2 vertexB, final Vector2 vertexC,
    						  final int side, final int orient, final MutableList<Vector2> left, 
    						  final MutableList<Vector2> right) {
        this.vertexA = vertexA;
        this.vertexB = vertexB;
        this.vertexC = vertexC;
        this.side = side;
        this.orient = orient;
        lefts.addAll(left);
        rights.addAll(right);
        
		final Vector2 specMinV = rights.getLast().sub(lefts.getFirst());
		specMin = atan3(specMinV.y, specMinV.x, false);
        final Vector2 specMaxV = lefts.getLast().sub(rights.getFirst());
		specMax = atan3(specMaxV.y, specMaxV.x, true);
    }
    
    // This is used when using create
    private TriangleBilliard4(final Vector2 vertexA, final Vector2 vertexB, final Vector2 vertexC,
			  final int side, final int orient) {
		this.vertexA = vertexA;
		this.vertexB = vertexB;
		this.vertexC = vertexC;
		this.side = side;
		this.orient = orient;
		lefts.add(vertexA);
		rights.add(vertexB);
		
		specMin = 0;
		specMax = Math.PI;
    }

    public static TriangleBilliard4 create(final double xAngle, final double yAngle) {

        if (xAngle + yAngle >= Math.PI) {
            throw new RuntimeException("Error: Angles given to a TriangleBilliard sum to over pi radians");
        }

        final double baseWidth = Math.sin(xAngle + yAngle);

        final Vector2 vertexA = Vector2.create(0, 0);
        final Vector2 vertexB = Vector2.create(baseWidth, 0);

        final double cx = Math.sin(yAngle) * Math.cos(xAngle) + vertexA.x;
        final double cy = Math.sin(yAngle) * Math.sin(xAngle) + vertexA.y;

        final Vector2 vertexC = Vector2.create(cx, cy);

        return new TriangleBilliard4(vertexA, vertexB, vertexC, 2, 1);
    }

    // calculates the next unfolded triangle of this triangle
    public Optional<TriangleBilliard4> getNext(final boolean left) {
    	MutableList<Vector2> tempL = new FastList<>();
    	MutableList<Vector2> tempR = new FastList<>();
    	tempL.addAll(lefts);
    	tempR.addAll(rights);
		final Vector2 direc1 = vertexC.sub(tempR.getFirst());
    	final double newAngle1 = atan3(direc1.y, direc1.x, false);
		
    	final Vector2 direc2 = vertexC.sub(tempL.getFirst());
    	final double newAngle2 = atan3(direc2.y, direc2.x, true);
    	
    	if (left) {

			if (newAngle1 >= specMax) {
				return Optional.empty();
			} 
			if (newAngle2 > specMin) {
				tempR.add(vertexC);
				tempL = reconfigure(true, tempL, tempR);
			}
		} else {
	    	
			if (newAngle2 <= specMin) {
				return Optional.empty();
			} if (newAngle1 < specMax) {
				tempL.add(vertexC);
				tempR = reconfigure(false, tempL, tempR);
			}
		}
    	
        final int newSide;
        final Vector2 newA;
        final Vector2 newB;
        final Vector2 newC;

        final Optional<TriangleBilliard4> newOne;
        if (left) {
            newA = this.vertexA;
            newB = this.vertexC;
            newSide = mod3(this.side + this.orient * 2);
            newC = Vector2.reflect(this.vertexA, this.vertexC, this.vertexB);
            newOne = Optional.of(new TriangleBilliard4(
                    newA, newB, newC, newSide, -1 * this.orient, tempL, tempR));
            
        } else {
            newA = this.vertexC;
            newB = this.vertexB;
            newSide = mod3(this.side + this.orient);
            newC = Vector2.reflect(this.vertexB, this.vertexC, this.vertexA);
            newOne = Optional.of(new TriangleBilliard4(
                    newA, newB, newC, newSide, -1 * this.orient, tempL, tempR));
        }
        return newOne;
    }

    public double getSpecialAngle() {
        return FastMath.atan2(vertexC.y, vertexC.x);
    }
    
    public boolean between(final double perfectAngle) {
    	
    	return specMax > perfectAngle && perfectAngle > specMin;
    }
    
    public double interval() {
    	return specMax - specMin;
    }
    
	private static MutableList<Vector2> reconfigure(
			boolean left, MutableList<Vector2> L, MutableList<Vector2> R) {

		if (left) {
			double specMin = 0;
			int index = 0;
			final Vector2 end = R.getLast();
			for (int i = 0; i < L.size(); i++) {
				final Vector2 direc = end.sub(L.get(i));
				final double result =  Math.abs(FastMath.atan2(direc.y, direc.x));
				if (Math.abs(specMin) < result) {
					specMin = result;
					index = i;
				}
			}
			
			return L.drop(index);
			
		} else {
			double specMax = Math.PI;
			int index = 0;
			final Vector2 end = L.getLast();
			for (int i = 0; i < R.size(); i++) {
				final Vector2 direc = end.sub(R.get(i));
				final double result =  Math.abs(FastMath.atan2(direc.y, direc.x));
				if (Math.abs(specMax) > result) {
					specMax = result;
					index = i;
				}
			}

			return R.drop(index);
		}
		
	}
	
	// we don't want negative angles, but it is possible to have a valid unfolding that
	// dips into the negative y coordinates. For this we set all negative values to 0 if
	// unfolding left, and pi if unfolding right.
	private static double atan3(double y, double x, boolean left) {
		double result = FastMath.atan2(y, x);
		if (result < 0) {
			if (left) {
				result = 0;
			} else {
				result = Math.PI;
			}
		}
		return result;
	}
	
	// the usual %3 wasn't working for some reason, so we do this

    // Cycle through the three sides of a triangle
    private static int mod3(int value) {
        while (value >= 3) {
            value -= 3;
        }
        while (value < 0) {
            value += 3;
        }
        return value;
    }

    @Override
    public String toString() {
        return this.side + "/" + this.orient +
            " (" + this.vertexA + ", " + this.vertexB + ", " + this.vertexC + ")\nL: " + lefts + "\nR: " + rights;
    }
}
