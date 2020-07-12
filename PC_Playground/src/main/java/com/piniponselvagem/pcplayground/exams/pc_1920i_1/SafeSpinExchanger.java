package com.piniponselvagem.pcplayground.exams.pc_1920i_1;

import com.piniponselvagem.pcplayground.utils.TimeoutHolder;

import java.util.concurrent.atomic.AtomicReference;

public class SafeSpinExchanger<T> {
    private class Holder {
        T first, second;
        boolean done;
        Holder(T f) { first = f; }
    }


    private AtomicReference<Holder> xchg = new AtomicReference<Holder>(null);     // the exchange spot

    public T exchange(T myData, long timeout) {
        while (true) {  // ADDED
            Holder h = new Holder(myData);

            if (xchg.compareAndSet(null, h)) {  // CHANGED
                TimeoutHolder th = new TimeoutHolder(timeout);
                while (!h.done && th.value() > 0) {
                    Thread.yield();
                }
                if (h.done) return h.second;
                if (xchg.compareAndSet(h, null)) {  // CHANGED
                    return null;
                }
                while (!h.done) {
                    Thread.yield();
                }
                return h.second;
            } else {
                h = xchg.get(); // ADDED
                if (xchg.compareAndSet(h, null)) {  // CHANGED
                    h.second = myData;
                    h.done = true;
                    return h.first;
                }
            }
        }
    }

    /*
    if (xchg == null) {
        Holder h = new Holder(myData);
        xchg = h;
        TimeoutHolder th = new TimeoutHolder(timeout);
        while (!h.done && th.value() > 0) Thread.yield();
        if (h.done) return h.second;
        if (xchg == h) {
            xchg = null;
            return null;
        }
        while (!h.done) Thread.yield();
        return h.second;
    } else {
        Holder h = xchg;
        xchg = null;
        h.second = myData;
        h.done = true;
        return h.first;
    }
    */

    private final Object monitor = new Object();
    private Holder xchg_m = null;
    public T exchange_monitorStyle(T myData, long timeout) throws InterruptedException {

        synchronized (monitor) {
            if (xchg_m == null) {
                Holder h = new Holder(myData);
                xchg_m = h;
                TimeoutHolder th = new TimeoutHolder(timeout);
                while (!h.done && th.value() > 0) {
                    monitor.wait();
                    //Thread.yield();
                }
                if (h.done) return h.second;
                if (xchg_m == h) {
                    xchg_m = null;
                    return null;
                }
                while (!h.done) {
                    monitor.wait();
                    //Thread.yield();
                }
                return h.second;
            }
            else {
                Holder h = xchg_m;
                xchg_m = null;
                h.second = myData;
                h.done = true;
                monitor.notify();
                return h.first;
            }
        }
    }
}
