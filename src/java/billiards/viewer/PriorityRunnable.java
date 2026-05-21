package billiards.viewer;

// Define the type of task which can be submitted to priorityExecutors
public interface PriorityRunnable<T> extends Runnable {
    public int getPriority();
}
