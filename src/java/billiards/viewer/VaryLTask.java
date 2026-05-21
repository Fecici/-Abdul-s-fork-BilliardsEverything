package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeType;
import billiards.codeseq.Storage;
import billiards.database.Database;
import billiards.geometry.Vector2;
import billiards.wrapper.ConnectionPool;

import javaslang.collection.Array;
import javaslang.control.Either;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.sorted.mutable.TreeSortedSet;
/*
PolyVaryTask encompasses the process of finding codes and calculating corresponding code regions.
Both these processes are multithreaded. Because of this, the task does not perform ui updates directly. Instead, you should access partialResults using getPartials() or getPartialProperty() and set up a change listener from the javafx application thread if you wish to provide ui updates during execution.

If only the final result is required, you can just call get() on this task after it finishes.
 * */
public final class VaryLTask extends Task<ObservableList<Storage>> {
    // Expose task property representing partial results
    private final ReadOnlyObjectWrapper<ObservableList<Storage>> partialResults =
            new ReadOnlyObjectWrapper<>(
                    this, 
                    "partialResults",
                    FXCollections.observableArrayList(
                            new ArrayList<Storage>()
                    )
            );
    private final Array<Vector2> coordList;
    private final MutableSortedSet<String> coverCodes = new TreeSortedSet<>();
    private final BoyanMenu boyanMenu;
    private final ConnectionPool pool;
    private final int CSmax;
    private final int OSOmax;
    private final int OSNOmax;
    private final int CSmaxSS;
    private final int OSOmaxSS;
    private final int OSNOmaxSS;
    private final boolean overrideSS;
    private final boolean draw;
    private final int maxPrint;
    private final ExecutorService storageExecutor;
    private final ExecutorService shotExecutor;
    private final boolean printMid;
    private final boolean firstLast;
    private final boolean addToAllPositive;
    private final boolean addToPlusMinus;
    private final IterateToLimitWindow iterateToLimitWindow;
    private final int idx;
    private final int step;
    private final int end;
    private final int codesFound;

    // Constructor takes a list of points to vary at
    public VaryLTask(
        final Array<Vector2> points, List<String> coverCodes, final BoyanMenu boyan,
        final Array<Integer> max, final ConnectionPool pool, final boolean override, final boolean draw,
        final Integer maxPrint, final ExecutorService eOne, final ExecutorService eTwo, final boolean printMid,
        final boolean firstLast, boolean addToAllPositive, boolean addToPlusMinus, IterateToLimitWindow iterateToLimitWindow,
        final int idx, final int step, final int end, final int codesFound) {
        this.coordList = points; // Points are in degrees
        this.coverCodes.addAll(coverCodes);
        this.boyanMenu = boyan;
        this.CSmax = max.get(0);
        this.OSOmax = max.get(1);
        this.OSNOmax = max.get(2);
        this.CSmaxSS = max.get(3);
        this.OSOmaxSS = max.get(4);
        this.OSNOmaxSS = max.get(5);
        this.pool = pool;
        this.overrideSS = override;
        this.draw = draw;
        this.maxPrint = maxPrint;
        this.storageExecutor = eOne;
        this.shotExecutor = eTwo;
        this.printMid = printMid;
        this.firstLast = firstLast;

        // Zhao Yu Li, Jun 24, 2025.
        // Variables for adding content to the IterateToLimitWindow Cover
        this.addToAllPositive = addToAllPositive;
        this.addToPlusMinus = addToPlusMinus;
        this.iterateToLimitWindow = iterateToLimitWindow;

        // Zhao Yu Li, Jun 27, 2025.
        this.idx = idx;
        this.step = step;
        this.end = end;
        this.codesFound = codesFound;
    }

