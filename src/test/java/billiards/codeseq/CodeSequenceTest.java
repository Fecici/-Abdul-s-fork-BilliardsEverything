package billiards.codeseq;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.Array;

import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

public final class CodeSequenceTest {
    @Test
    public static void testEmptyCodeSequence() {
        final InvalidCodeSequence errorCode = CodeSequence.create(IntArrayList.newListWith()).getLeft();
        Assertions.assertEquals(errorCode, InvalidCodeSequence.EMPTY);
    }

    @Test
    public static void testNegativeCodeNumbers() {
        final Array<IntList> invalidCodeNumbers = Array.of(
            IntArrayList.newListWith(0),
            IntArrayList.newListWith(-1),
            IntArrayList.newListWith(0, 1),
            IntArrayList.newListWith(1, 2, 3, 0), IntArrayList.newListWith(-1, 2, -3, 4));

        for (final IntList codeNumbers : invalidCodeNumbers) {
            final InvalidCodeSequence errorCode = CodeSequence.create(codeNumbers).getLeft();
            Assertions.assertEquals(errorCode, InvalidCodeSequence.NEGATIVE_OR_ZERO_NUMBERS);
        }
    }

    @Test
    public static void testIllegalCodeSequences() {
        final List<IntList> illegalCodeSequences = new ArrayList<>();
        illegalCodeSequences.add(IntArrayList.newListWith(1)); // O
        illegalCodeSequences.add(IntArrayList.newListWith(2)); // E

        illegalCodeSequences.add(IntArrayList.newListWith(3, 5)); // OO
        illegalCodeSequences.add(IntArrayList.newListWith(1, 2)); // OE
        illegalCodeSequences.add(IntArrayList.newListWith(4, 7)); // EO

        illegalCodeSequences.add(IntArrayList.newListWith(1, 3, 8));    // OOE
        illegalCodeSequences.add(IntArrayList.newListWith(15, 4, 7));   // OEO
        illegalCodeSequences.add(IntArrayList.newListWith(32, 17, 81)); // EOO
        illegalCodeSequences.add(IntArrayList.newListWith(3, 12, 18));  // OEE
        illegalCodeSequences.add(IntArrayList.newListWith(8, 21, 78));  // EOE
        illegalCodeSequences.add(IntArrayList.newListWith(38, 52, 25)); // EEO
        illegalCodeSequences.add(IntArrayList.newListWith(2, 4, 8));    // EEE

        illegalCodeSequences.add(IntArrayList.newListWith(15, 37, 55, 21)); // OOOO
        illegalCodeSequences.add(IntArrayList.newListWith(15, 37, 55, 20)); // OOOE
        illegalCodeSequences.add(IntArrayList.newListWith(15, 37, 54, 21)); // OOEO
        illegalCodeSequences.add(IntArrayList.newListWith(15, 38, 55, 21)); // OEOO
        illegalCodeSequences.add(IntArrayList.newListWith(16, 37, 55, 21)); // EOOO

        // TODO finish the rest of the test cases

        for (final IntList codeNumbers : illegalCodeSequences) {
            final InvalidCodeSequence errorCode = CodeSequence.create(codeNumbers).getLeft();
            Assertions.assertEquals(errorCode, InvalidCodeSequence.ILLEGAL_PATTERN);
        }
    }

    @Test
    public static void testRepeaters() {
        final Array<Tuple2<IntList, IntList>> repeaters =
            Array.of(Tuple.of(IntArrayList.newListWith(1, 1, 1, 1, 1, 1),
                              IntArrayList.newListWith(1, 1, 1)),
                     Tuple.of(IntArrayList.newListWith(1, 1, 1, 1, 1, 1, 1, 1, 1),
                              IntArrayList.newListWith(1, 1, 1)),
                     Tuple.of(IntArrayList.newListWith(1, 1, 4, 1, 1, 4),
                              IntArrayList.newListWith(1, 1, 4, 1, 1, 4)));

        for (final Tuple2<IntList, IntList> pair : repeaters) {
            final CodeSequence codeSeq = CodeSequence.create(pair._1).get();
            Assertions.assertEquals(codeSeq.codeNumbers, pair._2);
        }
    }

    @Test
    public static void testOrder() {
        final Array<Tuple2<IntList, IntList>> codes = Array.of(
            Tuple.of(IntArrayList.newListWith(1, 1, 3), IntArrayList.newListWith(1, 1, 3)),
            Tuple.of(IntArrayList.newListWith(3, 1, 1), IntArrayList.newListWith(1, 1, 3)),
            Tuple.of(IntArrayList.newListWith(2, 4), IntArrayList.newListWith(2, 4)),
            Tuple.of(IntArrayList.newListWith(4, 2), IntArrayList.newListWith(2, 4)));

        for (final Tuple2<IntList, IntList> pair : codes) {
            final CodeSequence codeSeq = CodeSequence.create(pair._1).get();
            Assertions.assertEquals(codeSeq.codeNumbers, pair._2);
        }
    }
}
