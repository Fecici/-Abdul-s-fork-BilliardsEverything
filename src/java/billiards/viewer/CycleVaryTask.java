package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeType;
import billiards.codeseq.Storage;
import billiards.database.Database;
import billiards.geometry.Vector2;
import billiards.utils.PrintMid;
import billiards.wrapper.ConnectionPool;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javaslang.collection.Array;
import javaslang.control.Either;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.sorted.mutable.TreeSortedSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/*
PolyVaryTask encompasses the process of finding codes and calculating corresponding code regions.
Both these processes are multithreaded. Because of this, the task does not perform ui updates directly. Instead, you should access partialResults using getPartials() or getPartialProperty() and set up a change listener from the javafx application thread if you wish to provide ui updates during execution.

If only the final result is required, you can just call get() on this task after it finishes.
 * */
public final class CycleVaryTask extends Task<ObservableList<Storage>> {
    // Expose task property representing partial results
    private ReadOnlyObjectWrapper<ObservableList<Storage>> partialResults =
            new ReadOnlyObjectWrapper<>(
                    this,
                    "partialResults",
                    FXCollections.observableArrayList(
                            new ArrayList<Storage>()
                    )
            );
    private final Array<Vector2> coordList;
    private final MutableSortedSet<ClassifiedCodeSequence> onScreenCodes;
    private final ConnectionPool pool;
    private final int CSmax;
    private final int OSOmax;
    private final int OSNOmax;
    private final int CSmaxSS;
    private final int OSOmaxSS;
    private final int OSNOmaxSS;
    private final ExecutorService storageExecutor;
    private final ExecutorService shotExecutor;
    private final ImageView screenImage;
    private final PixelRadianMap screenMap;
    private final int mode;
    private final int numGroupToPrint;
    private final int shots;
    final boolean CSIsSelected;
    final boolean OSOIsSelected;
    final boolean OSNOIsSelected;

    // Constructor takes a list of points to vary at
    public CycleVaryTask(
        final MutableList<Double> points, final MutableSortedSet<ClassifiedCodeSequence> onScreenCodes,
        final Array<Integer> max, final ConnectionPool pool, final ExecutorService eOne, final int shots,
        final ExecutorService eTwo, final ImageView screen, final PixelRadianMap map, final int mode,
        final int numGroupToPrint, final boolean CSIsSelected, final boolean OSOIsSelected, final boolean OSNOIsSelected) {
        this.coordList = toCoords(points);
        this.onScreenCodes = onScreenCodes;
        this.CSmax = max.get(0);
        this.OSOmax = max.get(1);
        this.OSNOmax = max.get(2);
        this.CSmaxSS = max.get(3);
        this.OSOmaxSS = max.get(4);
        this.OSNOmaxSS = max.get(5);
        this.pool = pool;

        this.storageExecutor = eOne;
        this.shotExecutor = eTwo;
        this.screenImage = screen;
        this.screenMap = map;
        this.mode = mode;
        this.numGroupToPrint = numGroupToPrint;
        this.shots = shots;
        this.CSIsSelected = CSIsSelected;;
        this.OSOIsSelected = OSOIsSelected;;
        this.OSNOIsSelected = OSNOIsSelected;;
    }

