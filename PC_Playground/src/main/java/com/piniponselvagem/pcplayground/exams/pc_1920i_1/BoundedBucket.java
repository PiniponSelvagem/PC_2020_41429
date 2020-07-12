package com.piniponselvagem.pcplayground.exams.pc_1920i_1;

import com.piniponselvagem.pcplayground.utils.TimeoutHolder;

import java.util.*;

public class BoundedBucket<T> {
    private final Object monitor = new Object();
    private final LinkedList<T> bucket = new LinkedList<T>();
    private final int capacity;

    public BoundedBucket(int capacity) {
        this.capacity = capacity;
    }

    public boolean put(T item, long timeout) throws InterruptedException {
        synchronized (monitor) {
            TimeoutHolder th = new TimeoutHolder(timeout);
            while (th.value() > 0 && bucket.size() >= capacity) {
                monitor.wait(th.value());
            }

            if (th.value() <= 0)
                return false;

            bucket.add(item);
            monitor.notify();
            return true;
        }
    }

    public List<T> takeAll(long timeout) throws InterruptedException {
        synchronized (monitor) {
            TimeoutHolder th = new TimeoutHolder(timeout);
            while (th.value() > 0 && bucket.isEmpty()) {
                monitor.wait(th.value());
            }

            if (th.value() <= 0)
                return null;

            List<T> list = new LinkedList<T>();
            while (!bucket.isEmpty()) {
                list.add(bucket.remove());
            }
            monitor.notify();
            return list;
        }
    }
}
