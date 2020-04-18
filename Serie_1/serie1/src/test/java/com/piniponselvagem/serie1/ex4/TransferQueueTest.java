package com.piniponselvagem.serie1.ex4;

import com.piniponselvagem.serie1.ex1.BoundedLazy;
import com.piniponselvagem.serie1.ex1.WorkerThread;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class TransferQueueTest {

    private static final Iterable<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8);
    private static int sumExpected;

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
        int dup = 2;
        BoundedLazy<Integer> boundedLazy = new BoundedLazy<>(supplierInstant, dup);

        WorkerThread w1 = new WorkerThread(1, boundedLazy, 4, 1000);
        WorkerThread w2 = new WorkerThread(2, boundedLazy, 4, 1000);
        WorkerThread w3 = new WorkerThread(3, boundedLazy, 4, 1000);
        WorkerThread w4 = new WorkerThread(4, boundedLazy, 4, 1000);

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

        Assert.assertEquals(sumExpected * dup, sumActual);
    }

    @Test
    public void getConcurrentValues_WithSleep() {
        int dup = 2;
        BoundedLazy<Integer> boundedLazy = new BoundedLazy<>(supplierWithSleep, dup);

        WorkerThread w1 = new WorkerThread(1, boundedLazy, 4, 1000);
        WorkerThread w2 = new WorkerThread(2, boundedLazy, 4, 1000);
        WorkerThread w3 = new WorkerThread(3, boundedLazy, 4, 1000);
        WorkerThread w4 = new WorkerThread(4, boundedLazy, 4, 1000);

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

        Assert.assertEquals(sumExpected * dup, sumActual);
    }

    @Test
    public void shouldThrowNoSuchElementException() {
        int dup = 2;
        BoundedLazy<Integer> boundedLazy = new BoundedLazy<>(supplierThrowEx, dup);

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

        Assert.assertEquals(NoSuchElementException.class, w1.getException().getClass());
        Assert.assertEquals(NoSuchElementException.class, w2.getException().getClass());
    }
}