    @Override
    protected ObservableList<Storage> call() {
        // storageExecutor handles the more expensive process of calculating code regions,
        // while shotExecutor handles the much faster calculation of finding the codes present at a given point

        final MutableSortedSet<ClassifiedCodeSequence> usedCodes = new TreeSortedSet<ClassifiedCodeSequence>();
        final MutableList<Future<Either<String, Storage>>> futures = new FastList<>();

        AtomicInteger progress = new AtomicInteger(); // Create an integer which supports non-locking concurrent operations
        final int todo = this.coordList.size();
        this.updateProgress(0, todo);

        int emptyMax = 8; // Max number of empty pixels. Hardcoded for now//george jan3,2025 you can change the 8 to whatever
        int empty = 0; // Number of empty pixels
        // The meat and potatoes. Finds codes sequentially, and submits them to the executer as they are found.
        // This is the most efficient way to implement multithreaded polyvary since each code can be calculated as soon as it's found, without interfering with the process of finding more codes.
        for(Vector2 coord: this.coordList) {
            MutableSortedSet<ClassifiedCodeSequence> localCodes;
            // The BoyanCodes method vary3() called by autoVary() can throw exceptions. We need to catch them
            // By taking a second to check the pixel color, we can potentially avoid all other work for this coord.
            int color = pixelColor(coord);
            this.updateProgress(progress.incrementAndGet(), todo);
            if(color != 0) continue; 
            try {
                localCodes = autoCodesFiltered(coord, shotExecutor);
            } catch(RuntimeException e) {
                if(this.isCancelled() || Thread.interrupted()) {
                    break;
                } else {
                    System.err.println("Terminating because of uncaught exception when finding codeSet");
                    throw e;
                }
            }
            // We want to know if we submitted a task that will update the progress for us.
            if(localCodes.isEmpty()) {
                ++empty;
                if(empty >= emptyMax) {
                    System.out.println("Finish Vary due to too many empty pixels");
                    break;
                }
                this.updateProgress(progress.incrementAndGet(), todo);
                continue;
            }

            // Zhao Yu Li, Jul 03, 2025.
            // Do not invalidate all results just because only one code was used previously
            // Check if any of the codes found were previously used
//            boolean used = false;
//            for(ClassifiedCodeSequence code: usedCodes) {
//                used = used || localCodes.contains(code);
//            }
//            for(ClassifiedCodeSequence code: this.onScreenCodes) {
//                used = used || localCodes.contains(code);
//            }
//            if(used) {
//                this.updateProgress(progress.incrementAndGet(), todo);
//                continue;
//            }
            if (mode == 0) {
                boolean noCodes = true;

                // Take the first code not already drawn, and submit it to the storageExecutor for processing
                for(ClassifiedCodeSequence classCodeSeq: localCodes) {
                    if(this.onScreenCodes.contains(classCodeSeq) || usedCodes.contains(classCodeSeq)) continue;

                    noCodes = loadStorageFromDB(classCodeSeq, usedCodes, futures, progress, todo);
                    break;
                }

                if(noCodes) { // Still need to update progress even if nothing found
                    this.updateProgress(progress.incrementAndGet(), todo);
                }
            } else if (mode == 1) {
                ArrayList<ClassifiedCodeSequence> printedCodes = PrintMid.printMid(localCodes, numGroupToPrint);
                loadPrintedCodesStorage(usedCodes, futures, progress, todo, printedCodes);
            } else if (mode == 2) {
                ArrayList<ClassifiedCodeSequence> printedCodes = PrintMid.printFirstMidLast(localCodes, numGroupToPrint, true);
                loadPrintedCodesStorage(usedCodes, futures, progress, todo, printedCodes);
            } else {
                throw new NotImplementedException("Invalid mode value for CycleTask");
            }
        }


        Optional<ExecutionException> except = Optional.empty();

        // If one of the futures throws an exception (like a failed to
        // calculate exception), we need to save it, cancel the rest of
        // the futures, and then throw that exception to bubble up the stack
        for (final Future<Either<String, Storage>> future : futures) {
            except = checkStatus(future);
        }

        if (except.isPresent()) {
            throw new RuntimeException(except.get());
        }

        return this.partialResults.get();
    }

