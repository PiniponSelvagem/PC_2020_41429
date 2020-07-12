package com.piniponselvagem.pcplayground.exams;

import com.piniponselvagem.pcplayground.exams.pc_1920i_1.BoundedBucket;
import com.piniponselvagem.pcplayground.exams.pc_1920i_1.MessageQueue;
import com.piniponselvagem.pcplayground.exams.pc_1920i_1.SafeSpinExchanger;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class PC_1920i_1 {

    private static class WorkerThread<T> extends Thread {
        private final SafeSpinExchanger<T> sse;
        private T myData, result;

        public WorkerThread(SafeSpinExchanger<T> sse, T myData) {
            this.sse = sse;
            this.myData = myData;
        }

        @Override
        public void run() {
            System.out.println(" --- Start : myData="+myData);
            result = sse.exchange(myData, 1000);
            System.out.println(" --- End : myData="+myData+" | result="+result);
        }

        public T getResult() {
            return result;
        }
    }
    @Test
    public void testEx1() {
        SafeSpinExchanger<Integer> sse = new SafeSpinExchanger<Integer>();

        WorkerThread<Integer> t1 = new WorkerThread<Integer>(sse, 1);
        WorkerThread<Integer> t2 = new WorkerThread<Integer>(sse, 2);
        WorkerThread<Integer> t3 = new WorkerThread<Integer>(sse, 3);
        WorkerThread<Integer> t4 = new WorkerThread<Integer>(sse, 4);

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

        int sumActual = t1.getResult()
                + t2.getResult()
                + t3.getResult()
                + t4.getResult();

        Assert.assertEquals(10, sumActual);
    }


    private static class WorkerThreadB<T> extends Thread {
        private final MessageQueue<T> mq;
        private List <T> list = new LinkedList<T>();
        private final int thread;

        public WorkerThreadB(MessageQueue<T> mq, int thread) {
            this.mq = mq;
            this.thread = thread;
        }

        @Override
        public void run() {
            System.out.println(" --- Start : "+thread);
            try {
                list = mq.get(thread, 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(" --- End : "+thread+" | list="+Arrays.toString(list.toArray()));
        }
    }
    @Test
    public void testEx2() {
        MessageQueue<Integer> mq = new MessageQueue<Integer>();

        WorkerThreadB<Integer> t1 = new WorkerThreadB<Integer>(mq, 1);
        WorkerThreadB<Integer> t2 = new WorkerThreadB<Integer>(mq, 2);
        WorkerThreadB<Integer> t3 = new WorkerThreadB<Integer>(mq, 3);
        WorkerThreadB<Integer> t4 = new WorkerThreadB<Integer>(mq, 4);

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        mq.put(100);

        mq.put(200);
        mq.put(201);

        mq.put(300);
        mq.put(301);
        mq.put(302);

        mq.put(400);
        mq.put(401);
        mq.put(402);
        mq.put(403);

        try {
            t1.join();
            t2.join();
            t3.join();
            t4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


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
}
