package pc.serie2.ex2;

import org.junit.Assert;
import org.junit.Test;

public class TransferQueueTest {

    protected static String MESSAGE = "Sample Message";

    @Test
    public void senderPut_receiverTake_inTime() {
        TransferQueue<String> queue = new TransferQueue<>();

        int timeout = 1000;
        SenderPut sender = new SenderPut(queue, timeout);
        Receiver receiver = new Receiver(queue, timeout);

        sender.start();
        receiver.start();

        try {
            sender.join();
            receiver.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(MESSAGE, receiver.getMessage());
    }

    @Test
    public void receiverTake_timeout() {
        TransferQueue<String> queue = new TransferQueue<>();

        int timeout = 1000;
        Receiver receiver = new Receiver(queue, timeout);

        receiver.start();

        try {
            receiver.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertNull(receiver.getMessage());
    }

    @Test
    public void senderPut_receiver1Take_receiver2NoTake() throws InterruptedException {
        TransferQueue<String> queue = new TransferQueue<>();

        int timeout = 1000;
        SenderPut sender = new SenderPut(queue, timeout);
        Receiver receiver1 = new Receiver(queue, timeout);
        Receiver receiver2 = new Receiver(queue, timeout);

        sender.start();
        receiver1.start();
        Thread.sleep(timeout/10);
        receiver2.start();

        try {
            sender.join();
            receiver1.join();
            receiver2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(MESSAGE, receiver1.getMessage());
        Assert.assertNull(receiver2.getMessage());
    }
}