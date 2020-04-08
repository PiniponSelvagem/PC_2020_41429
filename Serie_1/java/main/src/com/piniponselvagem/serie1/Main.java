package com.piniponselvagem.serie1;

import com.piniponselvagem.serie1.ex1.BoundedLazy;
import com.piniponselvagem.serie1.ex1.Worker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Supplier;

public class Main {

    private static Iterable<Integer> list = new ArrayList() {{
        add(1); add(2); add(3); add(4); add(5); add(6); add(7); add(8);
    }};
    private static BoundedLazy<Integer> boundedLazy;

    public static void main(String[] args) {
        Supplier<Integer> supplier = new Supplier<Integer>() {
            Iterator<Integer> it = list.iterator();

            @Override
            public Integer get() {
                if (it.hasNext())
                    return it.next();
                return -1;
            }
        };

        /*
        for (int i=0; i<8; ++i) {
            System.out.println(supplier.get());
        }
        */

        boundedLazy = new BoundedLazy<>(supplier, 4);

        Worker<Integer> w1 = new Worker<>(boundedLazy, 1);
        Worker<Integer> w2 = new Worker<>(boundedLazy, 2);
        Worker<Integer> w3 = new Worker<>(boundedLazy, 3);

        w1.start();
        w2.start();
        w3.start();
    }
}