    // Cancel or detect execution errors; This is where we do checking to see if we were cancelled
    private Optional<ExecutionException> checkStatus(final Future<Either<String, Storage>> future) {
        Optional<ExecutionException> except = Optional.empty();
        if (this.isCancelled()) {
            // If the task was cancelled, or one of the futures threw an
            // exception, we need to cancel the rest of the futures
            //System.out.println("//Cancelling submitted future");
            future.cancel(true);
        } else {
            try {
                final Either<String, Storage> either = future.get();
                if (either.isLeft()) { // Print things like empty sets 
                    if(!either.left().get().isEmpty()) System.out.println(either.left().get());
                }
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
        return except;
    }

    // Calculates codeSequence set at a specific coordinate 
    private MutableSortedSet<ClassifiedCodeSequence> autoCodesFiltered(final Vector2 coords, final ExecutorService executor) {
        // autoVary requires coordinates to be in degree format
        final Vector2 degCoords = Vector2.create(Math.toDegrees(coords.x), Math.toDegrees(coords.y));
        final MutableSortedSet<ClassifiedCodeSequence> codes = new TreeSortedSet<>();
        final MutableSortedSet<ClassifiedCodeSequence> boyanCodes = autoVary(degCoords, this.CSmaxSS, this.OSOmaxSS, this.OSNOmaxSS, executor);
        // Generate the filtered list
        for (ClassifiedCodeSequence code : boyanCodes) {
            if (CSIsSelected && code.codeType.equals(CodeType.CS)) {
                if (code.codeLength <= this.CSmax) codes.add(code);
            } else if (OSOIsSelected && code.codeType.equals(CodeType.OSO)) {
                if (code.codeLength <= this.OSOmax) codes.add(code);
            } else if (OSNOIsSelected && code.codeType.equals(CodeType.OSNO)) {
                if (code.codeLength <= this.OSNOmax) codes.add(code);
            }
        }
        return codes;
    }

    // Converts list of points into array of coordinate pairs 
    private Array<Vector2> toCoords(final MutableList<Double> points) {
        final MutableList<Vector2> out = new FastList<Vector2>();
        for(int i = 0; i < points.size(); i += 2) {
            final Vector2 coords = Vector2.create(points.get(i), points.get(i+1));
            out.add(coords);
        }
        Collections.shuffle(out); // Randomize as an optimization
        return Array.ofAll(out);
    }

    // Runs a fast application thread task which determines the color of the pixel at a point
    private int pixelColor(final Vector2 point) {
        FutureTask<Integer> task = new FutureTask<Integer>(() -> {
            final Image image = this.screenImage.getImage();
            final PixelReader reader = image.getPixelReader();
            final int midX = (int) this.screenMap.pixelX(point.x);
            final int midY = (int) this.screenMap.pixelY(point.y);
            return reader.getArgb(midX, midY);
        });
        Platform.runLater(task);
        try {
            //System.err.println("//Found pixel color");
            return task.get();
        } catch(InterruptedException e) {
            System.err.println("//Interruption when finding pixel color");
            return -1;
        } catch(ExecutionException e) {
            System.err.println("//Failed to find pixel color");
            e.printStackTrace();
            return -1;
        }

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

    private boolean loadStorageFromDB(ClassifiedCodeSequence classCodeSeq, MutableSortedSet<ClassifiedCodeSequence> usedCodes,
                                      MutableList<Future<Either<String, Storage>>> futures, AtomicInteger progress,
                                      int todo) {
        usedCodes.add(classCodeSeq);
        // Submit the runnable for this code
        futures.add(storageExecutor.submit(new PriorityCallable<Either<String, Storage>>() {
                    @Override
                    public Either<String, Storage> call() {
                        Either<String, Storage> result = loadStorage(classCodeSeq);
                        if(!CycleVaryTask.this.isCancelled()) CycleVaryTask.this.updateProgress(progress.incrementAndGet(), todo); // updateProgress is thread safe
                        return result;
                    }

                    @Override
                    public int getPriority() {
                        return classCodeSeq.length();
                    }
                })
        );

        return false;
    }


    private void loadPrintedCodesStorage(MutableSortedSet<ClassifiedCodeSequence> usedCodes, MutableList<Future<Either<String, Storage>>> futures, AtomicInteger progress, int todo, ArrayList<ClassifiedCodeSequence> printedCodes) {
        boolean atLeastOneCode = false;

        for(ClassifiedCodeSequence classCodeSeq: printedCodes) {
            boolean skipped = !loadStorageFromDB(classCodeSeq, usedCodes, futures, progress, todo);
            atLeastOneCode = atLeastOneCode || skipped;
        }

        if(!atLeastOneCode) { // Still need to update progress even if nothing found
            this.updateProgress(progress.incrementAndGet(), todo);
        }
    }

    public MutableSortedSet<ClassifiedCodeSequence> autoVary(
            final Vector2 point, final int CSmaxSS, final int OSOmaxSS, final int OSNOmaxSS, final ExecutorService exe) {
        int CSmin = 0;
        int CSstep = 0;
        int OSmin = 0;
        int OSstep = 0;
        final boolean[] noCS = {OSOmaxSS > 0, false, false, false, OSNOmaxSS > 0};

        final boolean[] onlyCS = {false, CSmaxSS > 0, false, false, false};

        final MutableSortedSet<ClassifiedCodeSequence> unfilteredCodesFound = new TreeSortedSet<>();
        final MutableSortedSet<ClassifiedCodeSequence> codesFound = new TreeSortedSet<>();
        if(CSmaxSS > 0) {
            unfilteredCodesFound.addAll(BoyanMenu.findCodes3(point.x, point.y, CSmin, CSmaxSS + CSstep, shots, onlyCS, exe));
        }
        unfilteredCodesFound.addAll(BoyanMenu.findCodes3(point.x, point.y, OSmin, Math.max(OSOmaxSS, OSNOmaxSS) + OSstep, shots, noCS, exe));

        for(final ClassifiedCodeSequence code: unfilteredCodesFound) { // Filter out overly large OSO/OSNO
            final CodeType type = code.codeType;
            if(type.equals(CodeType.OSO) && code.codeSum >= OSOmaxSS) {
                continue;
            }
            if(type.equals(CodeType.OSNO) && code.codeSum >= OSNOmaxSS) {
                continue;
            }
            codesFound.add(code);
        }

        return codesFound;
    }
}
