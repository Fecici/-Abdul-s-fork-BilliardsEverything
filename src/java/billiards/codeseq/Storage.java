package billiards.codeseq;

import billiards.geometry.ConvexPolygon;
import billiards.geometry.LineSegment;
import billiards.geometry.Rectangle;
import billiards.math.Equation;
import billiards.math.LinCom;
import billiards.math.XYPi;

import org.apache.commons.math3.util.Precision;
import org.eclipse.collections.api.list.ImmutableList;

// TODO remove this ADT Storage.Stable stuff, and just have a CompStable and
// CompUnstable (or something like that) classes. It is useful to have these
// classes (with types giving structure to our data). It in fact might be nice
// to have some kind of Map<ClassifiedCodeSequence, Storage> or something,
// or maybe have two of them? I don't know

// This problem naturally breaks down into stable and unstable code sequences
// Writing these as sum types is the natural way of dealing with this.
// Unfortunately, Java doesn't have sum types. So we have two options:
// If you squint really hard, stable and unstable sequences sorta have the
// same structure, though there will be some duplication between them. (Eg,
// you could write the two boundary poinst for an unstable sequence in a list,
// and just put in the coefficients for a stable sequence in anyway, even though
// they are all zero). Or, we could model things properly (the way they should
// be modeled, with two different types), and hack Java to make them work properly.
// I will go for the second. It won't be that ugly, and we should do things the
// more correct but slightly more work way.

// This only stores information that we will need later in the program
public abstract class Storage implements Comparable<Storage> {
    public final ClassifiedCodeSequence classCodeSeq;
    private final ImmutableList<Equation> equations;
    public final String points;
    // private constructor ensures we can only subclass from within the class
    private Storage(final ClassifiedCodeSequence classCodeSeq, final ImmutableList<Equation> equations) {
        this.classCodeSeq = classCodeSeq;
        this.equations = equations;
        this.points="";
    }
    private Storage(final ClassifiedCodeSequence classCodeSeq, final ImmutableList<Equation> equations,final String points) {
        this.classCodeSeq = classCodeSeq;
        this.equations = equations;
        this.points=points;
    }



    public static final class Stable extends Storage {
        public final ConvexPolygon polygon;


        public Stable(final ClassifiedCodeSequence classCodeSeq,
                      final ImmutableList<Equation> equations,
                      final ConvexPolygon polygon) {
            super(classCodeSeq, equations);
            this.polygon = polygon;

        }
        public Stable(final ClassifiedCodeSequence classCodeSeq,
                      final ImmutableList<Equation> equations,
                      final ConvexPolygon polygon,final String points) {
            super(classCodeSeq, equations,points);
            this.polygon = polygon;

        }


        @Override
        public boolean intersects(final Rectangle rect) {
            return this.polygon.intersects(rect);
        }

        @Override
        public boolean intersects(final ConvexPolygon rect) {
            return this.polygon.intersects(rect);
        }

        @Override
        public double getMinX() {
            return this.polygon.vertices.collectDouble(point -> point.x).min();
        }

        @Override
        public double getMaxX() {
            return this.polygon.vertices.collectDouble(point -> point.x).max();
        }

        @Override
        public double getMinY() {
            return this.polygon.vertices.collectDouble(point -> point.y).min();
        }

        @Override
        public double getMaxY() {
            return this.polygon.vertices.collectDouble(point -> point.y).max();
        }
    }

    public static final class Unstable extends Storage {

        public final LinCom<XYPi> constraint;
        public final LineSegment lineSegment;

        public Unstable(final ClassifiedCodeSequence classCodeSeq, final ImmutableList<Equation> equations,
                        final LinCom<XYPi> constraint, final LineSegment lineSegment) {
            super(classCodeSeq, equations);
            this.constraint = constraint;
            this.lineSegment = lineSegment;
        }
        public Unstable(final ClassifiedCodeSequence classCodeSeq, final ImmutableList<Equation> equations,
                        final LinCom<XYPi> constraint, final LineSegment lineSegment,final String points) {
            super(classCodeSeq, equations,points);
            this.constraint = constraint;
            this.lineSegment = lineSegment;
        }


        @Override
        public boolean intersects(final Rectangle rect) {
            return this.lineSegment.intersects(rect);
        }

        @Override
        public boolean intersects(final ConvexPolygon rect) {
            return this.lineSegment.intersects(rect);
        }

        @Override
        public double getMinX() {
            return Math.min(this.lineSegment.start.x, this.lineSegment.end.x);
        }

        @Override
        public double getMaxX() {
            return Math.max(this.lineSegment.start.x, this.lineSegment.end.x);
        }

        @Override
        public double getMinY() {
            return Math.min(this.lineSegment.start.y, this.lineSegment.end.y);
        }

        @Override
        public double getMaxY() {
            return Math.max(this.lineSegment.start.y, this.lineSegment.end.y);
        }
    }

    public abstract boolean intersects(final Rectangle rect);
    public abstract boolean intersects(final ConvexPolygon rect);
    public abstract double getMinX();
    public abstract double getMaxX();
    public abstract double getMinY();
    public abstract double getMaxY();

    public boolean isPositive(final double rx, final double ry) {
        for (final Equation equation : this.equations) {
            final double result = equation.evalf(rx, ry);
            // TODO replace this with something simpler
            if (Precision.compareTo(result, 0, 1e-14) < 0) {
                return false;
            }
        }

        return true;
    }

    public boolean isPositiveProver(
        final double rx, final double ry, final double halfWidth, final double offset) {
        for (final Equation equation : this.equations) {
            final double result = equation.evalf(rx, ry);
            if (result - equation.bound * halfWidth <= offset) {
                return false;
            }
        }

        return true;
    }

    // All of these methods are implemented using the code sequence
    // so they are the same for the two subclasses
    public CodeType codeType() {
        return this.classCodeSeq.codeType;
    }

    public long codeLength() {
        return this.classCodeSeq.codeLength;
    }

    public long codeSum() {
        return this.classCodeSeq.codeSum;
    }

    public String oddEvenPattern() {
        return this.classCodeSeq.oddEvenPattern;
    }

    @Override
    public int compareTo(final Storage other) {
        return this.classCodeSeq.compareTo(other.classCodeSeq);
    }

    @Override
    public String toString() {
        return this.classCodeSeq.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        final Storage other = (Storage) obj;
        return this.classCodeSeq.equals(other.classCodeSeq);
    }

    @Override
    public int hashCode() {
        return this.classCodeSeq.hashCode();
    }
}
