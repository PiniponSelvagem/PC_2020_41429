package pc.serie2.ex2;

public abstract class Messenger extends Thread {
    protected final TransferQueue<String> queue;
    protected final int timeout;
    protected String message;

    public Messenger(TransferQueue<String> queue, int timeout) {
        this.queue = queue;
        this.timeout = timeout;
    }

    public String getMessage() {
        return message;
    }
}
