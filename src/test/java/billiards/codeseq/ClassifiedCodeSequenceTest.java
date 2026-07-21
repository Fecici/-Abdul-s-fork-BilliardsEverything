package billiards.codeseq;

import billiards.viewer.Utils;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.Array;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Optional;

public final class ClassifiedCodeSequenceTest {
    @Test
    public static void testCodeType() {
        final Array<Tuple2<IntList, CodeType>> classifications = Array.of(
            Tuple.of(IntArrayList.newListWith(1, 1, 1), CodeType.OSO),
            Tuple.of(IntArrayList.newListWith(2, 2), CodeType.CNS),
            Tuple.of(IntArrayList.newListWith(1, 1, 2, 1, 3, 2), CodeType.ONS),
            Tuple.of(IntArrayList.newListWith(1, 1, 1, 1, 2, 1, 1, 1, 1, 2), CodeType.CS),
            Tuple.of(IntArrayList.newListWith(1, 1, 2, 2, 1, 1, 3, 3), CodeType.OSNO));

        for (final Tuple2<IntList, CodeType> pair : classifications) {
            final ClassifiedCodeSequence codeSeq = ClassifiedCodeSequence.create(pair._1).get();
            Assertions.assertEquals(codeSeq.codeType, pair._2);
        }
    }

    @Test
    public void testReportedLongOsnoCalculateInput() {
        // abdul 20/07/2026 Keep the reported top-Calculate OSNO input as a database-free parser and classifier regression.
        final String input = "OSNO (108, 1168) "
                + "1 5 22 4 22 4 20 4 22 4 20 4 20 2 4 2 16 4 26 4 14 2 10 4 31 1 5 24 4 20 4 20 4 24 "
                + "5 1 31 4 10 2 14 4 26 4 16 2 4 2 20 4 20 4 22 4 20 4 22 4 22 5 1 30 1 6 1 25 4 16 2 6 2 "
                + "16 4 26 5 1 34 1 5 28 4 14 4 28 4 14 4 28 4 14 4 28 4 12 2 12 2 8 2 12 2 10 2 12 4 30 6 31";

        final Optional<ImmutableIntList> parsed = Utils.splitString(Utils.tripleTrimmer(input));
        Assertions.assertTrue(parsed.isPresent());

        final ClassifiedCodeSequence codeSeq = ClassifiedCodeSequence.create(parsed.get()).get();
        Assertions.assertEquals(108, codeSeq.codeLength);
        Assertions.assertEquals(1168, codeSeq.codeSum);
        Assertions.assertEquals(CodeType.OSNO, codeSeq.codeType);
    }
}
