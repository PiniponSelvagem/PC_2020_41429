package com.piniponselvagem.serie1.ex1;

public class WorkerThread<T> extends Thread {

    private BoundedLazy<T> boundedLazy;
    private int id, timeout;

    public WorkerThread(int id, BoundedLazy<T> boundedLazy, int timeout) {
        this.boundedLazy = boundedLazy;
        this.id = id;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        for (int i=0; i<8; ++i) {
            try {
                String str = boundedLazy.get(timeout).toString();
                String value = str.replace("Optional", "").replace(".", "").replace("[", "").replace("]", "");
                System.out.println("Thread ID: " + id + " ---> " + value);
            } catch (Exception e) {
                System.out.println("Thread ID: " + id + " ---> EXCEPTION: " + e.getClass().getSimpleName());
                break;
            }
        }
    }
}
