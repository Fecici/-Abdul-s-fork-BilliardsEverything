package billiards.math;

import javaslang.Tuple;
import javaslang.Tuple2;

import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

public abstract class Equation {

    // The coeffs and bound are all ints, but the calculations are faster
    // if we save them as doubles. In Java, doing an int -> double conversion
    // is safe (but long -> double is not!)
    protected final double[] coeffs;
    public final double bound;

    protected Equation(final int[] intCoeffs) {
        this.coeffs = convertDouble(intCoeffs);
        this.bound = calculateBound(intCoeffs);
    }

    private static double[] convertDouble(final int[] intCoeffs) {
        final double[] doubleCoeffs = new double[intCoeffs.length];
        for (int i = 0; i < intCoeffs.length; ++i) {
            doubleCoeffs[i] = intCoeffs[i];
        }
        return doubleCoeffs;
    }

    private static int calculateBound(final int[] intCoeffs) {
        int bound = 0;

        for (int i = 0; i < intCoeffs.length; i += 3) {
            final int trigCoeff = MathUtils.absExact(intCoeffs[i]);
            final int xCoeff = MathUtils.absExact(intCoeffs[i + 1]);
            final int yCoeff = MathUtils.absExact(intCoeffs[i + 2]);

            final int varSum = Math.addExact(xCoeff, yCoeff);
            final int prod = Math.multiplyExact(trigCoeff, varSum);
            bound = Math.addExact(bound, prod);
        }

        return bound;
    }

    // so like "sin("
    // final int[] intCoeffs = parseArray(str, trig + '(');
    private static int[] parseArray(final String str, final String trig) {
        final MutableIntList builder = new IntArrayList();

        if (str.equals("0")) {
            // An empty list
            return builder.toArray();
        }

        int index = 0;
        int length = 0;
        for (int i = 0; i < str.length(); i += 1) {
            final char ch = str.charAt(i);
            if (ch == '-') {
                index = i;
                length = 1;
            } else if (ch == trig.charAt(0)) {
                final int trigCoeff = parseNumber(str.substring(index, index + length));

                // here is where the magic happens
                if (!str.substring(i, i + 4).equals(trig)) {
                    final String err = "\"" + str.substring(i, i + 4) + "\" != \"" + trig + "\" in \"" + str + "\" at position " + i;
                    throw new RuntimeException(err);
                }

                final int ind = str.indexOf(")", i);

                final Tuple2<Integer, Integer> tup = parseXY(str.substring(i + 4, ind));

                builder.add(trigCoeff);
                builder.add(tup._1);
                builder.add(tup._2);

                // now skip past the sin
                i = ind;
                // not parsing number any more
                length = 0;

            } else if (Character.isDigit(ch)) {
                if (length == 0) {
                    // we are not reading a number currently,
                    // so restart
                    index = i;
                }

                length += 1;

            } else if (ch == '+') {
                // the next character should be the start of an integer
                // 3*x+y
                index = i + 1;
                length = 0;

            } else if (ch != '*') {
                final String err =
                    "unrecognized character '" + ch + "' in \"" + str + "\" at position " + i;
                throw new RuntimeException(err);
            }
        }

        return builder.toArray();
    }

    private static Tuple2<Integer, Integer> parseXY(final String str) {
        int x = 0;
        int y = 0;

        if (str.equals("0")) {
            return Tuple.of(x, y);
        }

        int index = 0;
        int length = 0;
        for (int i = 0; i < str.length(); i += 1) {
            final char ch = str.charAt(i);
            if (ch == '-') {
                index = i;
                length = 1;
            } else if (ch == 'x') {
                x = parseNumber(str.substring(index, index + length));
                // not parsing number any more
                length = 0;

            } else if (ch == 'y') {
                y = parseNumber(str.substring(index, index + length));
                // not parsing number any more
                length = 0;

            } else if (Character.isDigit(ch)) {
                if (length == 0) {
                    // we are not reading a number currently,
                    // so restart
                    index = i;
                }

                length += 1;

            } else if (ch == '+') {
                // the next character should be the start of an integer
                // 3*x+y
                index = i + 1;
                length = 0;

            } else if (ch != '*') {
                final String err =
                    "unrecognized character '" + ch + "' in \"" + str + "\" at position " + i;
                throw new RuntimeException(err);
            }
        }

        return Tuple.of(x, y);
    }

    private static int parseNumber(final String num) {
        if (num.isEmpty()) {
            return 1;
        } else if (num.equals("-")) {
            return -1;
        } else {
            // The input string should match this regex: D+ U +D+ U -D+, where D is 0-9
            // If it doesn't, it should throw an exception
            return Integer.parseInt(num);
        }
    }

    protected String formatSum(final String trig) {

        final StringBuilder builder = new StringBuilder();

        if (this.coeffs.length == 0) {
            builder.append('0');
            return builder.toString();
        }

        // Set to false once we add the first coefficient
        boolean front = true;

        for (int i = 0; i < this.coeffs.length; i += 3) {
            final int trigCoeff = (int) this.coeffs[i];
            final int xCoeff = (int) this.coeffs[i + 1];
            final int yCoeff = (int) this.coeffs[i + 2];
            
            
          

            // add a positive sign if it is positive and not at the front
            if ((trigCoeff > 0) && !front) {
                builder.append('+');
            }

            if (trigCoeff == 1) {
                // do nothing
            } else if (trigCoeff == -1) {
                builder.append('-');
            } else if (trigCoeff != 0) {
                builder.append(trigCoeff);
            } else {
                throw new RuntimeException("zero trig coefficient in equation");
            }

            builder.append(trig).append('(');
            formatArg(builder, xCoeff, yCoeff);
            builder.append(')');

            front = false;
        }

        return builder.toString();
    }

    private static void formatArg(final StringBuilder builder, final int x, final int y) {

        if (x == 0 && y == 0) {
            builder.append('0');
            return;
        }

        // Set to false once we add the first coefficient
        boolean front = true;

        // add a positive sign if it is positive and not at the front
        if ((x > 0) && !front) {
            builder.append('+');
        }

        if (x == -1) {
            builder.append('-');
        } else if ((x != 0) && (x != 1)) {
            builder.append(x);
        }

        // if coeff == 0, we ignore it
        // otherwise, we << it and set front to false
        if (x != 0) {
            builder.append('x');
            front = false;
        }

        // Now do y
        if ((y > 0) && !front) {
            builder.append('+');
        }

        if (y == -1) {
            builder.append('-');
        } else if ((y != 0) && (y != 1)) {
            builder.append(y);
        }

        // if coeff == 0, we ignore it
        // otherwise, we << it and set front to false
        if (y != 0) {
            builder.append('y');
            front = false;
        }
    }

    public abstract double evalf(final double x, final double y);
}
