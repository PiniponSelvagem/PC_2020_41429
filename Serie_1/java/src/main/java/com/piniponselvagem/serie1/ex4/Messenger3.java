package com.piniponselvagem.serie1.ex4;

public class Messenger3 extends Thread {

    private final TransferQueue<String> queue;
    private final String prefix = "Thread ID: 3 ---> ";

    public Messenger3(TransferQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            /*
            Thread.sleep(500);

            System.out.println(prefix + "TAKE: timeout = 1000");
            System.out.println(prefix + "TAKE: " + queue.take(1000));
            */
        } catch (Exception e) {
            System.out.println(prefix + "EXCEPTION: " + e.getClass().getSimpleName());
        }
    }
}
