package billiards.codeseq;

import billiards.math.XYZ;

import javaslang.control.Either;

import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Validates, canonicalizes, and classifies code sequences.
 */
public final class ClassifiedCodeSequence implements Comparable<ClassifiedCodeSequence> {
    public final CodeSequence codeSequence;
    public final long codeLength;
    public final long codeSum;
    public final CodeType codeType;
    public final boolean stable;
    public final String oddEvenPattern;

    private ClassifiedCodeSequence(final CodeSequence codeSequence) {
        this.codeSequence = codeSequence;
        this.codeLength = codeSequence.codeNumbers.size();
        this.codeSum = calculateCodeSum(codeSequence.codeNumbers);
        this.codeType = calculateCodeType(codeSequence.codeNumbers);
        this.stable = isStableCodeType(this.codeType);
        this.oddEvenPattern = calculateOddEvenPattern(codeSequence.codeNumbers);
    }

    public static Either<InvalidCodeSequence, ClassifiedCodeSequence> create(
        final IntList dirtyCodeNumbers) {
        final Either<InvalidCodeSequence, CodeSequence> either =
            CodeSequence.create(dirtyCodeNumbers);

        if (either.isLeft()) {
            final InvalidCodeSequence errorCode = either.getLeft();
            return Either.left(errorCode);
        } else {
            final CodeSequence codeSequence = either.get();
            final ClassifiedCodeSequence classCodeSeq = new ClassifiedCodeSequence(codeSequence);
            return Either.right(classCodeSeq);
        }
    }

    /**
     * Zhao Yu Li, May 06, 2025.
     * Calculates the odd-even pattern of a code sequence. The odd-even pattern is used to distinguished two code
     * sequence when they are of the same type and have the same code length. The odd-even pattern is stored as an
     * array of string, where each character in the string can be either an "O(dd)" or an "E(ven)". It is important to
     * note that using a String or StringBuilder to store the entire pattern is memory intensive, as each element can
     * take only two values, and each element is eight bytes. A more efficient way is to use a single bit, but the code
     * sequence can have up to a thousand numbers, and we don't have a integer type that big.
     * e.g. 1 1 368 1 1 739 <==> "OOEOOO"
     * @param codeNumbers The list of numbers that constitutes the code sequence
     * @return The odd-even pattern
     */
    private static String calculateOddEvenPattern(final IntList codeNumbers) {
        StringBuilder oddEvenPattern = new StringBuilder();

        for (int i = 0; i < codeNumbers.size(); i++) {
            if (codeNumbers.get(i) % 2 == 0) {
                oddEvenPattern.append("E");
            } else {
                oddEvenPattern.append("O");
            }
        }

        return oddEvenPattern.toString();
    }

    private static CodeType calculateCodeType(final IntList codeNumbers) {
        final boolean odd = isOdd(codeNumbers);
        final boolean closed = isClosed(codeNumbers);
        final boolean stable = isStable(codeNumbers);

        if (!closed && stable && odd) {
            return CodeType.OSO;
        } else if (!closed && stable && !odd) {
            return CodeType.OSNO;
        } else if (!closed && !stable) {
            return CodeType.ONS;
        } else if (closed && stable) {
            return CodeType.CS;
        } else if (closed && !stable) {
            return CodeType.CNS;
        } else {
            throw new RuntimeException(codeNumbers + " cannot be classified");
        }
    }

    private static long calculateCodeSum(final IntList codeNumbers) {
        long sum = 0;
        for (int i = 0; i < codeNumbers.size(); ++i) {
            final int codeNumber = codeNumbers.get(i);
            sum = Math.addExact(sum, codeNumber);
        }

        return sum;
    }

    public static boolean isStableCodeType(final CodeType codeType) {
        switch (codeType) {
        case OSO:
            return true;
        case OSNO:
            return true;
        case ONS:
            return false;
        case CS:
            return true;
        case CNS:
            return false;
        default:
            // Java does not check for exhaustiveness when switching on an enum
            throw new RuntimeException("unknown code type in isStableCodeType");
        }
    }

    /**
     * A code sequence is *odd* if the sum of its numbers is odd.
     *
     * Note that the sum of a code sequence is odd iff its length is odd. This is
     * because this property holds for all the legal patterns, and all legal code
     * sequences must be some combination of those patterns.
     */
    private static boolean isOdd(final IntList codeNumbers) {
        return codeNumbers.size() % 2 != 0;
    }

