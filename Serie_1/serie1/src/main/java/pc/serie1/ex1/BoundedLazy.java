package pc.serie1.ex1;

import pc.serie1.utils.TimeoutHolder;

import java.util.Optional;
import java.util.function.Supplier;

public class BoundedLazy<T> {

    private final Object monitor = new Object();
    private final int lives;

    private int currLives = 0;
    private MonitorState state = MonitorState.NOTCREATED;
    private Optional<T> value;

    private Supplier<T> supplier;
    private Exception exception;

    public BoundedLazy(Supplier<T> supplier, int lives) {
        this.supplier = supplier;
        this.lives = lives;
    }

    public Optional<T> get(long timeout) throws Exception {
        synchronized (monitor) {
            if (state == MonitorState.NOTCREATED) {
                state = MonitorState.CREATING;
            }
            else if (state == MonitorState.CREATING) {
                TimeoutHolder th = new TimeoutHolder(timeout);
                do {
                    monitor.wait(timeout);
                    if (state == MonitorState.ERROR) throw exception;
                    if (state == MonitorState.CREATING && (timeout = th.getTimeoutLeft()) <= 0) return Optional.empty();
                } while (state != MonitorState.CREATED);
            }
            if (state == MonitorState.CREATED) {
                if (currLives > 0) {
                    --currLives;
                    return value;
                }
                state = MonitorState.CREATING;
            }
            if (state == MonitorState.ERROR) throw exception;
        }

        Optional<T> v = Optional.empty();
        Exception ex = null;
        try {
            v = Optional.of(supplier.get());
        } catch (Exception e) {
            ex = e;
        }

        synchronized (monitor) {
            if (ex != null) {
                state = MonitorState.ERROR;
                monitor.notifyAll();
                exception = ex;
                throw ex;
            } else {
                value = v;
                currLives = lives - 1;    // -1 since this call counts as 1 live
                state = MonitorState.CREATED;
                monitor.notifyAll();
                return value;
            }
        }
    }
}
