package billiards.geometry;

import org.apache.commons.math3.util.FastMath;
import org.eclipse.collections.api.list.ImmutableList;

import billiards.viewer.Utils;

// Ideally, I would just use an external library, but I can't find any good ones
// for Java. It looks like JTS is no longer being developed
// It is also true that we can tune these algorithms for the special cases we have to
// worry about (eg, intersection of rectangle with convex polygon),
// though a good library would likely do the same thing.
// Whenever you are writing your own library code, the first rule is NEVER INVENT YOUR OWN
// ALGORITHMS
// always, *always*, **always** look up preexisting algorithms in books or on the internet
// even for very simple stuff, like checking if two intervals intersect
public final class ConvexPolygon implements Project {

    public final ImmutableList<Vector2> vertices;

    private ConvexPolygon(final ImmutableList<Vector2> vertices) {
        this.vertices = vertices;
        
        //george aug 26,2019 using 1 3 3 say
        //System.out.print("vertices " + vertices + "\n");//vertices [(0.39269908169872414, 0.7853981633974483), (0.7853981633974483, 0.39269908169872414), (0.7853981633974483, 0.7853981633974483)]

    }

    public static ConvexPolygon create(final ImmutableList<Vector2> points) {
        if (points.size() < 3) {
            throw new RuntimeException("Error: not enough points for a convex polygon");
        }

        if (!giftWrapCheck(points)) {
            throw new RuntimeException("Error: tried to make a convex polygon that was"
            		+ " not convex");
        }
        return new ConvexPolygon(points);
    }
    
    
    private static boolean giftWrapCheck(final ImmutableList<Vector2> points) {
    	
    	Vector2 current = points.get(0);
    	
    	// either all the angles will be less than 180 degrees or all will be more.
    	// we find out which it should be after trying the first angle.
    	int threeState = 0;
    	
    	for (int i = 0; i < points.size(); i++) {
    		final int iMinus = Utils.modN(i - 1, points.size());
    		final int iPlus = Utils.modN(i + 1, points.size());
    		
    		final Vector2 back = points.get(iMinus).sub(current);
    		final Vector2 forth = points.get(iPlus).sub(current);
    		
    		//george aug 26,2019 using 1 3 3 say only for stable
    		//System.out.print("back " + back + "\n");
    		//System.out.print("forth " + forth + "\n");
    		/*back (0.39269908169872414, 0.0)
              forth (0.39269908169872414, -0.39269908169872414)
              back (0.0, 0.0)
              forth (0.39269908169872414, 0.0)
              back (0.39269908169872414, -0.39269908169872414)
              forth (0.0, 0.0)*/
    		
    		final double angle = FastMath.acos(Vector2.dot(back, forth) / (back.norm() * forth.norm()));

    	
    		if (angle > Math.PI) {
    			if (threeState == 0) {
    				threeState = 1;
    			} else if (threeState == -1) {
    				return false;
    			}
    		}
    		
    		if (angle < Math.PI) {
    			if (threeState == 0) {
    				threeState = -1;
    			} else if (threeState == 1) {
    				return false;
    			}
    		}
    	}
    	return true;
    }
    
    // algorithm taken from http://paulbourke.net/geometry/polygonmesh
    public Location location(final double x, final double y) {
        // in theory, it is possible to determine if a point is on the boundary
        // but with floating point math you have no guarantee
        Vector2 p0 = this.vertices.get(0);
        Vector2 p1 = this.vertices.get(1);

        final int firstSideSign = sign(p0, p1, x, y);
        if (firstSideSign == 0) {
            return Location.BOUNDARY;
        }

        final int size = this.vertices.size();
        for (int i = 1; i < size; ++i) {
            p0 = this.vertices.get(i);
            p1 = this.vertices.get((i + 1) % size);

            final int currentSideSign = sign(p0, p1, x, y);

            if (currentSideSign == 0) {
                return Location.BOUNDARY;
            } else if (currentSideSign != firstSideSign) {
                return Location.OUTSIDE;
            }
        }

        // if we have the same sign for everything, we are on the inside
        return Location.INSIDE;
    }

