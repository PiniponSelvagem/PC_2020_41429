package com.piniponselvagem.serie1.ex2;

public class Server extends Thread {
    public final static String MESSAGE = "THIS IS A BROADCAST TEST MESSAGE";
    private final BroadcastBox<String> broadcastBox;
    private final String prefix;
    private int deliveredCount;

    public Server(int id, BroadcastBox<String> broadcastBox) {
        this.broadcastBox = broadcastBox;
        this.prefix = "Thread ID: " + id + " ---> ";
    }

    @Override
    public void run() {
        try {
            deliveredCount = broadcastBox.deliverToAll(MESSAGE);
        } catch (Exception e) {
            System.out.println(prefix + "EXCEPTION: " + e.getClass().getSimpleName());
        }
    }

    public int getDeliveredCount() {
        return deliveredCount;
    }
}
