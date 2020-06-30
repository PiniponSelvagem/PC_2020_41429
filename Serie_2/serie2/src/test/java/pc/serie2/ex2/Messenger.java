package pc.serie2.ex2;

public abstract class Messenger extends Thread {
    protected final TransferQueue<String> queue;
    protected String message;

    public Messenger(TransferQueue<String> queue) {
        this.queue = queue;
    }

    public String getMessage() {
        return message;
    }
}
