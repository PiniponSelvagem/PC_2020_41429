package com.piniponselvagem.pcplayground.exams;

import com.piniponselvagem.pcplayground.exams.pc_1920i_2.NaryExchanger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PC_1920i_2 {

    private static class WorkerThread extends Thread {
        private final NaryExchanger<Long> ne;
        private List<Long> list = new LinkedList<Long>();

        public WorkerThread(NaryExchanger<Long> ne) {
            this.ne = ne;
        }

        @Override
        public void run() {
            long thread = Thread.currentThread().getId();

            System.out.println(" --- Start : "+thread);
            try {
                list = ne.exchange(thread, 5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (list == null) {
                list = new LinkedList<Long>();
            }

            System.out.println(" --- End : "+thread+" | list="+Arrays.toString(list.toArray()));
        }
    }
    @Test
    public void testEx2() {
        NaryExchanger<Long> ne = new NaryExchanger<Long>(3);

        WorkerThread t1 = new WorkerThread(ne);
        WorkerThread t2 = new WorkerThread(ne);
        WorkerThread t3 = new WorkerThread(ne);
        WorkerThread t4 = new WorkerThread(ne);

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        try {
            t1.join();
            t2.join();
            t3.join();
            t4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
    private static class WorkerThreadC<T> extends Thread {
        private final BoundedBucket<T> bb;
        private List <T> list = new LinkedList<T>();
        private final int thread;

        public WorkerThreadC(BoundedBucket<T> bb, int thread) {
            this.bb = bb;
            this.thread = thread;
        }

        @Override
        public void run() {
            System.out.println(" --- Start : "+thread);
            try {
                list = bb.takeAll(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (list != null)
                System.out.println(" --- End : "+thread+" | list="+Arrays.toString(list.toArray()));
            else
                System.out.println(" --- End : "+thread+" | list=null");
        }
    }
    @Test
    public void testEx3() throws InterruptedException {
        BoundedBucket<Integer> bb = new BoundedBucket<Integer>(4);

        WorkerThreadC<Integer> t1 = new WorkerThreadC<Integer>(bb, 1);
        WorkerThreadC<Integer> t2 = new WorkerThreadC<Integer>(bb, 2);
        WorkerThreadC<Integer> t3 = new WorkerThreadC<Integer>(bb, 3);
        WorkerThreadC<Integer> t4 = new WorkerThreadC<Integer>(bb, 4);

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        System.out.println(bb.put(1, 1000));
        System.out.println(bb.put(2, 1000));
        System.out.println(bb.put(3, 1000));
        System.out.println(bb.put(4, 1000));

        System.out.println(bb.put(11, 1000));
        System.out.println(bb.put(12, 1000));
        System.out.println(bb.put(13, 1000));
        System.out.println(bb.put(14, 1000));

        Thread.sleep(5000);
        System.out.println(bb.put(21, 1000));
        System.out.println(bb.put(22, 1000));

        try {
            t1.join();
            t2.join();
            t3.join();
            t4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    */
}
