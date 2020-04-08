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
            String value = null;

            try {
                String str = boundedLazy.get(1000).toString();
                value = str.replace("Optional", "").replace(".", "").replace("[", "").replace("]", "");
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Thread ID: " + id + " ---> " + value);
        }
    }
}
