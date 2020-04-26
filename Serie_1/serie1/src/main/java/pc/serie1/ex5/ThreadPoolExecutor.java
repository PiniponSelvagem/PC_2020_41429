package pc.serie1.ex5;

import java.util.LinkedList;
import java.util.concurrent.Callable;

public class ThreadPoolExecutor {

    private final LinkedList<ExecutorThread> pool = new LinkedList<>();
    private final int maxPoolSize, keepAliveTime;
    private int currPoolSize;

    public ThreadPoolExecutor(int maxPoolSize, int keepAliveTime) {
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
    }

    public <T> Result<T> execute(Callable<T> command) throws InterruptedException {
        ExecutorThread<T> thread = new ExecutorThread<>();
        pool.add(thread);
        thread.submitCommand(command);


        return null;
    }

    public void shutdown() {

    }

    public boolean awaitTermination(int timeout) throws InterruptedException {
        return false;
    }
}
