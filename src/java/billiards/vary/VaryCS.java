package billiards.vary;

import java.util.ArrayList;
import java.util.Optional;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.geometry.TriangleBilliard;
import billiards.viewer.SideSum;
import billiards.viewer.Utils;
import billiards.wrapper.Wrapper;

public final class VaryCS {
    private static final double OFFSET = 0.0005;
    private static final double SMALLOFFSET = 0.0000000000005;

    // Zhao Yu Li, Jul 10, 2025.
    // Restructured the recursion into a while loop. Uses multiple ArrayLists to simulate the stack.
    private static void iterateFireAway(
            final int min, final int max, final double specMax, final SideSum sideSum,
            final TriangleBilliard billiard, final MutableIntList code,
            final MutableList<ClassifiedCodeSequence> codesFound) {

        IntArrayList sideSumArray = new IntArrayList();
        DoubleArrayList specMinArray = new DoubleArrayList();
        specMinArray.add(0);

        DoubleArrayList specMaxArray = new DoubleArrayList();
        specMaxArray.add(specMax);

        BooleanArrayList leftArray = new BooleanArrayList();
        leftArray.add(false);

        BooleanArrayList rightArray = new BooleanArrayList();
        rightArray.add(false);

        ArrayList<TriangleBilliard> billiards = new ArrayList<>();
        billiards.add(billiard);

        int iterationDepth = 0;

        while (true) {
            if (iterationDepth >= max || Thread.currentThread().isInterrupted()) {
                iterationDepth = doneIteration(code, sideSumArray, specMinArray, specMaxArray, leftArray, rightArray, billiards, iterationDepth, sideSum);
                continue;
            }

            boolean currentLeft = leftArray.getLast();
            boolean currentRight = rightArray.getLast();

            if (iterationDepth > min && !currentLeft && !currentRight) {
                // here we check if we have reached a periodic path

                if (Math.abs(sideSum.sum()) < OFFSET) {
                    // we do an additional check meant to reduce the number of empty sets due to
                    // rounding error. It is not very effective at large numbers of moves.
                    if (specMaxArray.getLast() - specMinArray.getLast() > SMALLOFFSET) {
                        final MutableIntList code2 = new IntArrayList();
                        code2.addAll(code);
                        code2.addAll(code.toReversed());
                        final Optional<ClassifiedCodeSequence> codeSeq = Utils.convert(code2);
                        codeSeq.ifPresent(codesFound::add);
                    }
                }
            }

            TriangleBilliard currentBilliard = billiards.get(billiards.size() - 1);
            final double specialPos = currentBilliard.vertexC.x;

            if (!currentLeft && !currentRight) {
                if (specMinArray.getLast() < specialPos) {
                    // go left
                    final TriangleBilliard leftBilliard = currentBilliard.getNext(true);
                    final int leftSwap = 3 - currentBilliard.side - leftBilliard.side;

                    sideSum.add(leftSwap);
                    sideSumArray.add(leftSwap);
                    code.add(leftSwap);

                    specMinArray.add(specMinArray.getLast());
                    specMaxArray.add(Math.min(specialPos, specMaxArray.getLast()));
                    leftArray.add(false);
                    rightArray.add(false);
                    billiards.add(leftBilliard);

                    iterationDepth++;
                } else {
                    currentLeft = true;
                    leftArray.set(leftArray.size() - 1, true);
                }
            }

            if (currentLeft && !currentRight) {
                if (specMaxArray.getLast() > specialPos) {
                    // go right
                    final TriangleBilliard rightBilliard = currentBilliard.getNext(false);
                    final int rightSwap = 3 - currentBilliard.side - rightBilliard.side;

                    sideSum.sub(rightSwap);
                    sideSumArray.add(rightSwap);
                    code.add(rightSwap);

                    specMinArray.add(Math.max(specialPos, specMinArray.getLast()));
                    specMaxArray.add(specMaxArray.getLast());
                    leftArray.add(false);
                    rightArray.add(false);
                    billiards.add(rightBilliard);

                    iterationDepth++;
                } else {
                    currentRight = true;
                    rightArray.set(rightArray.size() - 1, true);
                }
            }

            if (currentLeft && currentRight) {
                iterationDepth = doneIteration(code, sideSumArray, specMinArray, specMaxArray, leftArray, rightArray, billiards, iterationDepth, sideSum);
            }

            if (leftArray.isEmpty() && rightArray.isEmpty()) break;
        }
    }

