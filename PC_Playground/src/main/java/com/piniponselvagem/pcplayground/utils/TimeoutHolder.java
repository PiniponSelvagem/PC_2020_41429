package com.piniponselvagem.pcplayground.utils;

import java.util.concurrent.TimeUnit;

public class TimeoutHolder {
    private final long deadline;		// timeout deadline: non-zero if timed

    public TimeoutHolder(long millis) {
        deadline = millis > 0L ? System.currentTimeMillis() + millis: 0L;
    }

    public TimeoutHolder(long time, TimeUnit unit) {
        deadline = time > 0L ? System.currentTimeMillis() + unit.toMillis(time) : 0L;
    }

    // returns true if a timeout was defined
    public boolean isTimed() { return deadline != 0L; }

    // returns the remaining timeout
    public long value() {
        if (deadline == 0L)
            return Long.MAX_VALUE;
        long remainder = deadline - System.currentTimeMillis();
        return remainder > 0L ? remainder : 0L;
    }
}