    @Override
    protected ObservableList<Storage> call() {
        // storageExecutor handles the more expensive process of calculating code regions,
        // while shotExecutor handles the much faster calculation of finding the codes present at a given point

        final MutableSortedSet<ClassifiedCodeSequence> usedCodes = new TreeSortedSet<ClassifiedCodeSequence>();
        final MutableList<Future<Either<String, Storage>>> futures = new FastList<>();

        AtomicInteger progress = new AtomicInteger(); // Create an integer which supports non-locking concurrent operations
        final int todo = this.coordList.size() * maxPrint;
        this.updateProgress(0, todo);

        // The meat and potatoes. Finds codes sequentially, and submits them to the executer as they are found.
        // This is the most efficient way to implement varyL since each code can be calculated as soon as it's found, without interfering with the process of finding more codes.
        int totalCodes = 0;

        // Zhao Yu Li, Jun 27, 2025.
        // Removed for loop inside the task; we use a recursion of this task instead (similar to AustinMaxVary, which
        // uses PolyVaryTask). This is to facilitate moving the screen from one point to the next.
        Vector2 coord = this.coordList.get(idx);

        MutableSortedSet<ClassifiedCodeSequence> localCodes;
        System.out.println();
        System.out.println("//------------- working on point " + (idx + 1) + " -------------"); // george added // sept 27,2017
        // The BoyanCodes method vary3() called by varyTrianglesL() can throw exceptions. We need to catch them
        try {
            localCodes = autoCodesFiltered(coord, shotExecutor);
        } catch(RuntimeException e) {
            if(this.isCancelled() || Thread.interrupted()) {
                return this.partialResults.get();
            } else {
                System.err.println("Terminating because of uncaught exception when finding codeSet");
                throw e;
            }
        }
        // We draw the first i codes we found
        int i = this.maxPrint == 0 ? localCodes.size() : this.maxPrint;

        // Zhao Yu Li, Jun 24, 2025.
        // Changed from a primitive integer to an AtomicInteger so it can be incremented inside functions
        AtomicInteger codeNum = new AtomicInteger(1);

        // Take the first code not already drawn, and submit it to the storageExecutor for processing
        totalCodes += localCodes.size();
        if (!printMid) {
            for(ClassifiedCodeSequence classCodeSeq: localCodes) {
                if(i <= 0) break;
                --i;

                System.out.println(Utils.standard(classCodeSeq, codeNum.getAndIncrement()));

                addToIterToLimitCover(classCodeSeq.toString());

                // Zhao Yu Li, Jul 31, 2025
                // We need to always load the storages because we will check if the next coordinate is inside of any
                // of the polygons formed by these storages.
//                if(usedCodes.contains(classCodeSeq) || !this.draw) { // Update in the case of not drawing this code
//                    this.updateProgress(progress.incrementAndGet(), todo);
//                    continue;
//                }

                // Zhao Yu Li, Jun 24, 2025.
                // Replaced code block with function call
                loadStorage(usedCodes, todo, futures, progress, classCodeSeq);
            }
        } else {
            // Zhao Yu Li, May 06, 2025.
            // Prints only the middle code of each (code type, code length, and odd-even pattern) group
            final CodeType[] codeTypes = {CodeType.CS, CodeType.OSO, CodeType.OSNO, CodeType.CNS, CodeType.ONS};

            long currentLength = -1;
            Map<CodeType, Map<String, ArrayList<ClassifiedCodeSequence>>> processedCodes = new HashMap<>();
            Map<CodeType, Map<String, Integer>> processedCodesLength = new HashMap<>();

            for (CodeType codeType : codeTypes) {
                processedCodes.put(codeType, new HashMap<>());
                processedCodesLength.put(codeType, new HashMap<>());
            }

            for(ClassifiedCodeSequence code: localCodes) {
                if (i <= 0) break;

                if (currentLength == -1) {
                    currentLength = code.codeLength;
                }

                // Zhao Yu Li, Jun 24, 2025.
                // Replaced code block with function call
                if (code.codeLength == currentLength) addProcessedCode(processedCodes, processedCodesLength, code);
                else {
                    for (CodeType codeType : codeTypes) {
                        if (i <= 0) break;

                        for (String oddEvenPattern : processedCodesLength.get(codeType).keySet()) {
                            if (i <= 0) break;

                            --i;
                            printAndLoadStorage(
                                    processedCodes,
                                    processedCodesLength,
                                    codeType,
                                    oddEvenPattern,
                                    codeNum,
                                    usedCodes,
                                    todo,
                                    futures,
                                    progress
                            );
                        }

                        // Clear and re-initialize the maps for the next iteration
                        processedCodes.get(codeType).clear();
                        processedCodesLength.get(codeType).clear();
                    }

                    currentLength = code.codeLength;
                    processedCodes.get(code.codeType).put(code.oddEvenPattern, new ArrayList<>());
                    processedCodes.get(code.codeType).get(code.oddEvenPattern).add(code);
                    processedCodesLength.get(code.codeType).put(code.oddEvenPattern, 1);
                }
            }

            for (CodeType codeType : codeTypes) {
                if (i <= 0) break;

                // We reached the end of the iteration, add the middle of last (code type, code length, odd-even) group
                for (String oddEvenPattern : processedCodesLength.get(codeType).keySet()) {
                    if (i <= 0) break;

                    // Zhao Yu Li, Jun 24, 2025.
                    // Replaced code block with function call
                    if (!processedCodes.get(codeType).get(oddEvenPattern).isEmpty()) {
                        --i;
                        printAndLoadStorage(
                                processedCodes,
                                processedCodesLength,
                                codeType,
                                oddEvenPattern,
                                codeNum,
                                usedCodes,
                                todo,
                                futures,
                                progress
                        );
                    }
                }
            }
        }

        for(int p = 0; p < i; ++p) { // Update in the case of < i codes
            this.updateProgress(progress.incrementAndGet(), todo);
        }

		if (idx + step >= end) {
            System.out.println("//~~~~~~~~~~~~~~~~~~~~~~~~~~~ " + (totalCodes + codesFound)
                    + " codes found total ~~~~~~~~~~~~~~~~~~~~~~~~~~~");//added // george sept27,2017
        }

        Optional<ExecutionException> except = Optional.empty();

        // If one of the futures throws an exception (like a failed to
        // calculate exception), we need to save it, cancel the rest of
        // the futures, and then throw that exception to bubble up the stack
        for (final Future<Either<String, Storage>> future : futures) {
            checkStatus(future, except);
        }

        return this.partialResults.get();
    }

