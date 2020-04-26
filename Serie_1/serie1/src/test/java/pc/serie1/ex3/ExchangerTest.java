package pc.serie1.ex3;

import pc.serie1.utils.TimeoutHolder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class ExchangerTest {

    @Test
    public void success() throws InterruptedException {
        Exchanger<Integer> exchanger = new Exchanger<>();

        long timeout = 1000;
        DataShareThread<Integer> thread1 = new DataShareThread<>(1, timeout, exchanger, 1111);
        DataShareThread<Integer> thread2 = new DataShareThread<>(2, timeout, exchanger, 2222);

        TimeoutHolder th = new TimeoutHolder(timeout);
        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
        Assert.assertFalse(th.getTimeoutLeft() <= 0);
        Assert.assertEquals(Optional.of(2222), thread1.getSharedData());
        Assert.assertEquals(Optional.of(1111), thread2.getSharedData());
    }

    @Test
    public void timeout() throws InterruptedException {
        Exchanger<Integer> exchanger = new Exchanger<>();

        long timeout = 1000;
        DataShareThread<Integer> thread1 = new DataShareThread<>(1, timeout, exchanger, 1111);

        TimeoutHolder th = new TimeoutHolder(timeout);
        thread1.start();

        thread1.join();
        Assert.assertTrue(th.getTimeoutLeft() <= 0);
        Assert.assertEquals(Optional.empty(), thread1.getSharedData());
    }
}