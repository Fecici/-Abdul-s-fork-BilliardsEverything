package billiards.math;

public final class CoverSquare {

    public final long numerX;
    public final long numerY;
    public final byte denom;

    // the value is to be interpreted as num / 2^denom
    private CoverSquare(final long numerX, final long numerY, final byte denom) {
        this.numerX = numerX;
        this.numerY = numerY;
        this.denom = denom;
    }

    // initialize (1/2, 1/2) as our center
    public static CoverSquare initial() {
        return new CoverSquare(1, 1, (byte) 1);
    }

    public CoverSquare[] subdivide() {
        // order of return:
        // | 1 2 |
        // | 3 4 |
        final byte newDenom = (byte) (denom + 1);
        final long numXLeft = (2 * numerX) - 1;
        final long numXRight = (2 * numerX) + 1;
        final long numYLower = (2 * numerY) - 1;
        final long numYUpper = (2 * numerY) + 1;
        final CoverSquare[] fracs =
            {new CoverSquare(numXLeft, numYUpper, newDenom),
             new CoverSquare(numXRight, numYUpper, newDenom),
             new CoverSquare(numXLeft, numYLower, newDenom),
             new CoverSquare(numXRight, numYLower, newDenom)};
        return fracs;
    }

    public double radius() {
        return 1.0 / (1 << denom);
    }

    // return is { Xmin, Xmax, Ymin, Ymax} !
    public double[] bounds() {
        final double rad = (Math.PI / 2) * this.radius();
        final double centerX = rad * numerX;
        final double centerY = rad * numerY;
        final double[] bounds = {centerX - rad, centerX + rad, centerY - rad, centerY + rad};
        return bounds;
    }

    public double[] center() {
        final double rad = (Math.PI / 2) * this.radius();
        final double[] center = {rad * numerX, rad * numerY};
        return center;
    }

    public boolean contains(final double x, final double y) {
        final double[] b = this.bounds();
        return b[0] < x && x < b[1] && b[2] < y && y < b[3];
    }

    @Override
    public String toString() {
        return "(" + numerX + "," + numerY + ") / " + denom;
    }
}