    /**
     * A code sequence is *closed* if it looks something like this:
     *
     * __ E ~~~~~ E ---
     *
     * where the E's are arbitrary even numbers, a = ~~~~~, b = ---__, and a == b.reverse().
     *
     * For example, this is a sum 28 closed sequence:
     *
     * 1 2 1 3 3 1 3 4 3 1 3 3
     * _ E ~ ~ ~ ~ ~ E - - - -
     *
     * a = 1 3 3 1 3
     * b = 3 1 3 3 1
     *
     * A code sequence that is not closed is called *open*.
     *
     * Note that all closed have even length (and so also even sum).
     *
     * This function determines if the given code sequence is closed.
     */
    private static boolean isClosed(final IntList codeNumbers) {
        // Odd code sequences are never closed
        if (isOdd(codeNumbers)) {
            return false;
        }

        final int length = codeNumbers.size();
        final int halfLength = codeNumbers.size() / 2;

        // Iterate over codeNumbers in intervals of halfLength, checking
        // to see if we find two even numbers.
        for (int i = 0; i < halfLength; i += 1) {
            final int first = codeNumbers.get(i);
            final int second = codeNumbers.get(i + halfLength);

            final boolean firstEven = first % 2 == 0;
            final boolean secondEven = second % 2 == 0;

            if (firstEven && secondEven) {
                // Now check if the two lists between the first and second integers are
                // reverses of each other.
                // We could have __ E ~~~~~ E ---, for example, and then
                // a = ~~~~~
                // b = ---__ (and then we reverse it)
                // final IntList a = codeNumbers.subList(i + 1, i + halfLength);
                final IntList a = CodeSequence.subList(codeNumbers, i + 1, i + halfLength);

                final MutableIntList b = new IntArrayList();
                // b.addAll(codeNumbers.subList(i + halfLength + 1, length));
                // b.addAll(codeNumbers.subList(0, i));
                b.addAll(CodeSequence.subList(codeNumbers, i + halfLength + 1, length));
                b.addAll(CodeSequence.subList(codeNumbers, 0, i));

                b.reverseThis();

                if (a.equals(b)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determine if the code sequence is stable.
     */
    // TODO replace this method with the constraint method
    private static boolean isStable(final IntList codeNumbers) {
        // odd sequences are stable
        if (codeNumbers.size() % 2 != 0) {
            return true;
        }

        final List<XYZ> codeAngles = new ArrayList<>();

        // we always start off with the first two
        codeAngles.add(XYZ.X);
        codeAngles.add(XYZ.Y);

        // the code sequence is already constructed, so we know it is valid
        for (int i = 1; i < codeNumbers.size() - 1; i += 1) {
            final int number = codeNumbers.get(i);
            if (number % 2 == 0) {
                // even number
                // second last one
                final XYZ prev = codeAngles.get(codeAngles.size() - 2);
                codeAngles.add(prev);
            } else {
                // odd number
                final XYZ one = codeAngles.get(codeAngles.size() - 1);
                final XYZ two = codeAngles.get(codeAngles.size() - 2);

                final XYZ other = XYZ.otherAngle(one, two);

                codeAngles.add(other);
            }
        }

        // use long just to be safe
        final EnumMap<XYZ, Long> coeffs = new EnumMap<>(XYZ.class);

        // init everything to zero
        coeffs.put(XYZ.X, 0L);
        coeffs.put(XYZ.Y, 0L);
        coeffs.put(XYZ.Z, 0L);

        for (int i = 0; i < codeNumbers.size(); i += 1) {
            final int codeNumber = codeNumbers.get(i);
            final XYZ codeAngle = codeAngles.get(i);

            final long oldCoeff = coeffs.get(codeAngle);

            // we need to take the alternating sum
            final long newCoeff;
            if (i % 2 == 0) {
                newCoeff = Math.addExact(oldCoeff, codeNumber);
            } else {
                newCoeff = Math.subtractExact(oldCoeff, codeNumber);
            }

            coeffs.put(codeAngle, newCoeff);
        }

        final long coeffA = coeffs.get(XYZ.X);
        final long coeffB = coeffs.get(XYZ.Y);
        final long coeffC = coeffs.get(XYZ.Z);

        final boolean stable = (coeffA == 0) && (coeffB == 0) && (coeffC == 0);

        return stable;
    }
    
    public int length() {
    	return this.codeSequence.codeNumbers.size();
    }
    
    @Override
    public String toString() {
        return this.codeSequence.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        // Strictly speaking, we should return false if obj is not an instance of CodeSequence, but
        // I don't care. Comparing equality with an object of a different type is always a bug and
        // has bitten me before.
        final ClassifiedCodeSequence other = (ClassifiedCodeSequence) obj;
        return this.codeSequence.equals(other.codeSequence);
    }

    @Override
    public int hashCode() {
        return this.codeSequence.hashCode();
    }

    @Override
    public int compareTo(final ClassifiedCodeSequence other) {
        return this.codeSequence.compareTo(other.codeSequence);
    }
}
