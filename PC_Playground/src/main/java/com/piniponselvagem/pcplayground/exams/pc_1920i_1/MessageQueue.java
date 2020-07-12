package com.piniponselvagem.pcplayground.exams.pc_1920i_1;

import com.piniponselvagem.pcplayground.utils.TimeoutHolder;

import java.util.*;


public class MessageQueue<T> {

    private final LinkedList<T> queue = new LinkedList<T>();
    private final Object monitor = new Object();


    public void put(T message) {
        synchronized (monitor) {
            queue.addFirst(message);
            monitor.notify();
        }
    }

    public List<T> get(int nOfMessages, int timeout) throws InterruptedException {
        synchronized (monitor) {
            TimeoutHolder th = new TimeoutHolder(timeout);
            while (queue.size() < nOfMessages) {
                monitor.wait(th.value());

                if (th.value() <= 0) {
                    return null;
                }
            }

            List<T> list = new LinkedList<T>();
            for (int i = 0; i < nOfMessages; ++i) {
                T msg = queue.removeLast();
                list.add(msg);
            }
            return list;
        }
    }
}
