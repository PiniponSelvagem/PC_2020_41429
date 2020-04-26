package pc.serie1.ex3;

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

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            return receivedData;
        }
    }
}
