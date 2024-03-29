package pc.serie1.ex3;

import pc.serie1.utils.TimeoutHolder;

import java.util.LinkedList;
import java.util.Optional;

public class Exchanger<T> {
    private final Object monitor = new Object();
    private final LinkedList<Request<T>> reqQueue = new LinkedList<>();

    private static class Request<T> {
        T data;
        boolean hasData;
    }


    public Optional<T> exchange(T data, long timeout) throws InterruptedException {
        synchronized (monitor) {
            Request<T> request = new Request<>();
            boolean sentData = false;

            // --- SET ---
            // check if theres requests to add Data and if not add one
            if (!reqQueue.isEmpty()) {
                sentData = sendDataToNextRequest(data);
            }
            reqQueue.add(request);


            // --- WAIT ---
            TimeoutHolder th = new TimeoutHolder(timeout);
            do {
                if ((timeout = th.getTimeoutLeft()) <= 0) {
                    reqQueue.remove(request);
                    return Optional.empty();
                }
                try {
                    monitor.wait(timeout);
                } catch (InterruptedException e) {
                    if (!request.hasData)
                        reqQueue.remove(request);
                    throw e;
                }
            } while (!request.hasData);


            // --- send DATA to next Request
            if (!sentData) {
                sendDataToNextRequest(data);
            }
            return Optional.of(request.data);
        }
    }

    private boolean sendDataToNextRequest(T data) {
        for (Request<T> req : reqQueue) {
            if (!req.hasData) {
                req.data = data;
                req.hasData = true;
                monitor.notify();
                return true;
            }
        }
        return false;
    }
}















    /*
        synchronized (monitor) {
            if (reqQueue.isEmpty()) {
                reqQueue.add(new Request<>());
            } else {
                Request<T> reqOther = reqQueue.peek();
                reqOther.data = data;
                reqOther.hasData = true;
                reqQueue.add(new Request<>());
                monitor.notify();
            }

            monitor.wait(timeout);

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            Optional<T> receivedData = Optional.empty();
            Request<T> reqMy = null;
            for (Request<T> req : reqQueue) {
                if (req.hasData) {
                    receivedData = Optional.of(req.data);
                    reqMy = req;
                }
                else {
                    req.data = data;
                    req.hasData = true;
                    monitor.notify();
                }
            }
            reqQueue.remove(reqMy);

            return receivedData;
        }
     */
