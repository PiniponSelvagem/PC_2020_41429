package com.piniponselvagem.pcplayground.exams.pc_1920i_2;

import com.piniponselvagem.pcplayground.utils.TimeoutHolder;

import java.util.*;

public class SynchronousSemaphore {

    private class Request {
        boolean done = false;
    }

    private final Object monitor = new Object();
    private final LinkedList<Request> req = new LinkedList<Request>();
    private int releases = 0;


    public boolean release(int releases, long timeout) throws InterruptedException {
        synchronized (monitor) {
            if (req.isEmpty()) {
                this.releases = releases;
                return true;
            }

            LinkedList<Request> reqWait = new LinkedList<Request>();

            for (Request request : req) {
                request.done = true;
                reqWait.add(request);
            }
            this.releases += releases;
            monitor.notifyAll();

            TimeoutHolder th = new TimeoutHolder(timeout);
            do {
                try {
                    if ((timeout = th.value()) <= 0) {
                        break;
                    }
                    monitor.wait(timeout);
                } catch (InterruptedException ie) {
                    for (Request request : reqWait) {
                        request.done = false;
                    }
                    throw ie;
                }
            } while(!reqWait.isEmpty());

            if (timeout <= 0) {
                for (Request request : reqWait) {
                    request.done = false;
                }
                return false;
            }

            return true;
        }
    }

    public boolean acquire(long timeout) throws InterruptedException {
        synchronized (monitor) {
            if (releases > 0) {
                --releases;
                return true;
            }

            Request request = new Request();
            req.addLast(request);

            TimeoutHolder th = new TimeoutHolder(timeout);

            do {
                try {
                    if ((timeout = th.value()) <= 0) {
                        req.remove(request);
                        return false;
                    }
                    monitor.wait(timeout);
                }
                catch (InterruptedException ie) {
                    req.remove(request);
                    throw ie;
                }
            } while (!request.done);

            req.remove(request);
            --releases;
            return true;
        }
    }
}
