package com.piniponselvagem.serie1.ex4;

public class Messenger1 extends Thread {

    private final TransferQueue<String> queue;
    private final String prefix = "Thread ID: 1 ---> ";

    public Messenger1(TransferQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            /*
            System.out.println(prefix + "TAKE: timeout = 1000");
            String msg = queue.take(1000);
            System.out.println(prefix + "TAKE: " + msg);

            System.out.println();
            Thread.sleep(100);
            /////////////////////////////////////////////////////

            System.out.println(prefix + "SLEEPING FOR 2000");
            Thread.sleep(2000);
            System.out.println(prefix + "TAKE: timeout = 1000");
            System.out.println(prefix + "TAKE: " + queue.take(1000));

            System.out.println();
            Thread.sleep(150);
            ////////////////////////////////////////////////////

            System.out.println(prefix + "SLEEPING FOR 1000");
            Thread.sleep(1000);
            System.out.println(prefix + "TAKE: timeout = 500");
            System.out.println(prefix + "TAKE: " + queue.take(1000));

            System.out.println();
            Thread.sleep(100);
            ////////////////////////////////////////////////////


            */

            //queue.put(msg);
            //System.out.println("Thread ID: " + id + " -> PUT:  " + msg);
            //boolean ret = queue.transfer(msg, 10000);
            //System.out.println("Thread ID: 1 ---> TRNF: " + msg + " RESULT: " + ret);
            //System.out.println("Thread ID: " + id + " -> TAKE: " + queue.take(timeout));
        } catch (Exception e) {
            System.out.println(prefix + "EXCEPTION: " + e.getClass().getSimpleName());
        }
    }
}
