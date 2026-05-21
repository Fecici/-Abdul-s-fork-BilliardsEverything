package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;

import javaslang.collection.Array;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.concurrent.Task;

public final class DontDrawPictureTask extends Task<Void> {
    private final Array<Callable<String>> tasks;

    public DontDrawPictureTask(final Array<ClassifiedCodeSequence> classCodeSeqs, final ConnectionPool pool) {

        this.tasks = classCodeSeqs.map(classCodeSeq -> () -> {

            final boolean nonEmpty = Wrapper.saveToDatabase(classCodeSeq, pool);

            if (nonEmpty) {
                return classCodeSeq.toString();
            } else {
                return "//empty set " + classCodeSeq;
            }
        });
    }

    @Override
    protected Void call() {
        // You know, we could use the structures in javaslang. They are functional,
        // so they might not have the performance of the standard library collections,
        // but they are far better designed, and honestly the performance won't
        // matter. All the high performance code is in C++, and the one place where
        // I think I will use it for basic maps, folds, and filters, but things
        // that could update the data structure, such as add, append, etc that
        // often find in maps I'll avoid.
        // Javaslang is really just a replacement for the whole builder, add to builder,
        // build pattern that I see quite a bit. Replacing it with a map is quite nice.

        final ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);

        // Beauty.
        final Array<Future<String>> futures = this.tasks.map(task -> executor.submit(task));

        Optional<ExecutionException> except = Optional.empty();

        int progress = 0;
        final int todo = futures.size();

        this.updateProgress(progress, todo);

        for (final Future<String> future : futures) {
            if (this.isCancelled() || except.isPresent()) {
                // No point in interrupting the thread, because we can't
                // cancel the future if it has already begun
                future.cancel(false);
            } else {
                try {
                    // When running the futures on multiple threads, it seems
                    // that we update the message and progress too often
                    // for the event loop in the Viewer to register it.
                    // So we have updated this several times by the time
                    // the Viewer gets around to checking that it has been
                    // modified. So we just print it.
                    final String msg = future.get();
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

        executor.shutdown();

        if (except.isPresent()) {
            throw new RuntimeException(except.get());
        }

        return null;
    }
}
