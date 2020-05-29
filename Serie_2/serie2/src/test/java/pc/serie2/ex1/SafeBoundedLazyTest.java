package pc.serie2.ex1;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class SafeBoundedLazyTest {

    private static final Iterable<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
    private static int sumExpected;

    private static final int dupValues = 2;
    private static final int threadCycles = dupValues * 4;


    @BeforeClass
    public static void init() {
        for (int value : list) {
            sumExpected += value;
        }
    }

    private Supplier<Integer> supplierInstant = new Supplier<>() {
        private Iterator<Integer> it = list.iterator();

        @Override
        public Integer get() {
            if (it.hasNext())
                return it.next();
            throw new NoSuchElementException();
        }
    };
    private Supplier<Integer> supplierWithSleep = new Supplier<>() {
        private Iterator<Integer> it = list.iterator();

        @Override
        public Integer get() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (it.hasNext())
                return it.next();
            throw new NoSuchElementException();
        }
    };
    private Supplier<Integer> supplierThrowEx = () -> {
        throw new NoSuchElementException();
    };



    @Test
    public void getConcurrentValues() {
        SafeBoundedLazy<Integer> boundedLazy = new SafeBoundedLazy<>(supplierInstant, dupValues);

        WorkerThread w1 = new WorkerThread(1, boundedLazy, threadCycles, 1000);
        WorkerThread w2 = new WorkerThread(2, boundedLazy, threadCycles, 1000);
        WorkerThread w3 = new WorkerThread(3, boundedLazy, threadCycles, 1000);
        WorkerThread w4 = new WorkerThread(4, boundedLazy, threadCycles, 1000);

        w1.start();
        w2.start();
        w3.start();
        w4.start();

        try {
            w1.join();
            w2.join();
            w3.join();
            w4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int sumActual = w1.getSum().orElse(0) +
                        w2.getSum().orElse(0) +
                        w3.getSum().orElse(0) +
                        w4.getSum().orElse(0);

        Assert.assertEquals(sumExpected * dupValues, sumActual);
    }

    @Test
    public void getConcurrentValues_WithSleep() {
        SafeBoundedLazy<Integer> boundedLazy = new SafeBoundedLazy<>(supplierWithSleep, dupValues);

        WorkerThread w1 = new WorkerThread(1, boundedLazy, threadCycles, 1000);
        WorkerThread w2 = new WorkerThread(2, boundedLazy, threadCycles, 1000);
        WorkerThread w3 = new WorkerThread(3, boundedLazy, threadCycles, 1000);
        WorkerThread w4 = new WorkerThread(4, boundedLazy, threadCycles, 1000);

        w1.start();
        w2.start();
        w3.start();
        w4.start();

        try {
            w1.join();
            w2.join();
            w3.join();
            w4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int sumActual = w1.getSum().orElse(0) +
                w2.getSum().orElse(0) +
                w3.getSum().orElse(0) +
                w4.getSum().orElse(0);

        Assert.assertEquals(sumExpected * dupValues, sumActual);
    }

    @Test
    public void shouldThrowNoSuchElementException() {
        SafeBoundedLazy<Integer> boundedLazy = new SafeBoundedLazy<>(supplierThrowEx, dupValues);

        WorkerThread w1 = new WorkerThread(1, boundedLazy, Integer.MAX_VALUE, 1000);
        WorkerThread w2 = new WorkerThread(2, boundedLazy, Integer.MAX_VALUE, 1000);

        w1.start();
        w2.start();

        try {
            w1.join();
            w2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(NoSuchElementException.class, w1.getThrowable().getClass());
        Assert.assertEquals(NoSuchElementException.class, w2.getThrowable().getClass());
    }
}