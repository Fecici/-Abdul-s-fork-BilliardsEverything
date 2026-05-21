package billiards.utils;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.Storage;
import billiards.database.Database;
import billiards.viewer.Utils;
import billiards.wrapper.ConnectionPool;
import javaslang.control.Either;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Zhao Yu Li, Jun 3, 2025.
 * Load Storage for a Collection of ClassifiedCodeSequences
 * Updated Jun 5, 2025.
 * Always load the storage for the code. Before, we don't load storage for duplicate codes. The check for duplicate
 * codes is removed so that we return the same number of Storages as the number of input ClassifiedCodeSequences
 */
public class BatchLoadStorage {
    public static ArrayList<Storage> batchLoadStorage(Collection<ClassifiedCodeSequence> codes, ConnectionPool pool) {
        final MutableList<Future<Either<String, Storage>>> futures = new FastList<>();

        ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);

        for (ClassifiedCodeSequence code : codes) {
            futures.add(executor.submit(() -> loadStorage(code, pool)));
        }

        int taskCount = 0;
        int taskCompleted = 0;
        ArrayList<Either<String, Storage>> result = new ArrayList<>();

        for (Future<Either<String, Storage>> future : futures) {
            taskCompleted += checkStatus(future, !(taskCount++ == taskCompleted), result);
        }

        ArrayList<Storage> storages = new ArrayList<>();

        for (Either<String, Storage> e : result) {
            if (e.isRight()) storages.add(e.get());
        }

        executor.shutdown();

        return storages;
    }

    // Find the storage associated to a codeSequence if it exists. Return the error if not
    private static Either<String, Storage> loadStorage(final ClassifiedCodeSequence classCodeSeq, ConnectionPool pool) {
        // Check to see if cancel was called
        if(Thread.interrupted()) {
            // Note that this method is intended to be submitted to an executor, hence this interrupts the thread inside the thread pool
            Thread.currentThread().interrupt();
            System.out.println("//Cancel detected before loadStorage");
            return Either.left("");
        }
        // Load from database if code already exists. If not, calculate
        final Optional<Storage> opt = Database.loadStorage(classCodeSeq, pool);
        // Check to see if cancel was called
        if(Thread.interrupted()) {
            Thread.currentThread().interrupt();
            System.out.println("//Cancel detected after loadStorage");
            return Either.left("");
        }
        if (opt.isPresent()) {
            final Storage storage = opt.get();
            return Either.right(storage);
        } else {
            return Either.left("//empty set " + classCodeSeq);
        }
    }

    // Cancel or detect execution errors; This is where we do checking to see if we were cancelled
    private static int checkStatus(
            final Future<Either<String, Storage>> future, boolean cancel, ArrayList<Either<String, Storage>> codes
    ) {
        if (cancel) {
            // If the task was cancelled, or one of the futures threw an
            // exception, we need to cancel the rest of the futures
            future.cancel(true);
            return 0;
        } else {
            try {
                codes.add(future.get());
                return 1;
            } catch (final ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
