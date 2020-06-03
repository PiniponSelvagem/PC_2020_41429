package pc.serie2.ex2;

public class Receiver extends Messenger {

    public Receiver(TransferQueue<String> queue, int timeout) {
        super(queue, timeout);
    }

    @Override
    public void run() {
        try {
            message = queue.take(timeout);
        } catch (Exception e) {
            System.out.println("Receiver -> EXCEPTION: " + e.getClass().getSimpleName());
        }
    }
}