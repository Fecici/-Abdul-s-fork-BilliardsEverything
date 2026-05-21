package billiards.vary;

import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.math3.util.FastMath;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.geometry.TriangleBilliard;
import billiards.viewer.SideSum;
import billiards.wrapper.Wrapper;

import static billiards.vary.VaryCS.doneIteration;

public final class Vary3 {
    private static final double OFFSET = 0.05;

	// Zhao Yu Li, Jul 11, 2025.
	// Replaced recursion with a while loop.
	private static void iterateFireAway(
			final int min, final int max, final double initPosition,
			final SideSum sideSum, final TriangleBilliard billiard, final MutableIntList code,
			final MutableList<ClassifiedCodeSequence> codesFound) {

		IntArrayList sideSumArray = new IntArrayList();
		DoubleArrayList specMinArray = new DoubleArrayList();
		specMinArray.add(0);

		DoubleArrayList specMaxArray = new DoubleArrayList();
		specMaxArray.add(Math.PI);

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
			TriangleBilliard currentBilliard = billiards.get(billiards.size() - 1);

			if (iterationDepth > min && !currentLeft && !currentRight) {
				// here we check if we have reached a periodic path

				if (Math.abs(sideSum.sum()) < OFFSET && currentBilliard.side == 2 && currentBilliard.orient == 1) {
					final double perfectAngle = FastMath.atan2(currentBilliard.vertexA.y, currentBilliard.vertexA.x + initPosition);

					if (specMaxArray.getLast() > perfectAngle && perfectAngle > specMinArray.getLast()) {
						final Optional<ClassifiedCodeSequence> codeSeq = Convert.convert(code);
                        codeSeq.ifPresent(codesFound::add);
					}
				}
			}

			final double specialAngle = currentBilliard.getSpecialAngle();

			if (!currentLeft && !currentRight) {
				if (specMaxArray.getLast() > specialAngle) {
					// go left
					final TriangleBilliard leftBilliard = currentBilliard.getNext(true);
					final int leftSwap = 3 - currentBilliard.side - leftBilliard.side;

					sideSum.add(leftSwap);
					sideSumArray.add(leftSwap);
					code.add(leftSwap);

					specMinArray.add(Math.max(specialAngle, specMinArray.getLast()));
					specMaxArray.add(specMaxArray.getLast());
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
				if (specMinArray.getLast() < specialAngle) {
					// go right
					final TriangleBilliard rightBilliard = currentBilliard.getNext(false);
					final int rightSwap = 3 - currentBilliard.side - rightBilliard.side;

					sideSum.sub(rightSwap);
					sideSumArray.add(rightSwap);
					code.add(rightSwap);

					specMinArray.add(specMinArray.getLast());
					specMaxArray.add(Math.min(specialAngle, specMaxArray.getLast()));
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
		}  // End of loop
	}

	public static MutableList<ClassifiedCodeSequence> fireAway(final int movesMin, final int movesMax,
            final double xAngle, final double yAngle, final double pos) {

        final MutableList<ClassifiedCodeSequence> codes = new FastList<>();
        final TriangleBilliard billiard = TriangleBilliard.create(xAngle, yAngle, pos);
        final SideSum sideSum = SideSum.create(xAngle, yAngle);
        final MutableIntList foundCode = new IntArrayList();

        iterateFireAway(movesMin, movesMax, pos, sideSum, billiard, foundCode, codes);
		return codes;
	}

		    /* jul,31,2025 Marco Mai
     * move vary3 to backend, rest of the code here being disable
	 * passing codetype to the backend
     */
	public static MutableList<ClassifiedCodeSequence> fireAway(final int movesMin, final int movesMax,
            final double xAngle, final double yAngle, final double pos,final String reqTypes) {
		
		Optional<MutableList<ClassifiedCodeSequence>> values = Wrapper.vary3Cpp( movesMin, movesMax,pos, xAngle,yAngle,reqTypes);
		if(!values.isPresent()){
			final MutableList<ClassifiedCodeSequence> codes = new FastList<>();
			return codes;
		} else {
			MutableList<ClassifiedCodeSequence> codes = values.get();
			return codes;
		}

	}

}
