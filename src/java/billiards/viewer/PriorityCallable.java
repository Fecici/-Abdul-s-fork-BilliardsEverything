package billiards.viewer;

import java.util.concurrent.Callable;

// Define the type of task which can be submitted to priorityExecutors
public interface PriorityCallable<T> extends Callable<T> {
    public int getPriority();
}
