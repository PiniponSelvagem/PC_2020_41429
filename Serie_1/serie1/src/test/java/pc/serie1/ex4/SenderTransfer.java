package pc.serie1.ex4;

public class SenderTransfer extends Messenger {

    private boolean status;

    public SenderTransfer(TransferQueue<String> queue, int timeout) {
        super(queue, timeout);
    }

    @Override
    public void run() {
        try {
            status = queue.transfer(TransferQueueTest.MESSAGE, timeout);
        } catch (Exception e) {
            System.out.println("Sender -> EXCEPTION: " + e.getClass().getSimpleName());
        }
    }

    public boolean getTransferStatus() {
        return status;
    }
}
