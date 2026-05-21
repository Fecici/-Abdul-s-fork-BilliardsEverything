package billiards.math;

public final class MathUtils {

    public static int absExact(final int val) {
        if (val == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return Math.abs(val);
    }

    public static int divideExact(final int left, final int right) {
        if (left == Integer.MIN_VALUE && right == -1) {
            throw new ArithmeticException("integer overflow");
        }

        return left / right;
    }
}
