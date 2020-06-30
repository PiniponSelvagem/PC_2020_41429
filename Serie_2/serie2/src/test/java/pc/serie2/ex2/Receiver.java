package pc.serie2.ex2;

public class Receiver extends Messenger {

    public Receiver(TransferQueue<String> queue) {
        super(queue);
    }

    @Override
    public void run() {
        try {
            message = queue.take();
            System.out.println(message);
        } catch (Exception e) {
            System.out.println("Receiver -> EXCEPTION: " + e.getClass().getSimpleName());
        }
    }
}