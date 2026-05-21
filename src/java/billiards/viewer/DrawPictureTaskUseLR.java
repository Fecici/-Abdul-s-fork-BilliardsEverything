package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.Storage;
import billiards.database.Database;
import billiards.database.InfoAll;
import billiards.wrapper.ConnectionPool;
import billiards.database.Info;

import billiards.wrapper.Wrapper;
import javaslang.collection.Array;
import javaslang.control.Either;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.*;

import javafx.concurrent.Task;

// This task is almost identical to the other one. Hmmm, is there some way
// of dealing with that?
//
// Who wrote this stupid code?
public final class DrawPictureTaskUseLR extends Task<Array<Storage>> {
    private final Array<Callable<Either<String, Storage>>> tasks;
    //public CopyOnWriteArrayList<ClassifiedCodeSequence> baseCodeSeq = new CopyOnWriteArrayList<>();
    //public ArrayList<ClassifiedCodeSequence> baseCodeSeq = new ArrayList<>();
    private static ClassifiedCodeSequence breakPoint = null;
    private static Object breakPointLock = new Object();
    public DrawPictureTaskUseLR(
            final Array<ClassifiedCodeSequence> classCodeSeqs, final ConnectionPool pool) {

        // The first code seq in the array is the one we use the left rights from
        //baseCodeSeq.add(classCodeSeqs.get(0));
        final ClassifiedCodeSequence baseCodeSeq = classCodeSeqs.get(0);
        this.tasks =  classCodeSeqs.map(classCodeSeq -> () -> {

            // We could check here for Thread.interrupted() to see if we should
            // cancel the task, but most of these operations are one-shot,
            // so there's no point
            final Optional<Storage> opt = Database.loadStorageUseLR("empty",baseCodeSeq, classCodeSeq, pool);
            if (opt.isPresent()) {
                return Either.right(opt.get());
            } else {
                synchronized (breakPointLock) {
                    if (breakPoint == null || classCodeSeq.compareTo(breakPoint) < 0) {
                        breakPoint = classCodeSeq;
                    }
                }
                Wrapper.deleteFromDatabase(classCodeSeq,pool);
                return Either.left("// empty set " + classCodeSeq);
            }

        });
    }

    @Override
    protected Array<Storage> call() {
        final ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);

        final Array<Future<Either<String, Storage>>> futures =
                this.tasks.map(task -> executor.submit(task));

        int progress = 0;
        final int todo = futures.size();

        this.updateProgress(progress, todo);

        final ArrayList<Storage> storages = new ArrayList<>();

        Optional<ExecutionException> except = Optional.empty();

        // This is where we do checking to see if we were cancelled
        for (final Future<Either<String, Storage>> future : futures) {
            if (this.isCancelled() || except.isPresent()) {
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
                        msg = storage.toString();

                        //aug 25,2019 george thia shows the use left right and prints 1 3 3 for example as storage
                        // System.out.print("storage: " + storage + "\n");

                    }
                    String temp = "// empty set " + breakPoint;
                    //System.out.println("temp"+temp);
                    //System.out.println("msg"+msg);
                    if (msg.equals(temp)) {
                        System.out.println("Stop at " + breakPoint);
                        breakPoint = null;
                        break;
                    }
                    else if (msg.contains("// empty set ")) {
                        String msg2 = msg.replace("// empty set ","Stop at ");
                        System.out.println(msg2);
                        breakPoint = null;
                        break;
                    }
                    System.out.println(msg);
                    // this.updateMessage(msg);
                    progress += 1;
                    this.updateProgress(progress, todo);
                } catch (final ExecutionException e) {
                    except = Optional.of(e);
                } catch (final InterruptedException e) {
                    if (!this.isCancelled()) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        // TODO This does not cancel futures that are currently running
        // (like when we hit cancel). Should we wait for them to finish?
        executor.shutdown();

        if (except.isPresent()) {
            throw new RuntimeException(except.get());
        }

        return Array.ofAll(storages);
    }
}
