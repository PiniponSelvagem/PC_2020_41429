package pc.serie1.ex2;

public class Server<T> extends Thread {

    private final BroadcastBox<T> broadcastBox;
    private final String prefix;
    private int deliveredCount;
    private T message;

    public Server(int id, BroadcastBox<T> broadcastBox, T message) {
        this.broadcastBox = broadcastBox;
        this.prefix = "Thread ID: " + id + " ---> ";
        this.message = message;
    }

    @Override
    public void run() {
        try {
            deliveredCount = broadcastBox.deliverToAll(message);
        } catch (Exception e) {
            System.out.println(prefix + "EXCEPTION: " + e.getClass().getSimpleName());
        }
    }

    public int getDeliveredCount() {
        return deliveredCount;
    }
}
