package pc.serie1.ex4;

import pc.serie1.utils.TimeoutHolder;

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

    public boolean transfer(T message, long timeout) throws InterruptedException {
        synchronized (monitor) {
            queue.addFirst(message);
            monitor.notify();

            TimeoutHolder th = new TimeoutHolder(timeout);
            do {
                monitor.wait(timeout);
                if (Thread.interrupted()) {
                    queue.remove(message);
                    throw new InterruptedException();
                }

                if ((timeout = th.getTimeoutLeft()) <= 0 && queue.contains(message)) {
                    queue.remove(message);
                    return false;
                }
            } while (queue.contains(message));
        }
        return true;
    }

    public T take(long timeout) throws InterruptedException {
        T value;
        synchronized (monitor) {
            TimeoutHolder th = new TimeoutHolder(timeout);
            do {
                monitor.wait(timeout);
                if (Thread.interrupted()) throw new InterruptedException();

                if ((timeout = th.getTimeoutLeft()) <= 0 && queue.isEmpty()) {
                    return null;
                }
            } while (queue.isEmpty());
            value = queue.removeLast();
            monitor.notify();
        }
        return value;
    }
}
