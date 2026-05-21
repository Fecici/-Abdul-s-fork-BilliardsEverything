package billiards.codeseq;

import billiards.math.XYZ;

import javaslang.collection.Array;
import javaslang.control.Either;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import java.util.List;
import java.util.Optional;

// In Java, the type system isn't expressive enough to describe immutable
// or constant objects, so you deal with this by making immutability
// a feature of the API of the class. Aka, private members, and no
// methods that return a mutable reference to or mutate those members.
// As such, you have the common pattern of a mutable "builder" object,
// which you construct, and then you create an immutable object from
// the builder. In Rust and C++, this is unnecessary.

// You can't modify an IntList from the binding, but it could
// be modified by performing some other operation.
// But you can't modify an ImmutableIntList from anywhere.
// That is the difference. So use the first for function parameters,
// and the second (or MutableIntList) for variables.
// Use the interface when specifying the type, the specific
// class when creating the type

// We us an ImmutableIntList. It gives us a fixed length, immutable list.
// It doesn't have nice indexing, but that's stupid 'ol java for you

// A wrapper around a list of code numbers
// Fully canonicalized.
// No classification done here
// This class is immutable
public final class CodeSequence implements Comparable<CodeSequence> {
    public final ImmutableIntList codeNumbers;

    private CodeSequence(final IntList dirtyCodeNumbers) {
        this.codeNumbers = standardOrder(eliminateRepeaters(dirtyCodeNumbers));
    }

    public static Either<InvalidCodeSequence, CodeSequence> create(final IntList dirtyCodeNumbers) {
        final Optional<InvalidCodeSequence> invalid = validate(dirtyCodeNumbers);

        if (invalid.isPresent()) {
            final InvalidCodeSequence errorCode = invalid.get();
            return Either.left(errorCode);
        } else {
            final CodeSequence codeSequence = new CodeSequence(dirtyCodeNumbers);
            return Either.right(codeSequence);
        }
    }

    // subList of a primitive list is not yet implemented. Until then, this is a work around
    public static IntList subList(final IntList list, final int start, final int end) {
        final MutableIntList subList = new IntArrayList();
        for (int i = start; i < end; ++i) {
            final int elem = list.get(i);
            subList.add(elem);
        }

        return subList;
    }

    // split apart the list into sublists of length div
    // and then check if they are all the same
    private static boolean isRepeated(final IntList codeNumbers, final int subLength) {
        // the number of sublists to check
        final int numSublists = codeNumbers.size() / subLength;

        // final IntList first = codeNumbers.subList(0, subLength);
        final IntList first = subList(codeNumbers, 0, subLength);
        for (int i = 1; i < numSublists; i += 1) {
            // final IntList other = codeNumbers.subList(subLength * i, subLength * (i + 1));
            final IntList other = subList(codeNumbers, subLength * i, subLength * (i + 1));

            if (!other.equals(first)) {
                // one of the sublists is not repeated
                return false;
            }
        }

        return true;
    }

    /**
     * A code sequence is *legal* if it is some combination of the following patterns.
     * Here, `E` stands for even and `O` stands for odd.
     *
     * Eg. 1
     *
     *
     */
    private static final Array<String> legalPatterns =
        Array.of("EE", "OOO", "OEOE", "EOEO", "OOEOOE", "OEOOEO", "EOOEOO");

    /**
     * Check if a code sequence is valid.
     *
     * A *code sequence* is an `IntList`. It is *valid* if
     *   - it is nonempty
     *   - all its numbers are greater than zero
     *   - it is some combination of the legal patterns
     *
     * Note that there are two possible sources of ambiguity in the ordering of a code sequence.
     * First, every
     */
    private static Optional<InvalidCodeSequence> validate(final IntList dirtyCodeNumbers) {
        // Must be nonempty
        if (dirtyCodeNumbers.isEmpty()) {
            return Optional.of(InvalidCodeSequence.EMPTY);
        }

        // All numbers must be strictly positive
        final boolean allPos = dirtyCodeNumbers.allSatisfy(num -> num > 0);
        if (!allPos) {
            return Optional.of(InvalidCodeSequence.NEGATIVE_OR_ZERO_NUMBERS);
        }

        // Check if it is a combination of the legal patterns
        final boolean legal = isLegal(dirtyCodeNumbers);

        if (!legal) {
            return Optional.of(InvalidCodeSequence.ILLEGAL_PATTERN);
        }

        // Otherwise, code sequence is valid
        return Optional.empty();
    }

