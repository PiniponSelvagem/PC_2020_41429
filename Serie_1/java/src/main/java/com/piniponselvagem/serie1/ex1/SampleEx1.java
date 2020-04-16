package com.piniponselvagem.serie1.ex1;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class SampleEx1 {

    public static void sampleEx1() {
        Iterable<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8);

        Supplier<Integer> supplier = new Supplier<>() {
            Iterator<Integer> it = list.iterator();

            @Override
            public Integer get() {
                /*
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                */

                if (it.hasNext())
                    return it.next();
                throw new NoSuchElementException();
            }
        };

        /*
        for (int i=0; i<8; ++i) {
            System.out.println(supplier.get());
        }
        */

        BoundedLazy<Integer> boundedLazy = new BoundedLazy<>(supplier, 2);

        WorkerThread<Integer> w1 = new WorkerThread<>(1, boundedLazy, 1000);
        WorkerThread<Integer> w2 = new WorkerThread<>(2, boundedLazy, 1000);
        WorkerThread<Integer> w3 = new WorkerThread<>(3, boundedLazy, 1000);
        WorkerThread<Integer> w4 = new WorkerThread<>(4, boundedLazy, 1000);

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
    }
}
