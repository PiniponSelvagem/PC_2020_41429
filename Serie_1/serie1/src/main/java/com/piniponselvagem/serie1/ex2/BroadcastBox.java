package com.piniponselvagem.serie1.ex2;

import com.piniponselvagem.serie1.utils.TimeoutHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BroadcastBox<T> {
    private static class Request<T> {
        T message;
        boolean hasMessage;
        boolean isDone;
    }

    private final Object monitor = new Object();
    private final List<Request<T>> reqQueue = new ArrayList<>();


    public int deliverToAll(T message) {
        int nThreads = 0;
        synchronized (monitor) {
            if (reqQueue.isEmpty())
                return nThreads;

            for (Request<T> req : reqQueue) {
                req.message = message;
                req.hasMessage = true;
                ++nThreads;
            }
            monitor.notifyAll();
        }
        return nThreads;
    }

    public Optional<T> receive(long timeout) throws InterruptedException {
        synchronized(monitor) {
            if (Thread.interrupted())
                throw new InterruptedException();

            Request<T> request = new Request<>();
            reqQueue.add(request);

            TimeoutHolder th = new TimeoutHolder(timeout);
            do {
                try {
                    if ((timeout = th.getTimeoutLeft()) <= 0) {
                        reqQueue.remove(request);
                        return Optional.empty();
                    }
                    if (!request.hasMessage) {
                        monitor.wait(timeout);
                    }
                    else {
                        request.isDone = true;
                    }
                } catch (InterruptedException ie) {
                    if (request.isDone) {
                        Thread.currentThread().interrupt();
                        return Optional.of(request.message);
                    }
                    reqQueue.remove(request);
                    throw ie;
                }
            } while (!request.isDone);

            Optional<T> message = Optional.of(request.message);
            reqQueue.remove(request);
            return message;
        }
    }
}