    public static int doneIteration(MutableIntList code, IntArrayList sideSumArray, DoubleArrayList specMinArray, DoubleArrayList specMaxArray, BooleanArrayList leftArray, BooleanArrayList rightArray, ArrayList<TriangleBilliard> billiards, int iterationDepth, SideSum sideSum) {
        boolean sideSumArrayIsEmpty = sideSumArray.isEmpty();
        int sideSumDelta = sideSumArrayIsEmpty ? 0 : sideSumArray.getLast();

        removeLast(code, sideSumArray, specMinArray, specMaxArray, leftArray, rightArray, billiards);

        if (!leftArray.isEmpty() && !rightArray.isEmpty()) {
            if (!sideSumArrayIsEmpty) {
                if (!leftArray.getLast() && !rightArray.getLast()) sideSum.sub(sideSumDelta);
                else if (leftArray.getLast() && !rightArray.getLast()) sideSum.add(sideSumDelta);
            }

            if (!leftArray.getLast() && !rightArray.getLast()) leftArray.set(leftArray.size() - 1, true);
            else if (leftArray.getLast() && !rightArray.getLast()) rightArray.set(rightArray.size() - 1, true);
        }

        return iterationDepth - 1;
    }

    public static void removeLast(MutableIntList code, IntArrayList sideSumArray, DoubleArrayList specMinArray, DoubleArrayList specMaxArray, BooleanArrayList leftArray, BooleanArrayList rightArray, ArrayList<TriangleBilliard> billiards) {
        if (!code.isEmpty()) code.removeAtIndex(code.size() - 1);
        if (!sideSumArray.isEmpty()) sideSumArray.removeAtIndex(sideSumArray.size() - 1);
        if (!specMinArray.isEmpty()) specMinArray.removeAtIndex(specMinArray.size() - 1);
        if (!specMaxArray.isEmpty()) specMaxArray.removeAtIndex(specMaxArray.size() - 1);
        if (!leftArray.isEmpty()) leftArray.removeAtIndex(leftArray.size() - 1);
        if (!rightArray.isEmpty()) rightArray.removeAtIndex(rightArray.size() - 1);
        if (!billiards.isEmpty()) billiards.remove(billiards.size() - 1);
    }

    public static MutableList<ClassifiedCodeSequence> fireAway(final int movesMin, final int movesMax,
            final double xAngle, final double yAngle) {

		final MutableList<ClassifiedCodeSequence> codes = new FastList<>();
		final TriangleBilliard billiard = TriangleBilliard.create(xAngle, yAngle, 0);
		final SideSum sideSum = SideSum.create(xAngle, yAngle);
		final MutableIntList foundCode = new IntArrayList();
		
		iterateFireAway(movesMin/2, movesMax/2, billiard.vertexB.x, sideSum, billiard, foundCode, codes);

		return codes;
    }

            /* jul,31,2025 Marco Mai
     * move varyCS to backend, rest of the code here being disable
     */
    public static MutableList<ClassifiedCodeSequence> fireAway(final int movesMin, final int movesMax,
            final double xAngle, final double yAngle,final String reqTypes) {
		
		Optional<MutableList<ClassifiedCodeSequence>> values = Wrapper.varyCSCpp( movesMin, movesMax,xAngle,yAngle,reqTypes);
		if(!values.isPresent()){
			final MutableList<ClassifiedCodeSequence> codes = new FastList<>();
			return codes;
		} else {
			MutableList<ClassifiedCodeSequence> codes = values.get();
			return codes;
		}
		


    }
	
}
