package com.piniponselvagem.serie1.ex2;

import java.util.Optional;

public class Client extends Thread {
    private final BroadcastBox<String> broadcastBox;
    private final String prefix;
    private final long timeout;
    private Optional<String> message;

    public Client(int id, long timeout, BroadcastBox<String> broadcastBox) {
        this.broadcastBox = broadcastBox;
        this.timeout = timeout;
        this.prefix = "Thread ID: " + id + " ---> ";
    }

    @Override
    public void run() {
        try {
            message = broadcastBox.receive(timeout);
        } catch (Exception e) {
            System.out.println(prefix + "EXCEPTION: " + e.getClass().getSimpleName());
        }
    }

    public Optional<String> getMessage() {
        return message;
    }
}