package billiards.math;

import org.apache.commons.math3.util.FastMath;

public final class SinEquation extends Equation {

    public SinEquation(final int[] coeffs) {
        super(coeffs);
    }

    @Override
    public double evalf(final double x, final double y) {
        double sum = 0;

        for (int i = 0; i < this.coeffs.length; i += 3) {
            final double trigCoeff = this.coeffs[i];
            final double xCoeff = this.coeffs[i + 1];
            final double yCoeff = this.coeffs[i + 2];
            
          

            sum += trigCoeff * FastMath.sin(xCoeff * x + yCoeff * y);
        }

        return sum;
    }

    @Override
    public String toString() {
        return formatSum("sin");
    }
}