    private static int sign(final Vector2 v0, final Vector2 v1, final double x, final double y) {
        // final double value = (y - y0) * (x1 - x0) - (x - x0) * (y1 - y0);
        final double value = (y - v0.y) * (v1.x - v0.x) - (x - v0.x) * (v1.y - v0.y);

        if (value < 0) {
            return -1;
        } else if (value > 0) {
            return 1;
        } else if (value == 0) {
            return 0;
        } else {
            throw new RuntimeException("unorderable double in ConvexPolygon::sign");
        }
    }

    // Separating axis theorem
    // This is often called polygon collision in the gaming industry
    // http://www.dyn4j.org/2010/01/sat/

    // If we find a separating axis, they do not intersect. If we do not
    // find one, then they do intersect.

    // Return if we have found a separating axis by projecting the figure
    // and this along the axes of this
    public boolean separatingAxis(final Project figure) {
        // Now we get the axes from the sides of the polygon,
        // and project the rectangle and polygon along those
        // axes
        final int size = this.vertices.size();
        for (int i = 0; i < size; ++i) {
            final Vector2 p0 = this.vertices.get(i);
            final Vector2 p1 = this.vertices.get((i + 1) % size);

            // Vector from p0 to p1
            // The order and perpendicular vector are rather arbitrary
            // as long as they lie on the same line
            final Vector2 edge = p1.sub(p0);
            final Vector2 axis = Vector2.create(-edge.y, edge.x);

            final Interval figureProject = figure.project(axis);
            final Interval polyProject = this.project(axis);

            if (!Interval.intersects(figureProject, polyProject)) {
                return true;
            }
        }

        return false;
    }

    public boolean intersects(final ConvexPolygon poly) {
        // If we do not find a separating axis for the first one and we don't find a
        // separating axis for the second, then they do intersect.
        return !(poly.separatingAxis(this) || this.separatingAxis(poly));
    }

    public boolean intersects(final Rectangle rect) {
        // The axes for the rectangle are just the x and y
        // axes

        final Interval rectProjectX = rect.projectX();
        final Interval polyProjectX = this.projectX();

        if (!Interval.intersects(rectProjectX, polyProjectX)) {
            return false;
        }

        final Interval rectProjectY = rect.projectY();
        final Interval polyProjectY = this.projectY();

        if (!Interval.intersects(rectProjectY, polyProjectY)) {
            return false;
        }

        return !this.separatingAxis(rect);
    }

    // Project the polygon onto the x axis
    // so find the min and max x
    public Interval projectX() {
        final Vector2 first = this.vertices.get(0);

        double min = first.x;
        double max = first.x;

        for (final Vector2 point : this.vertices) {
            final double x = point.x;
            min = Math.min(min, x);
            max = Math.max(max, x);
        }

        return Interval.create(min, max);
    }

    // Ditto
    public Interval projectY() {
        final Vector2 first = this.vertices.get(0);

        double min = first.y;
        double max = first.y;

        for (final Vector2 point : this.vertices) {
            final double y = point.y;
            min = Math.min(min, y);
            max = Math.max(max, y);
        }

        return Interval.create(min, max);
    }

    @Override
    public Interval project(final Vector2 axis) {
        final Vector2 first = this.vertices.get(0);

        final double firstDot = Vector2.dot(first, axis);
        double min = firstDot;
        double max = firstDot;

        for (final Vector2 point : this.vertices) {
            final double dot = Vector2.dot(point, axis);
            min = Math.min(min, dot);
            max = Math.max(max, dot);
        }

        return Interval.create(min, max);
    }

    @Override
    public int hashCode() {
        return this.vertices.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        final ConvexPolygon other = (ConvexPolygon) obj;
        return this.vertices.equals(other.vertices);
    }

    @Override
    public String toString() {
        return this.vertices.toString();
    }
}
