package billiards.math;

import org.apache.commons.math3.util.ArithmeticUtils;

import java.util.Arrays;

public final class LinCom<E extends Enum<E>> {

    // We could use an EnumMap here, but an array avoids the overhead of boxing
    private final int[] coeffs;

    public LinCom(final int... coeffs) {
        this.coeffs = coeffs;
    }

    public int coeff(final E symbol) {
        final int index = symbol.ordinal();
        return this.coeffs[index];
    }

    public void add(final E symbol) {
        add(1, symbol);
    }

    public void sub(final E symbol) {
        sub(1, symbol);
    }

    public void add(final int coeff, final E symbol) {
        final int index = symbol.ordinal();
        this.coeffs[index] = Math.addExact(this.coeffs[index], coeff);
    }

    public void sub(final int coeff, final E symbol) {
        final int index = symbol.ordinal();
        this.coeffs[index] = Math.subtractExact(this.coeffs[index], coeff);
    }

    public void scale(final int s) {
        for (int i = 0; i < this.coeffs.length; ++i) {
            // coeffs[i] *= s
            this.coeffs[i] = Math.multiplyExact(s, this.coeffs[i]);
        }
    }

    public int unit() {
        for (final int coeff : coeffs) {
            if (coeff < 0) {
                return -1;
            } else if (coeff > 0) {
                return 1;
            }
        }

        // They are all zero, so return 0
        return 0;
    }

    public void normalizeUnit() {

        final int u = this.unit();
        // It is ok if we scale in the case of 0
        // because everything is 0
        this.scale(u);
    }

    public int content() {
        int gcd = 0;
        for (final int coeff : this.coeffs) {
            gcd = ArithmeticUtils.gcd(gcd, coeff);
        }

        // Always returns a positive number
        return gcd;
    }

    // This doesn't change the sign, since gcd > 0
    public void divideContent() {

        final int gcd = this.content();

        if (gcd != 0) {
            for (int i = 0; i < this.coeffs.length; ++i) {
                this.coeffs[i] = MathUtils.divideExact(this.coeffs[i], gcd);
            }
        }
    }

    public boolean isZero() {
        for (final int coeff : this.coeffs) {
            if (coeff != 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return Arrays.toString(this.coeffs);
    }

    /*
    @Override
    public String toString() {

        if (this.isZero()) {
            return "0";
        }

        final StringBuilder builder = new StringBuilder();

        boolean front = true;

        for (int i = 0; i < this.coeffs.length; ++i) {
            final E symbol = E.symbol(i);
            final int coeff = this.coeffs[i];

            // add a positive sign if it is positive and not at the front
            if ((coeff > 0) && !front) {
                builder.append('+');
            }

            if (coeff == -1) {
                builder.append('-');
            } else if ((coeff != 0) && (coeff != 1)) {
                builder.append(coeff);
            }

            // if coeff == 0, we ignore it
            // otherwise, we << it and set front to false
            if (coeff != 0) {
                builder.append(symbol);
                front = false;
            }
        }

        return builder.toString();
    }
*/
}