    // codeNumbers must already be valid
    private static IntList eliminateRepeaters(final IntList codeNumbers) {
        final int n = codeNumbers.size();

        // Find the shortest repeated legal sublist.
        for (int i = 2; i < n; ++i) {
            if (n % i == 0) {
                if (isRepeated(codeNumbers, i)) {
                    // final IntList subList = codeNumbers.subList(0, i);
                    final IntList subList = subList(codeNumbers, 0, i);

                    if (isLegal(subList)) {
                        return subList;
                    }
                }
            }
        }

        // Otherwise, there are no repeaters, so just return the code numbers
        return codeNumbers;
    }

    public static String evenOddSequence(final IntList codeNumbers) {
        final StringBuilder builder = new StringBuilder();
        codeNumbers.forEach(num -> {
            if (num % 2 == 0) {
                builder.append('E');
            } else {
                builder.append('O');
            }
        });

        return builder.toString();
    }

    public static String evenOddSequence(final List<Integer> codeNumbers) {
        // Perfect case for a map right here
        final StringBuilder builder = new StringBuilder();
        for (final int num : codeNumbers) {
            if (num % 2 == 0) {
                builder.append('E');
            } else {
                builder.append('O');
            }
        }

        return builder.toString();
    }

    private static XYZ nextAngle(final XYZ prev, final XYZ curr, final int number) {
        if (number % 2 == 0) {
            return prev;
        } else {
            return XYZ.otherAngle(prev, curr);
        }
    }

    private static boolean isLegal(final IntList codeNumbers) {

        XYZ prev = XYZ.X;
        XYZ curr = XYZ.Y;

        final int size = codeNumbers.size();

        for (int i = 0; i < size; ++i) {
            final int num = codeNumbers.get(i);

            final XYZ next = nextAngle(prev, curr, num);

            prev = curr;
            curr = next;
        }

        return prev == XYZ.X && curr == XYZ.Y;
    }

    // Specify this as an IntList rather than MutableIntList,
    // because we don't want to modify it through this binding
    // This is a very beatiful API

    /**
     *
     */
    // we need to look at all rotations and reflections
    private static ImmutableIntList standardOrder(final IntList codeNumbers) {
        final MutableIntList mutableCopy = IntArrayList.newList(codeNumbers);

        final int length = mutableCopy.size();

        // We rotate and reverse `mutableCopy`, which would change `min` too if it were
        // just a reference. To prevent this, `min` is an immutable copy
        ImmutableIntList min = mutableCopy.toImmutable();

        // Rotate length times
        for (int i = 0; i < length; ++i) {
            rotateLeft(mutableCopy);

            // if mutableCopy < min
            if (compareIntList(mutableCopy, min) < 0) {
                min = mutableCopy.toImmutable();
            }
        }

        // After length rotations, the list is now back
        // to where it was before. Rotate it, and do it again

        mutableCopy.reverseThis();

        for (int i = 0; i < length; ++i) {
            rotateLeft(mutableCopy);

            // if mutableCopy < min
            if (compareIntList(mutableCopy, min) < 0) {
                min = mutableCopy.toImmutable();
            }
        }

        return min;
    }

    public static int compareIntList(final IntList list1, final IntList list2) {
        final int lengthComp = Integer.compare(list1.size(), list2.size());

        if (lengthComp < 0) {
            // list1.size() < list2.size()
            return -1;
        } else if (lengthComp > 0) {
            // list1.size() > list2.size()
            return 1;
        } else {
            // sizes are the same

            for (int i = 0; i < list1.size(); ++i) {
                final int elem1 = list1.get(i);
                final int elem2 = list2.get(i);

                final int comp = Integer.compare(elem1, elem2);

                if (comp < 0) {
                    return -1;
                } else if (comp > 0) {
                    return 1;
                }

                // else comp == 0, so check the next element
            }

            return 0;
        }
    }

    // Rotate a list left by one spot
    // Test this method!
    public static void rotateLeft(final MutableIntList list) {
        if (list.notEmpty()) {
            final int first = list.get(0);
            for (int i = 1; i < list.size(); ++i) {
                final int elem = list.get(i);
                list.set(i - 1, elem);
            }

            list.set(list.size() - 1, first);
        }
    }

    @Override
    public String toString() {
        return this.codeNumbers.makeString(" ");
    }

    @Override
    public int hashCode() {
        return this.codeNumbers.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        final CodeSequence other = (CodeSequence) obj;
        return this.codeNumbers.equals(other.codeNumbers);
    }

    @Override
    public int compareTo(final CodeSequence other) {
        return compareIntList(this.codeNumbers, other.codeNumbers);
    }
}
