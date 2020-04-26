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

        Assert.assertEquals(Optional.of(2222), thread1.getSharedData());
        Assert.assertEquals(Optional.of(1111), thread2.getSharedData());
        Assert.assertFalse(th.getTimeoutLeft() <= 0);
    }

    @Test
    public void timeout() throws InterruptedException {
        Exchanger<Integer> exchanger = new Exchanger<>();

        long timeout = 1000;
        DataShareThread<Integer> thread1 = new DataShareThread<>(1, timeout, exchanger, 1111);

        TimeoutHolder th = new TimeoutHolder(timeout);
        thread1.start();

        thread1.join();
        Assert.assertEquals(Optional.empty(), thread1.getSharedData());
        Assert.assertTrue(th.getTimeoutLeft() <= 0);
    }

    @Test
    public void situationOf_3rdThread() throws InterruptedException {
        Exchanger<Integer> exchanger = new Exchanger<>();

        int valueT1 = 1111, valueT2 = 2222, valueT3 = 3333, valueT4 = 4444;

        long timeout = 1000;
        DataShareThread<Integer> thread1 = new DataShareThread<>(1, timeout, exchanger, valueT1);
        DataShareThread<Integer> thread2 = new DataShareThread<>(2, timeout, exchanger, valueT2);
        DataShareThread<Integer> thread3 = new DataShareThread<>(3, timeout, exchanger, valueT3);
        DataShareThread<Integer> thread4 = new DataShareThread<>(4, timeout, exchanger, valueT4);

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

        System.out.println("T1 send: "+valueT1+" | received: "+thread1.getSharedData());
        System.out.println("T2 send: "+valueT2+" | received: "+thread2.getSharedData());
        System.out.println("T3 send: "+valueT3+" | received: "+thread3.getSharedData());
        System.out.println("T4 send: "+valueT4+" | received: "+thread4.getSharedData());

        int[] array = new int[] {
                thread1.getSharedData().orElse(0),
                thread2.getSharedData().orElse(0),
                thread3.getSharedData().orElse(0),
                thread4.getSharedData().orElse(0)
        };

        if (array[0] == array[1] || array[0] == array[2] || array[0] == array[3]) Assert.fail();
        if (array[1] == array[2] || array[1] == array[3]) Assert.fail();
        if (array[2] == array[3]) Assert.fail();
    }
}