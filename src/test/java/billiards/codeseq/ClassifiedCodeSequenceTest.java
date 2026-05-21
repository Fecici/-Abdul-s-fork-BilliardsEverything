package billiards.codeseq;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.Array;

import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

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
}
