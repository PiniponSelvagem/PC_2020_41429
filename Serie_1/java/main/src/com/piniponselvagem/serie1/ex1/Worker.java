package com.piniponselvagem.serie1.ex1;

public class Worker<T> extends Thread {

    private BoundedLazy<T> boundedLazy;
    private int id;

    public Worker(BoundedLazy<T> boundedLazy, int id) {
        this.boundedLazy = boundedLazy;
        this.id = id;
    }

    @Override
    public void run() {
        for (int i=0; i<4; ++i) {
            try {
                System.out.println("ID: " + id + " - " + boundedLazy.get(1000));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
