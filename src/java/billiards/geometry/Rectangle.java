package billiards.geometry;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.Objects;

public final class Rectangle implements Project {
    public final Interval intervalX;
    public final Interval intervalY;

    public boolean trimable;

    private Rectangle(final Interval intervalX, final Interval intervalY) {
        this.intervalX = intervalX;
        this.intervalY = intervalY;
        this.trimable = true;
    }

    // Not necessary that x0 <= x1 and y0 <= y1. It gets sorted automatically
    public static Rectangle create(
        final double x0, final double x1, final double y0, final double y1) {
        final Interval intervalX = Interval.create(x0, x1);
        final Interval intervalY = Interval.create(y0, y1);
        
        //george aug 26,2019 for 1 3 3
        //System.out.print("intervalX " + intervalX + "\n");
        //System.out.print("intervalY " + intervalY + "\n");8
        /* prints
           intervalX [-0.2, 3.290658503988659]
           intervalY [-0.2, 3.290658503988659]
         */


        return new Rectangle(intervalX, intervalY);
    }

    public void setTrimable(boolean trim) {
        this.trimable = trim;
    }

    public Vector2 center() {
        return Vector2.create(intervalX.center(), intervalY.center());
    }

    public boolean contains(final double x, final double y) {
        return this.intervalX.contains(x) && this.intervalY.contains(y);
    }

    // http://stackoverflow.com/questions/306316/determine-if-two-rectangles-overlap-each-other
    public static boolean intersects(final Rectangle a, final Rectangle b) {
        return a.intervalX.min <= b.intervalX.max && a.intervalX.max >= b.intervalX.min && a.intervalY.min <= b.intervalY.max && a.intervalY.max >= b.intervalY.min;
    }

    // In C++, where we have value types, I wouldn't worry about this, because
    // the optimizer would take care of the copies. But in Java, we have to
    // allocate memory
    @Override
    public Interval project(final Vector2 axis) {
        final Vector2 a = Vector2.create(this.intervalX.min, this.intervalY.min);

        final double aDot = Vector2.dot(a, axis);
        double min = aDot;
        double max = aDot;

        final Vector2 b = Vector2.create(this.intervalX.min, this.intervalY.max);

        final double bDot = Vector2.dot(b, axis);
        min = Math.min(min, bDot);
        max = Math.max(max, bDot);

        final Vector2 c = Vector2.create(this.intervalX.max, this.intervalY.max);

        final double cDot = Vector2.dot(c, axis);
        min = Math.min(min, cDot);
        max = Math.max(max, cDot);

        final Vector2 d = Vector2.create(this.intervalX.max, this.intervalY.min);

        final double dDot = Vector2.dot(d, axis);

        min = Math.min(min, dDot);
        max = Math.max(max, dDot);

        return Interval.create(min, max);
    }

    public Interval projectX() {
        return this.intervalX;
    }

    public Interval projectY() {
        return this.intervalY;
    }

    public ConvexPolygon toConvexPolygon() {
        final MutableList<Vector2> points = new FastList<>();
        points.add(Vector2.create(this.intervalX.min, this.intervalY.min));
        points.add(Vector2.create(this.intervalX.min, this.intervalY.max));
        points.add(Vector2.create(this.intervalX.max, this.intervalY.max));
        points.add(Vector2.create(this.intervalX.max, this.intervalY.min));
        return ConvexPolygon.create(points.toImmutable());
    }

    public MutableList<Rectangle> subdivide() {
        MutableList<Rectangle> quarters = new FastList<>();
        final Vector2 center = this.center();

        final Rectangle upperLeft = Rectangle.create(this.intervalX.min, center.x, center.y, this.intervalY.max);

        final Rectangle upperRight = Rectangle.create(center.x, this.intervalX.max, center.y, this.intervalY.max);

        final Rectangle lowerLeft = Rectangle.create(this.intervalX.min, center.x, this.intervalY.min, center.y);

        final Rectangle lowerRight = Rectangle.create(center.x, this.intervalX.max, this.intervalY.min, center.y);

        // This is UL, UR, LL, LR
        quarters.add(upperLeft);
        quarters.add(upperRight);
        quarters.add(lowerLeft);
        quarters.add(lowerRight);
        return quarters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.intervalX, this.intervalY);
        
    }

    @Override
    public boolean equals(final Object obj) {
        final Rectangle other = (Rectangle) obj;
        return this.intervalX.equals(other.intervalX) && this.intervalY.equals(other.intervalY);
    }

    @Override
    public String toString() {
        return this.intervalX + ", " + this.intervalY;
    }
}
