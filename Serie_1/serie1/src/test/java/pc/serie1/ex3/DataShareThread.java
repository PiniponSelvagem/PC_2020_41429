package pc.serie1.ex3;

import java.util.Optional;

public class DataShareThread<T> extends Thread {
    private final Exchanger<T> exchanger;
    private final String prefix;
    private final long timeout;
    private final T data;
    private Optional<T> sharedData;
    private final int id;

    public DataShareThread(int id, long timeout, Exchanger<T> exchanger, T data) {
        this.exchanger = exchanger;
        this.timeout = timeout;
        this.data = data;
        this.id = id;
        this.prefix = "Thread ID: " + id + " ---> ";
    }

    @Override
    public void run() {
        try {
            sharedData = exchanger.exchange(data, timeout);
        } catch (InterruptedException e) {
            System.out.println(prefix + "EXCEPTION: " + e.getClass().getSimpleName());
        }
    }

    public Optional<T> getSharedData() {
        return sharedData;
    }
}