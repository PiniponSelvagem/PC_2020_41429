package pc.serie2.ex2;

import pc.serie2.utils.TimeoutHolder;

import java.util.LinkedList;

public class TransferQueue<T> {

    private final Object monitor = new Object();
    private final LinkedList<T> queue = new LinkedList<>();

    public TransferQueue() {
    }

    public void put(T message) {
        synchronized (monitor) {
            queue.addFirst(message);
            monitor.notify();
        }
    }

    public T take(long timeout) throws InterruptedException {
        synchronized (monitor) {
            T value;
            TimeoutHolder th = new TimeoutHolder(timeout);
            do {
                monitor.wait(timeout);

                if ((timeout = th.getTimeoutLeft()) <= 0 && queue.isEmpty()) {
                    return null;
                }
            } while (queue.isEmpty());
            value = queue.removeLast();
            monitor.notify();
            return value;
        }
    }
}