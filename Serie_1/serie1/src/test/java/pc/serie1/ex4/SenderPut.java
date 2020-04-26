package pc.serie1.ex4;

public class SenderPut extends Messenger {

    public SenderPut(TransferQueue<String> queue, int timeout) {
        super(queue, timeout);
    }

    @Override
    public void run() {
        try {
            queue.put(TransferQueueTest.MESSAGE);
        } catch (Exception e) {
            System.out.println("Sender -> EXCEPTION: " + e.getClass().getSimpleName());
        }
    }
}
