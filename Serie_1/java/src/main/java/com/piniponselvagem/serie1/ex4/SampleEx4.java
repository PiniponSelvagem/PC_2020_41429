package com.piniponselvagem.serie1.ex4;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class SampleEx4 {

    public static void sampleEx4() {
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

        TransferQueue<String> queue = new TransferQueue<>();

        Messenger1 m1 = new Messenger1(queue);
        Messenger2 m2 = new Messenger2(queue);
        Messenger3 m3 = new Messenger3(queue);

        m1.start();
        m2.start();
        m3.start();

        try {
            m1.join();
            m2.join();
            m3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
