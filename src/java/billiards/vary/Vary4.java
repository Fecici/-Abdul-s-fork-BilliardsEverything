package billiards.vary;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.math3.util.FastMath;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import com.google.common.collect.Lists;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.geometry.TriangleBilliard4;
import billiards.viewer.SideSum;
import billiards.viewer.Utils;
import javaslang.Tuple;
import javaslang.Tuple3;
import javaslang.collection.Array;
import billiards.wrapper.Wrapper;

public class Vary4 {
private static final double OFFSET = 0.05;
	// Zhao Yu Li, Jul 11, 2025.
	// Replaced recursion with a while loop.
	private static void iterateFireAway(
			final int min, final int max, final int depth, final SideSum sideSum, final TriangleBilliard4 billiard, 
			final MutableIntList code, final MutableList<ClassifiedCodeSequence> codesFound) {
		IntArrayList sideSumArray = new IntArrayList();

		BooleanArrayList leftArray = new BooleanArrayList();
		leftArray.add(false);

		BooleanArrayList rightArray = new BooleanArrayList();
		rightArray.add(false);

		ArrayList<TriangleBilliard4> billiards = new ArrayList<>();
		billiards.add(billiard);

		int iterationDepth = 0;

		while (true) {
			if (iterationDepth >= max || Thread.currentThread().isInterrupted()) {
				iterationDepth = doneIteration(code, sideSumArray, leftArray, rightArray, billiards, iterationDepth, sideSum);
				continue;
			}

			boolean currentLeft = leftArray.getLast();
			boolean currentRight = rightArray.getLast();
			final TriangleBilliard4 currentBilliard = billiards.get(billiards.size() - 1);

			if (iterationDepth > min && !currentLeft && !currentRight) {
				// here we check if we have reached a periodic path
				if (Math.abs(sideSum.sum()) < OFFSET && currentBilliard.side == 2 && currentBilliard.orient == 1) {
					final double perfectAngle = FastMath.atan2(currentBilliard.vertexA.y, currentBilliard.vertexA.x);

					if (currentBilliard.between(perfectAngle)) {
						final Optional<ClassifiedCodeSequence> codeSeq = Utils.convert(code);

						codeSeq.ifPresent(codesFound::add);
					}
				}
			}

			final Optional<TriangleBilliard4> optLeftBilliard = currentBilliard.getNext(true);

			if (!currentLeft && !currentRight) {
				if (optLeftBilliard.isPresent()) {
					// go left
					final TriangleBilliard4 leftBilliard = optLeftBilliard.get();
		    		final int leftSwap = 3 - currentBilliard.side - leftBilliard.side;

					sideSum.add(leftSwap);
					sideSumArray.add(leftSwap);
					code.add(leftSwap);

					leftArray.add(false);
					rightArray.add(false);
					billiards.add(leftBilliard);

					iterationDepth++;
				} else {
					currentLeft = true;
					leftArray.set(leftArray.size() - 1, true);
				}
			}

			final Optional<TriangleBilliard4> optRightBilliard = currentBilliard.getNext(false);

			if (currentLeft && !currentRight) {
				if (optRightBilliard.isPresent()) {
					// go right
					final TriangleBilliard4 rightBilliard = optRightBilliard.get();
					final int rightSwap = 3 - currentBilliard.side - rightBilliard.side;

					sideSum.sub(rightSwap);
					sideSumArray.add(rightSwap);
					code.add(rightSwap);

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
				iterationDepth = doneIteration(code, sideSumArray, leftArray, rightArray, billiards, iterationDepth, sideSum);
			}

			if (leftArray.isEmpty() && rightArray.isEmpty()) break;
		}
	}
	
	// this is a version of recurseFireAway meant to be done with a small maxDepth. It will return
	// all triangles which are exactly maxDepth moves away from the base triangle.
	private static MutableList<Tuple3<TriangleBilliard4, MutableIntList, SideSum>> makeStarts(
			final TriangleBilliard4 billiard, final int depth, final int maxDepth,
			final MutableIntList code, final SideSum sideSum) {
		IntArrayList sideSumArray = new IntArrayList();

		BooleanArrayList leftArray = new BooleanArrayList();
		leftArray.add(false);

		BooleanArrayList rightArray = new BooleanArrayList();
		rightArray.add(false);

		ArrayList<TriangleBilliard4> billiards = new ArrayList<>();
		billiards.add(billiard);

		int iterationDepth = 0;

		MutableList<Tuple3<TriangleBilliard4, MutableIntList, SideSum>> starts = new FastList<>();

		while (true) {
			final TriangleBilliard4 currentBilliard = billiards.get(billiards.size() - 1);

			if (iterationDepth >= maxDepth || Thread.currentThread().isInterrupted()) {
				final MutableList<Tuple3<TriangleBilliard4, MutableIntList, SideSum>> list = new FastList<>();
				final MutableIntList codeClone = IntArrayList.newList(code);
				final SideSum sumClone = sideSum.copy();
				list.add(Tuple.of(currentBilliard, codeClone, sumClone));
				starts.addAll(list);

				iterationDepth = doneIteration(code, sideSumArray, leftArray, rightArray, billiards, iterationDepth, sideSum);
				continue;
			}

			boolean currentLeft = leftArray.getLast();
			boolean currentRight = rightArray.getLast();

			final Optional<TriangleBilliard4> optLeftBilliard = currentBilliard.getNext(true);

			if (!currentLeft && !currentRight) {
				if (optLeftBilliard.isPresent()) {
					// go left
					final TriangleBilliard4 leftBilliard = optLeftBilliard.get();
					final int leftSwap = 3 - currentBilliard.side - leftBilliard.side;

					sideSum.add(leftSwap);
					sideSumArray.add(leftSwap);
					code.add(leftSwap);

					leftArray.add(false);
					rightArray.add(false);
					billiards.add(leftBilliard);

					iterationDepth++;
				} else {
					currentLeft = true;
					leftArray.set(leftArray.size() - 1, true);
				}
			}

			final Optional<TriangleBilliard4> optRightBilliard = currentBilliard.getNext(false);

			if (currentLeft && !currentRight) {
				if (optRightBilliard.isPresent()) {
					// go right
					final TriangleBilliard4 rightBilliard = optRightBilliard.get();
					final int rightSwap = 3 - currentBilliard.side - rightBilliard.side;

					sideSum.sub(rightSwap);
					sideSumArray.add(rightSwap);
					code.add(rightSwap);

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
				final MutableList<Tuple3<TriangleBilliard4, MutableIntList, SideSum>> list = new FastList<>();
				final MutableIntList codeClone = IntArrayList.newList(code);
				final SideSum sumClone = sideSum.copy();
				list.add(Tuple.of(currentBilliard, codeClone, sumClone));
				starts.addAll(list);

				iterationDepth = doneIteration(code, sideSumArray, leftArray, rightArray, billiards, iterationDepth, sideSum);
			}

			if (leftArray.isEmpty() && rightArray.isEmpty()) return starts;
		}
	}

	public static MutableList<ClassifiedCodeSequence> fireAway(final int movesMin, 
			final int movesMax, final double xAngle, final double yAngle) {
		final int numThreads = Utils.numThreads;
		
		final TriangleBilliard4 startBilliard = TriangleBilliard4.create(xAngle, yAngle);
		final SideSum sideSum = SideSum.create(xAngle, yAngle);
		final MutableIntList startCode = new IntArrayList();
		
        final Array<Tuple3<TriangleBilliard4, MutableIntList, SideSum>> starts = 
        		Array.ofAll(makeStarts(startBilliard, 0, numThreads, startCode, sideSum));
        
        final Array<Tuple3<TriangleBilliard4, MutableIntList, SideSum>> sortStarts = lazySort(starts);

        final Array<Callable<MutableList<ClassifiedCodeSequence>>> tasks = sortStarts.map(T -> () -> {
        	final MutableList<ClassifiedCodeSequence> codes = new FastList<>();
			
			iterateFireAway(movesMin, movesMax, numThreads, T._3, T._1, T._2, codes);
			return codes;
        });
        final MutableList<ClassifiedCodeSequence> allCodes = new FastList<ClassifiedCodeSequence>();
        
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final Array<Future<MutableList<ClassifiedCodeSequence>>> futures =
    			tasks.map(executor::submit);
        
        for (final Future<MutableList<ClassifiedCodeSequence>> future : futures) {
        	try {
				allCodes.addAll(future.get());
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
        }
        
        executor.shutdown();

		return allCodes;
	}
	
	// there must be a way to just sort an Array, but I can't find it, so here we are.
	static Array<Tuple3<TriangleBilliard4, MutableIntList, SideSum>> lazySort(
            final Array<Tuple3<TriangleBilliard4, MutableIntList, SideSum>> array) {
		
		final List<Tuple3<TriangleBilliard4, MutableIntList, SideSum>> list = Lists.newArrayList(array);
		
		list.sort(Comparator.comparingDouble(o -> o._1.interval()));

        return Array.ofAll(list);
	}

	public static int doneIteration(MutableIntList code, IntArrayList sideSumArray, BooleanArrayList leftArray, BooleanArrayList rightArray, ArrayList<TriangleBilliard4> billiards, int iterationDepth, SideSum sideSum) {
		boolean sideSumArrayIsEmpty = sideSumArray.isEmpty();
		int sideSumDelta = sideSumArrayIsEmpty ? 0 : sideSumArray.getLast();

		removeLast(code, sideSumArray, leftArray, rightArray, billiards);

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

	public static void removeLast(MutableIntList code, IntArrayList sideSumArray, BooleanArrayList leftArray, BooleanArrayList rightArray, ArrayList<TriangleBilliard4> billiards) {
		if (!code.isEmpty()) code.removeAtIndex(code.size() - 1);
		if (!sideSumArray.isEmpty()) sideSumArray.removeAtIndex(sideSumArray.size() - 1);
		if (!leftArray.isEmpty()) leftArray.removeAtIndex(leftArray.size() - 1);
		if (!rightArray.isEmpty()) rightArray.removeAtIndex(rightArray.size() - 1);
		if (!billiards.isEmpty()) billiards.remove(billiards.size() - 1);
	}

		/* jul,31,2025 Marco Mai
     * move vary3 to backend, rest of the code here being disable
	 * passing codetype to the backend
     */

	public static MutableList<ClassifiedCodeSequence> fireAway(final int movesMin, final int movesMax,
            final double xAngle, final double yAngle,final String reqTypes) {
		
		Optional<MutableList<ClassifiedCodeSequence>> values = Wrapper.vary4Cpp( movesMin, movesMax, xAngle,yAngle,reqTypes);
		if(!values.isPresent()){
			final MutableList<ClassifiedCodeSequence> codes = new FastList<>();
			return codes;
		} else {
			MutableList<ClassifiedCodeSequence> codes = values.get();
			return codes;
		}


        // final MutableList<ClassifiedCodeSequence> codes = new FastList<>();
        // final TriangleBilliard billiard = TriangleBilliard.create(xAngle, yAngle, pos);
        // final SideSum sideSum = SideSum.create(xAngle, yAngle);
        // final MutableIntList foundCode = new IntArrayList();
		// System.out.println("vray3");
        // recurseFireAway(movesMin, movesMax, 0, Math.PI, pos, 0, sideSum, billiard, foundCode, codes);
		// //fireAwayIterative(movesMin, movesMax, 0, Math.PI, pos, sideSum, billiard, codes);
		// return codes;
	}
}