package pc.serie1.ex4;

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

    @Test
    public void senderTransfer_receiver1Take_inTime() {
        TransferQueue<String> queue = new TransferQueue<>();

        int timeout = 1000;
        SenderTransfer sender = new SenderTransfer(queue, timeout*2);
        Receiver receiver1 = new Receiver(queue, timeout);

        sender.start();
        receiver1.start();

        try {
            sender.join();
            receiver1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(MESSAGE, receiver1.getMessage());
        Assert.assertTrue(sender.getTransferStatus());
    }

    @Test
    public void receiver1Take_senderTransfer_inTime() {
        TransferQueue<String> queue = new TransferQueue<>();

        int timeout = 1000;
        SenderTransfer sender = new SenderTransfer(queue, timeout);
        Receiver receiver1 = new Receiver(queue, timeout);

        receiver1.start();
        sender.start();

        try {
            receiver1.join();
            sender.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertTrue(sender.getTransferStatus());
        Assert.assertEquals(MESSAGE, receiver1.getMessage());
    }
}