    /**
     * Adds <code>code</code> to the appropriate (code type, odd-even pattern) group in <code>processedCodes</code>
     * and increments the size of that group.
     * @param processedCodes Stores all (code type, odd-even pattern) groups. All codes are of the same code length.
     * @param processedCodesLength Stores the size of each (code type, odd-even pattern group) in <code>processedCodes</code>.
     * @param code The <code>ClassifiedCodeSequence</code> to add.
     */
    public static void addProcessedCode(
            Map<CodeType, Map<String, ArrayList<ClassifiedCodeSequence>>> processedCodes,
            Map<CodeType, Map<String, Integer>> processedCodesLength,
            ClassifiedCodeSequence code
    ) {
        processedCodesLength.get(code.codeType).compute(code.oddEvenPattern,
                (k, lengthCount) -> (lengthCount == null) ? 1 : lengthCount + 1);

        if (!processedCodes.get(code.codeType).containsKey(code.oddEvenPattern)) {
            processedCodes.get(code.codeType).put(code.oddEvenPattern, new ArrayList<>());
        }
        processedCodes.get(code.codeType).get(code.oddEvenPattern).add(code);
    }

    // Cancel or detect execution errors; This is where we do checking to see if we were cancelled
    private void checkStatus(final Future<Either<String, Storage>> future, Optional<ExecutionException> except) {
        if (this.isCancelled() || except.isPresent()) {
            // If the task was cancelled, or one of the futures threw an
            // exception, we need to cancel the rest of the futures
            //System.out.println("//Cancelling submitted future");
            future.cancel(true);
        } else {
            try {
                future.get();
            } catch (final ExecutionException e) {
                // One of the futures threw an exception during its calculation,
                // so we need to cancel the rest of the futures
                except = Optional.of(e);
            } catch (final InterruptedException e) {
                if (!this.isCancelled()) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // Calculates codeSequence set at a specific coordinate 
    private MutableSortedSet<ClassifiedCodeSequence> autoCodesFiltered(final Vector2 coords, final ExecutorService executor) {
        // autoVary requires coordinates to be in degree format
        final MutableSortedSet<ClassifiedCodeSequence> codes = new TreeSortedSet<>();
        final MutableSortedSet<ClassifiedCodeSequence> boyanCodes = overrideSS ? boyanMenu.varyTrianglesL(coords, this.CSmaxSS, this.OSOmaxSS, this.OSNOmaxSS, executor) : boyanMenu.varyTrianglesL(coords, executor);
        // Generate the filtered list
        for (ClassifiedCodeSequence code : boyanCodes) {
            if (code.codeType.equals(CodeType.OSO) && code.codeLength > OSOmax) {
                continue;
            } else if (code.codeType.equals(CodeType.OSNO) && code.codeLength > OSNOmax) {
                continue;
            } else if (code.codeType.equals(CodeType.CS) && code.codeLength > CSmax) {
                continue;
            }
            if(!this.coverCodes.contains(code.codeSequence.toString())) {
                codes.add(code);
            }
        }
        return codes;
    }


    // Find the storage associated to a codeSequence if it exists. Return the error if not
    private Either<String, Storage> loadStorage(final ClassifiedCodeSequence classCodeSeq) {
        // Check to see if cancel was called
        if(this.isCancelled() || Thread.interrupted()) {
            // Note that this method is intended to be submitted to an executor, hence this interrupts the thread inside the threadpool
            Thread.currentThread().interrupt();
            System.out.println("//Cancel detected before loadStorage");
            return Either.left("");
        }
        // Load from database if code already exists. If not, calculate
        final Optional<Storage> opt = Database.loadStorage(classCodeSeq, this.pool);
        // Check to see if cancel was called
        if(this.isCancelled() || Thread.interrupted()) {
            Thread.currentThread().interrupt();
            System.out.println("//Cancel detected after loadStorage");
            return Either.left("");
        }
        if (opt.isPresent()) {
            final Storage storage = opt.get();
            // Update partialResults on the application thread in order to enforce thread safety
            Platform.runLater(() -> this.partialResults.get().add(storage));
            return Either.right(storage);
        } else {
            return Either.left("//empty set " + classCodeSeq);
        }
    }

    // These expose partialResults to the FX application thread
    public final ObservableList<Storage> getPartials() {
        return this.partialResults.get();
    }
    public final ReadOnlyObjectProperty<ObservableList<Storage>> getPartialProperty() {
        return this.partialResults.getReadOnlyProperty();
    }

    /**
     * Finds an all-positive and a plus/minus iteration pattern for <code>classCodeSeq</code> and add the pairs to the
     * appropriate text areas of the <code>IterateToLimitWindow</code> instance that was passed as an argument at the
     * construction of this task.
     * @param classCodeSeq The <code>ClassifiedCodeSequence</code> to add to the <code>IterateToLimitWindow</code> Cover
     */
    private void addToIterToLimitCover(String classCodeSeq) {
        if (addToAllPositive) {
            String iterationPattern = IterateToLimitWindow.getIterationPattern(classCodeSeq, true);

            if (iterationPattern.isEmpty()) {
                System.out.println("Skip adding "
                        + classCodeSeq
                        + " to the IterateToLimitWindow Cover (all-positive) because we cannot find a valid iteration pattern");
            } else iterateToLimitWindow.addToContent(classCodeSeq, iterationPattern, true);
        }

        if (addToPlusMinus) {
            String iterationPattern = IterateToLimitWindow.getIterationPattern(classCodeSeq, false);

            if (iterationPattern.isEmpty()) {
                System.out.println("Skip adding "
                        + classCodeSeq
                        + " to the IterateToLimitWindow Cover (+/-) because we cannot find a valid iteration pattern");
            } else iterateToLimitWindow.addToContent(classCodeSeq, iterationPattern, false);
        }
    }

    /**
     * <p>
     *     Prints the middle code of the (<code>codeType</code>, code length, <code>oddEvenPattern</code>) group to the
     *     terminal. Optionally also prints the first and last of each group.
     * </p>
     * <p>
     *     <b>NOTE</b>: Assumes all <code>ClassifiedCodeSequence</code> in <code>processedCodes</code> are of the same
     *     code length.
     * </p>
     * @param processedCodes Stores all (code type, odd-even pattern) groups. All codes are of the same code length.
     * @param processedCodesLength Stores the size of each (code type, odd-even pattern group) in <code>processedCodes</code>.
     * @param codeType The type of <code>ClassifiedCodeSequence</code> we are printing.
     * @param oddEvenPattern The odd-even pattern of the codes we are printing.
     * @param codeNum Within the context of the whole task, we will print the <code>codeNum</code>'th code and onwards.
     * @return The array of codes we printed, in the order that we printed them.
     */
    private ArrayList<ClassifiedCodeSequence> printMidFirstLast(
            Map<CodeType, Map<String, ArrayList<ClassifiedCodeSequence>>> processedCodes,
            Map<CodeType, Map<String, Integer>> processedCodesLength,
            CodeType codeType,
            String oddEvenPattern,
            AtomicInteger codeNum
    ) {
        ArrayList<ClassifiedCodeSequence> codesPrinted = new ArrayList<>();

        final ClassifiedCodeSequence codeToPrint = processedCodes.get(codeType)
                .get(oddEvenPattern)
                .get(processedCodesLength.get(codeType).get(oddEvenPattern) / 2);

        if (firstLast) {
            if (processedCodesLength.get(codeType).get(oddEvenPattern) >= 2) {
                final ClassifiedCodeSequence firstCode = processedCodes
                        .get(codeType)
                        .get(oddEvenPattern)
                        .get(0);
                addToIterToLimitCover(firstCode.toString());
                System.out.println(Utils.standard(firstCode, codeNum.getAndIncrement()));
                codesPrinted.add(firstCode);
            }
        }

        addToIterToLimitCover(codeToPrint.toString());
        System.out.println(Utils.standard(codeToPrint, codeNum.getAndIncrement()));
        codesPrinted.add(codeToPrint);

        if (firstLast) {
            if (processedCodesLength.get(codeType).get(oddEvenPattern) >= 3) {
                final ClassifiedCodeSequence lastCode = processedCodes
                        .get(codeType)
                        .get(oddEvenPattern)
                        .get(processedCodesLength
                                .get(codeType)
                                .get(oddEvenPattern) - 1);
                addToIterToLimitCover(lastCode.toString());
                System.out.println(Utils.standard(lastCode, codeNum.getAndIncrement()));
                codesPrinted.add(lastCode);
            }
        }

        return codesPrinted;
    }

    /**
     * Loads the <code>Storage</code> for <code>codePrinted</code> and updates the <code>progress</code>.
     * @param usedCodes The set of codes we have already loaded a <code>Storage</code> for.
     * @param todo The total number of codes to load <code>Storage</code> for.
     * @param futures Since loading <code>Storage</code> can take some time, we will launch each load as a task, and store its <code>Future</code> in this list.
     * @param progress The number of loads we have finished.
     * @param codePrinted The <code>ClassifiedCodeSequence</code> to load a <code>Storage</code> for.
     */
    private void loadStorage(
            MutableSortedSet<ClassifiedCodeSequence> usedCodes,
            int todo,
            MutableList<Future<Either<String, Storage>>> futures,
            AtomicInteger progress,
            ClassifiedCodeSequence codePrinted
    ) {
        usedCodes.add(codePrinted);
        // Submit the custom PriorityCallable for this code (Node that PriorityCallable is a custom interface)
        futures.add(storageExecutor.submit(new PriorityCallable<Either<String, Storage>>() {
                    @Override
                    public Either<String, Storage> call() {
                        Either<String, Storage> result = loadStorage(codePrinted);
                        VaryLTask.this.updateProgress(progress.incrementAndGet(), todo);
                        return result;
                    }

                    @Override
                    public int getPriority() {
                        return codePrinted.length();
                    }
                })
        );
    }

    /**
     * <code>printMidFirstLast</code> and <code>loadStorage</code> wrapped in one function.
     * @param processedCodes Stores all (code type, odd-even pattern) groups. All codes are of the same code length.
     * @param processedCodesLength Stores the size of each (code type, odd-even pattern group) in <code>processedCodes</code>.
     * @param codeType The type of <code>ClassifiedCodeSequence</code> we are printing.
     * @param oddEvenPattern The odd-even pattern of the codes we are printing.
     * @param codeNum Within the context of the whole task, we will print the <code>codeNum</code>'th code and onwards.
     * @param usedCodes The set of codes we have already loaded a <code>Storage</code> for.
     * @param todo The total number of codes to load <code>Storage</code> for.
     * @param futures Since loading <code>Storage</code> can take some time, we will launch each load as a task, and store its <code>Future</code> in this list.
     * @param progress The number of loads we have finished.
     */
    private void printAndLoadStorage(
            Map<CodeType, Map<String, ArrayList<ClassifiedCodeSequence>>> processedCodes,
            Map<CodeType, Map<String, Integer>> processedCodesLength,
            CodeType codeType,
            String oddEvenPattern,
            AtomicInteger codeNum,
            MutableSortedSet<ClassifiedCodeSequence> usedCodes,
            int todo,
            MutableList<Future<Either<String, Storage>>> futures,
            AtomicInteger progress

    ) {
        ArrayList<ClassifiedCodeSequence> codesPrinted = printMidFirstLast(
                processedCodes,
                processedCodesLength,
                codeType,
                oddEvenPattern,
                codeNum
        );

        for (ClassifiedCodeSequence codePrinted : codesPrinted) {
            // Zhao Yu Li, Jul 31, 2025
            // We need to always load the storages because we will check if the next coordinate is inside of any
            // of the polygons formed by these storages.
//            if(usedCodes.contains(codePrinted) || !this.draw) { // Update in the case of not drawing this code
//                this.updateProgress(progress.incrementAndGet(), todo);
//                continue;
//            }

            loadStorage(usedCodes, todo, futures, progress, codePrinted);
        }
    }
}
