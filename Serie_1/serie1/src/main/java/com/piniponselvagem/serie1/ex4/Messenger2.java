package com.piniponselvagem.serie1.ex4;

public class Messenger2 extends Thread {

    private final TransferQueue<String> queue;
    private final String prefix = "Thread ID: 2 ---> ";

    public Messenger2(TransferQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            /*
            String msg = "Message from 2 to 1";
            System.out.println(prefix + "PUT:  " + msg);
            queue.put(msg);

            Thread.sleep(100);
            ////////////////////////////////////////////////////

            msg = "Message from 2 to 1, but 3 takes first and 1 times out showing NULL";
            System.out.println(prefix + "PUT:  " + msg);
            queue.put(msg);
            Thread.sleep(1000);

            Thread.sleep(100);
            ////////////////////////////////////////////////////

            msg = "Message from 2 to 1";
            System.out.println(prefix + "TRANSFER: " + msg + " ---> timeout = 2000 ");
            queue.transfer(msg, 2000);

            Thread.sleep(100);
            ////////////////////////////////////////////////////

            System.out.println(prefix + "TRANSFER: " + msg + " ---> timeout = 1000 ");
            queue.transfer(msg, 1000);
            ////////////////////////////////////////////////////
            */

            /*
            Thread.sleep(1000);
            String msg = queue.take(1000);
            System.out.println("Thread ID: " + id + " -> TAKE: " + msg);
            */
        } catch (Exception e) {
            System.out.println(prefix + "EXCEPTION: " + e.getClass().getSimpleName());
        }
    }
}
