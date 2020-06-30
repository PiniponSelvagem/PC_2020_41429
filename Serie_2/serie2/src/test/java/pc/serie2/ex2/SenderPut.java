package pc.serie2.ex2;

public class SenderPut extends Messenger {
    public SenderPut(TransferQueue<String> queue) {
        super(queue);
    }

    @Override
    public void run() {
        try {
            queue.put("msg 1");
            queue.put("msg 2");
            queue.put("msg 3");
            queue.put("msg 4");
            queue.put("msg 5");
        } catch (Exception e) {
            System.out.println("Sender -> EXCEPTION: " + e.getClass().getSimpleName());
        }
    }
}
