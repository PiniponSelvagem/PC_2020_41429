package com.piniponselvagem.serie1.ex4;

import java.util.LinkedList;

public class TransferQueue<T> {

    private final Object monitor = new Object();
    private final LinkedList<T> queue = new LinkedList<>();

    public TransferQueue() {

        // TODO: Ver blackboard-10.md

        // TODO: Tests

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

            long time = System.currentTimeMillis();
            long timeoutLeft = timeout;
            do {
                monitor.wait(timeoutLeft);
                if (Thread.interrupted()) {
                    queue.remove(message);
                    throw new InterruptedException();
                }

                timeoutLeft -= System.currentTimeMillis() - time;
                if (timeoutLeft <= 0) {
                    queue.remove(message);
                    return false;
                }
            } while (queue.contains(message));
        }
        return true;
    }

    public T take(int timeout) throws InterruptedException {
        T value;
        synchronized (monitor) {
            long time = System.currentTimeMillis();
            long timeoutLeft = timeout;
            do {
                monitor.wait(timeoutLeft);
                if (Thread.interrupted()) throw new InterruptedException();

                timeoutLeft -= System.currentTimeMillis() - time;
                if (timeoutLeft <= 0 && queue.isEmpty()) {
                    return null;
                }
            } while (queue.isEmpty());
            value = queue.removeLast();
            monitor.notify();
        }
        return value;
    }
}
