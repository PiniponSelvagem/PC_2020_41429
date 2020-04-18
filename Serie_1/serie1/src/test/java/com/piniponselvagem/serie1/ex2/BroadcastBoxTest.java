package com.piniponselvagem.serie1.ex2;

import com.piniponselvagem.serie1.utils.TimeoutHolder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class BroadcastBoxTest {

    @Test
    public void Thread1And2_receiveFrom_Thread3() throws InterruptedException {
        BroadcastBox<String> box = new BroadcastBox<>();
        Server thread3 = new Server(3, box);

        long timeout = 2000;
        Client thread1 = new Client(1, timeout, box);
        Client thread2 = new Client(2, timeout, box);

        thread1.start();
        thread2.start();
        Thread.sleep(timeout/2);
        thread3.start();

        thread1.join();
        thread2.join();
        thread3.join();
        Assert.assertEquals(Optional.of(Server.MESSAGE), thread1.getMessage());
        Assert.assertEquals(Optional.of(Server.MESSAGE), thread2.getMessage());
        Assert.assertEquals(2, thread3.getDeliveredCount());
    }

    @Test
    public void Thread1And2_receive_timeout() throws InterruptedException {
        BroadcastBox<String> box = new BroadcastBox<>();
        long timeout = 2000;
        Client thread1 = new Client(1, timeout, box);
        Client thread2 = new Client(2, timeout, box);

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
        Server thread3 = new Server(3, box);

        thread3.start();

        thread3.join();
        Assert.assertEquals(0, thread3.getDeliveredCount());
    }

    @Test
    public void Thread1_receiveFrom_Thread3_And_Thread2_notReceived() throws InterruptedException {
        BroadcastBox<String> box = new BroadcastBox<>();
        Server thread3 = new Server(3, box);

        long timeout = 2000;
        Client thread1 = new Client(1, timeout, box);
        Client thread2 = new Client(2, timeout, box);

        thread1.start();
        Thread.sleep(timeout/2);
        thread3.start();
        Thread.sleep(timeout);
        thread2.start();

        thread1.join();
        thread2.join();
        thread3.join();
        Assert.assertEquals(Optional.of(Server.MESSAGE), thread1.getMessage());
        Assert.assertEquals(Optional.empty(), thread2.getMessage());
        Assert.assertEquals(1, thread3.getDeliveredCount());
    }
}