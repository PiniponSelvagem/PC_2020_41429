package com.piniponselvagem.serie1.ex1;

import java.util.Optional;
import java.util.function.Supplier;

import static com.piniponselvagem.serie1.ex1.MonitorState.*;

public class BoundedLazy<T> {

    private final Object monitor = new Object();
    private final int lives;

    private int currLives = 0;
    private MonitorState state = NOTCREATED;
    private Optional<T> value;

    private Supplier<T> supplier;
    private Exception exception;

    public BoundedLazy(Supplier<T> supplier, int lives) {
        this.supplier = supplier;
        this.lives = lives;
    }

    public Optional<T> get(long timeout) throws Exception {
        synchronized (monitor) {
            if (state == NOTCREATED) {
                state = CREATING;
            }
            else if (state == CREATING) {
                long time = System.currentTimeMillis();
                long timeoutLeft = timeout;
                do {
                    monitor.wait(timeoutLeft);
                    timeoutLeft -= System.currentTimeMillis() - time;
                    if (state == CREATING && (timeoutLeft < timeout)) {
                        return Optional.empty();
                    }
                } while (state != CREATED);
            }
            if (state == CREATED) {
                if (currLives > 0) {
                    --currLives;
                    return value;
                }
                state = CREATING;
            }
            if (state == ERROR) {
                throw exception;
            }
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
                state = ERROR;
                monitor.notifyAll();
                exception = ex;
                throw ex;
            } else {
                value = v;
                currLives = lives - 1;    // -1 since this call counts as 1 live
                state = CREATED;
                monitor.notifyAll();
                return value;
            }
        }
    }
}
