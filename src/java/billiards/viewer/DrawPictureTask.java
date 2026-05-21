package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeType;
import billiards.codeseq.Storage;
import billiards.database.Database;
import billiards.wrapper.ConnectionPool;

import javaslang.collection.Array;
import javaslang.control.Either;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

public final class DrawPictureTask extends Task<Array<Storage>> {
    protected final Array<Callable<Either<String, Storage>>> tasks;
    protected final boolean print;
    protected final boolean detailed;

    private final ExecutorService executor;
    private ReadOnlyObjectWrapper<ObservableList<Storage>> partialResults =
            new ReadOnlyObjectWrapper<>(
                    this, 
                    "partialResults",
                    FXCollections.observableArrayList(
                            new ArrayList<Storage>()
                    )
            );

    // These expose partialResults to the FX application thread
    public final ObservableList<Storage> getPartials() {
        return this.partialResults.get();
    }
    
    public final ReadOnlyObjectProperty<ObservableList<Storage>> getPartialProperty() {
        return this.partialResults.getReadOnlyProperty();
    }

    public DrawPictureTask(
        final Array<ClassifiedCodeSequence> classCodeSeqs, final ConnectionPool pool, final ExecutorService executor, boolean print, boolean detailed) {
        this.print = print;
        this.detailed = detailed;
        this.executor = executor;
        this.tasks = classCodeSeqs.map(classCodeSeq -> () -> {
            // We could check here for Thread.interrupted() to see if we should
            // cancel the task, but this operation is one-shot,
            // so there's not much point

            // Load from database if code already exists. If not, calculate
            final Optional<Storage> opt = Database.loadStorage(classCodeSeq, pool);

            if (opt.isPresent()) {
                final Storage storage = opt.get();
                Platform.runLater(() -> this.partialResults.get().add(storage));
                return Either.right(storage);
            } else {
                return Either.left("//empty set " + classCodeSeq);
            }
        });
    }

    @Override
    protected Array<Storage> call() {
        //final ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);

        final Array<Future<Either<String, Storage>>> futures =
            this.tasks.map(task -> this.executor.submit(task));

        int progress = 0;
        final int todo = futures.size();

        this.updateProgress(progress, todo);

        final ArrayList<Storage> storages = new ArrayList<>();

        Optional<ExecutionException> except = Optional.empty();

        // If one of the futures throws an exception (like a failed to
        // calculate exception), we need to save it, cancel the rest of
        // the futures, and then throw that exception to bubble up the stack

        // This is where we do checking to see if we were cancelled
        for (final Future<Either<String, Storage>> future : futures) {
            if (this.isCancelled() || except.isPresent()) {
                // If the task was cancelled, or one of the futures threw an
                // exception, we need to cancel the rest of the futures

                // There is no point in interrupting the thread, since we can't
                // cancel the future while it is running
                future.cancel(false);
            } else {
                try {
                    final Either<String, Storage> either = future.get();

                    final String msg;
                    if (either.isLeft()) {
                        msg = either.getLeft();
                    } else {
                        final Storage storage = either.get();
                        storages.add(storage);
                        if(detailed) {
                            // print the code, whether it covered the pixel or not
                            final CodeType type = storage.codeType();

                            String codeStr = "" + type;
                            // String codeStr = "xxx " + type; //george july 26 2017 -
                            // type whatever you want between the quotes in the line above
                            // make sure to add a space after the xxx
                            if (codeStr.equals("CS")) {
                                codeStr += "  ";
                            } else if (!codeStr.equals("OSNO")) {
                                codeStr += " ";
                            }
                            msg = codeStr + " (" + storage.codeLength() + ", " + storage.codeSum() + ") " + storage.toString();
                        } else {
                            msg = storage.toString();
                        }
                    }
                    
                    if (print) {
                    	System.out.println(msg);
                    }
                    // this.updateMessage(msg);

                    progress += 1;
                    this.updateProgress(progress, todo);
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

        // TODO This does not cancel futures that are currently running
        // (like when we hit cancel or a future throws an exception). Should we wait for them to
        // finish?

        // If there is an exception that happened, throw it now after shutting down the executor
        if (except.isPresent()) {
            throw new RuntimeException(except.get());
        }

        return Array.ofAll(storages);
    }
}
