package billiards.viewer;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// This is a custom threadPoolExecutor which maintains a priority queue, allowing us to complete tasks in order of size
public class PriorityExecutor extends ThreadPoolExecutor {

    public PriorityExecutor(final int poolSize) {
        super(poolSize, poolSize, 60, TimeUnit.SECONDS, 
                new PriorityBlockingQueue<Runnable>(11));
    }

    // The following overrides are used by the submit method to convert runnables into futures
    @Override
    protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
        if (callable instanceof PriorityCallable) {
            return new ComparableFutureTask<T>(((PriorityCallable<T>) callable).getPriority(),
                    callable);
        } else { // Still accept regular (incomparable) future tasks
            return new ComparableFutureTask<T>(0, callable);
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable,
            final T value) {
        if (runnable instanceof PriorityRunnable)
            return new ComparableFutureTask<T>(((PriorityRunnable<?>) runnable).getPriority(),
                    runnable, value);
        else
            return new ComparableFutureTask<T>(0, runnable, value);
    }

    private class ComparableFutureTask<T> extends FutureTask<T> implements Comparable<ComparableFutureTask<T>> {
        private final int priority;

        public ComparableFutureTask(final int priority, final Callable<T> call) {
            super(call);
            this.priority = priority;
        }

        public ComparableFutureTask(final int priority, final Runnable run, final T ret) {
            super(run, ret);
            this.priority = priority;
        }
        @Override
        public int compareTo(final ComparableFutureTask<T> o) {
            return Integer.compare(this.priority, o.priority);
        }
    }
}
