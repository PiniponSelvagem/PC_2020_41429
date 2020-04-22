package com.piniponselvagem.serie1.ex2;

import com.piniponselvagem.serie1.utils.TimeoutHolder;

import java.util.Optional;

public class BroadcastBox<T> {

    /**
     * NOTE:
     *      It is known that if the deliver thread sends a new broadcast
     *      without all threads receiving in time, it will lose the first broadcast.
     *      That's an intended behaviour, reinforcing the fact that the current last
     *      broadcast is the important one and others should be discarded.
     */


    private static class Request<T> {
        T message;
        int nWaiters;
        boolean hasMessage;
    }

    private final Object monitor = new Object();
    private final Request<T> request = new Request<>();


    public int deliverToAll(T message) {
        synchronized (monitor) {
            if (request.nWaiters != 0) {
                request.message = message;
                request.hasMessage = true;
                monitor.notifyAll();
            }
            return request.nWaiters;
        }
    }

    public Optional<T> receive(long timeout) throws InterruptedException {
        synchronized(monitor) {
            if (Thread.interrupted())
                throw new InterruptedException();

            ++request.nWaiters;
            TimeoutHolder th = new TimeoutHolder(timeout);

            do {
                try {
                    if ((timeout = th.getTimeoutLeft()) <= 0) {
                        checkIfNoWaitersAndAffectHasMessage();
                        return Optional.empty();
                    }
                    if (!request.hasMessage) {
                        monitor.wait(timeout);
                    }
                } catch (InterruptedException ie) {
                    if (request.hasMessage) {
                        Thread.currentThread().interrupt();
                        checkIfNoWaitersAndAffectHasMessage();
                        return Optional.of(request.message);
                    }
                    throw ie;
                }
            } while (!request.hasMessage);

            checkIfNoWaitersAndAffectHasMessage();
            return Optional.of(request.message);
        }
    }

    private void checkIfNoWaitersAndAffectHasMessage() {
        if (--request.nWaiters == 0) {
            request.hasMessage = false;
        }
    }
}
