package pc.serie1.ex2;

import pc.serie1.utils.TimeoutHolder;

import java.util.Optional;

public class BroadcastBox<T> {
    private final Object monitor = new Object();

    private static class SharedRequest<T> {
        int waiters;
        T message;
        boolean done;

        SharedRequest() { waiters = 1; }
    }

    private SharedRequest<T> reqQueue = null;
    private boolean signalState = false;

    public int deliverToAll(T message) {
        synchronized (monitor) {
            signalState = true;
            if (reqQueue != null) {
                reqQueue.message = message;
                reqQueue.done = true;
                monitor.notifyAll();
                return reqQueue.waiters;
            }
            return 0;
        }
    }

    public void reset() {
        synchronized (monitor) {
            signalState = false;
            reqQueue = null;		// remove all waiters
        }
    }

    // add a waiter to the queue
    private SharedRequest<T> enqueueWaiter() {
        if (reqQueue == null)
            reqQueue = new SharedRequest<>();
        else
            reqQueue.waiters++;
        return reqQueue;
    }

    // remove a waiter from the queue
    public void removeWaiter() {
        if (--reqQueue.waiters == 0) {
            reset();
        }
    }

    public Optional<T> receive(long timeout) throws InterruptedException {
        synchronized (monitor) {
            if (signalState)
                return Optional.of(reqQueue.message);

            SharedRequest<T> request = enqueueWaiter();

            TimeoutHolder th = new TimeoutHolder(timeout);
            do {
                if ((timeout = th.getTimeoutLeft()) <= 0) {
                    removeWaiter();
                    return Optional.empty();
                }
                try {
                    monitor.wait(timeout);
                } catch (InterruptedException e) {
                    if (!request.done)
                        removeWaiter();
                    throw e;
                }
            } while (!request.done);

            T msg = reqQueue.message;
            removeWaiter();
            return Optional.of(msg);
        }
    }
}