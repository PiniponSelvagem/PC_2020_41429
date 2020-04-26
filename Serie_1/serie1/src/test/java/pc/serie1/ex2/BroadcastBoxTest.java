package pc.serie1.ex2;

import pc.serie1.utils.TimeoutHolder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class BroadcastBoxTest {

    private final String MESSAGE = "THIS IS A BROADCAST TEST MESSAGE";

    @Test
    public void Thread1And2_receiveFrom_Thread3() throws InterruptedException {
        BroadcastBox<String> box = new BroadcastBox<>();
        Server<String> thread3 = new Server<>(3, box, MESSAGE);

        long timeout = 2000;
        Client<String> thread1 = new Client<>(1, timeout, box);
        Client<String> thread2 = new Client<>(2, timeout, box);

        thread1.start();
        thread2.start();
        Thread.sleep(timeout/2);
        thread3.start();

        thread1.join();
        thread2.join();
        thread3.join();
        Assert.assertEquals(Optional.of(MESSAGE), thread1.getMessage());
        Assert.assertEquals(Optional.of(MESSAGE), thread2.getMessage());
        Assert.assertEquals(2, thread3.getDeliveredCount());
    }

    @Test
    public void Thread1And2_receive_timeout() throws InterruptedException {
        BroadcastBox<String> box = new BroadcastBox<>();
        long timeout = 2000;
        Client<String> thread1 = new Client<>(1, timeout, box);
        Client<String> thread2 = new Client<>(2, timeout, box);

        TimeoutHolder th = new TimeoutHolder(timeout);
        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
        Assert.assertTrue(th.getTimeoutLeft() <= 0);
        Assert.assertEquals(Optional.empty(), thread1.getMessage());
        Assert.assertEquals(Optional.empty(), thread2.getMessage());
    }

    @Test
    public void Thread3_notDeliver() throws InterruptedException {
        BroadcastBox<String> box = new BroadcastBox<>();
        Server<String> thread3 = new Server<>(3, box, MESSAGE);

        thread3.start();

        thread3.join();
        Assert.assertEquals(0, thread3.getDeliveredCount());
    }

    @Test
    public void Thread1_receiveFrom_Thread3_And_Thread2_notReceived() throws InterruptedException {
        BroadcastBox<String> box = new BroadcastBox<>();
        Server<String> thread3 = new Server<>(3, box, MESSAGE);

        long timeout = 2000;
        Client<String> thread1 = new Client<>(1, timeout, box);
        Client<String> thread2 = new Client<>(2, timeout, box);

        thread1.start();
        Thread.sleep(timeout/10);
        thread3.start();
        Thread.sleep(timeout);
        thread2.start();

        thread1.join();
        thread3.join();
        thread2.join();
        Assert.assertEquals(Optional.of(MESSAGE), thread1.getMessage());
        Assert.assertEquals(Optional.empty(), thread2.getMessage());
        Assert.assertEquals(1, thread3.getDeliveredCount());
    }
}