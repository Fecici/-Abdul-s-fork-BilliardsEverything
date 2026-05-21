package billiards.geometry;

import java.util.Objects;

// A sorted interval between two points a and b
// We assume this is an open interval, since our polygons are open sets
public final class Interval {
    public final double min;
    public final double max;

    private Interval(final double min, final double max) {
        this.min = min;
        this.max = max;
    }

    public static Interval create(final double a, final double b) {
        final double min = Math.min(a, b);
        final double max = Math.max(a, b);

        return new Interval(min, max);
    }

    public double center() {
        return (this.min + this.max) / 2.0;
    }

    public double length() {
        return this.max - this.min;
    }

    public boolean contains(final double x) {
        return this.min < x && x < this.max;
    }

    // http://stackoverflow.com/questions/3269434/whats-the-most-efficient-way-to-test-two-integer-ranges-for-overlap
    public static boolean intersects(final Interval a, final Interval b) {
        return a.min < b.max && b.min < a.max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.min, this.max);
    }

    @Override
    public boolean equals(final Object obj) {
        final Interval other = (Interval) obj;
        return this.min == other.min && this.max == other.max;
    }

    @Override
    public String toString() {
        return "[" + this.min + ", " + this.max + "]";
    }
}
