package com.piniponselvagem.pcplayground.exams.pc_1920i_2;

import com.piniponselvagem.pcplayground.utils.TimeoutHolder;

import java.util.*;

public class NaryExchanger<T> {

    private final Object monitor = new Object();

    private final List<T> elems = new LinkedList<T>();
    private final int groupSize;
    private int currGroupSize;
    private boolean isAcceptingElems = true;


    public NaryExchanger(int size) {
        this.groupSize = size;
    }

    public List<T> exchange(T elem, int timeout) throws InterruptedException {
        synchronized (monitor) {
            TimeoutHolder th = new TimeoutHolder(timeout);
            boolean isTimeout = false;


            do {
                if (isAcceptingElems) {
                    elems.add(elem);
                    if (++currGroupSize == groupSize) {
                        isAcceptingElems = false;
                    }
                    break;
                }

                monitor.wait(timeout);
                isTimeout = th.value() <= 0;

            } while (isTimeout);

            if (isTimeout) {
                monitor.notify();
                return null;
            }


            while (isAcceptingElems) {
                monitor.wait(timeout);
                isTimeout = th.value() <= 0;

                if (isTimeout) break;
            }

            if (isTimeout) {
                elems.remove(elem);
                if (--currGroupSize == 0) {
                    isAcceptingElems = true;
                }
                monitor.notify();
                return null;
            }


            List<T> ret = new LinkedList<T>(elems);
            if (--currGroupSize == 0) {
                isAcceptingElems = true;
            }
            monitor.notify();
            return ret;
        }
    }
}
