package pc.serie1.ex2;

import java.util.Optional;

public class Client<T> extends Thread {
    private final BroadcastBox<T> broadcastBox;
    private final String prefix;
    private final long timeout;
    private Optional<T> message;

    public Client(int id, long timeout, BroadcastBox<T> broadcastBox) {
        this.broadcastBox = broadcastBox;
        this.timeout = timeout;
        this.prefix = "Thread ID: " + id + " ---> ";
    }

    @Override
    public void run() {
        //try {
        try {
            message = broadcastBox.receive(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //} catch (Exception e) {
            //System.out.println(prefix + "EXCEPTION: " + e.getClass().getSimpleName());
        //}
    }

    public Optional<T> getMessage() {
        return message;
    }
}