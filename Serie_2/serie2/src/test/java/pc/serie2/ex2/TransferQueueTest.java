package pc.serie2.ex2;

import org.junit.Test;

public class TransferQueueTest {

    @Test
    public void senderPut_receiverTake_inTime() {
        TransferQueue<String> queue = new TransferQueue<>();

        SenderPut sender = new SenderPut(queue);
        Receiver receiver = new Receiver(queue);
        Receiver receiver2 = new Receiver(queue);
        Receiver receiver3 = new Receiver(queue);
        Receiver receiver4 = new Receiver(queue);
        Receiver receiver5 = new Receiver(queue);

        sender.start();
        receiver.start();
        receiver2.start();
        receiver3.start();
        receiver4.start();
        receiver5.start();

        try {
            sender.join();
            receiver.join();
            receiver2.join();
            receiver3.join();
            receiver4.join();
            receiver5.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Assert.assertEquals(MESSAGE, receiver.getMessage());
        System.out.println("THIS TEST ONLY PRINTS RESULTS, DOES NOT ASSERT ANYTHING ATM");
    }




    /**
     * Tests not testing all possible cases correctly
     *
     *
    protected static String MESSAGE = "Sample Message";

    @Test
    public void senderPut_receiverTake_inTime() {
        TransferQueue<String> queue = new TransferQueue<>();

        SenderPut sender = new SenderPut(queue);
        Receiver receiver = new Receiver(queue);

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
    public void senderPut_receiver1Take_receiver2NoTake() throws InterruptedException {
        TransferQueue<String> queue = new TransferQueue<>();

        int th2startDelay = 100;
        SenderPut sender = new SenderPut(queue);
        Receiver receiver1 = new Receiver(queue);
        Receiver receiver2 = new Receiver(queue);

        sender.start();
        receiver1.start();
        Thread.sleep(th2startDelay);
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
    */
}