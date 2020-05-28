package pc.serie2.utils;

public class TimeoutHolder {

    private long timeoutLeft;

    private long start = System.currentTimeMillis();

    public TimeoutHolder(long timeout) {
        this.timeoutLeft = timeout;
    }

    public long getTimeoutLeft() {
        return timeoutLeft -= System.currentTimeMillis() - start;
    }
}
