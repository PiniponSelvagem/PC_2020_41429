package com.piniponselvagem.pcplayground.exams.pc_1920i_2;

import java.util.concurrent.atomic.AtomicLong;

public class CreateOnceSpinLock {

    private AtomicLong state = new AtomicLong(-1L);   // uncreated​

    // called in order to create the shared object
    public boolean tryCreate() {
        while (!state.compareAndSet(0L, 0L)) {
            if (state.compareAndSet(-1L, Thread.currentThread().getId())) {
                return true;    // the current thread must create the shared object
            }
            Thread.yield();
        }

        // the shared object has already been created
        return false;
    }

    // called after a successful creation of the shared object
    public void onCreationSucceeded() {
        long stateValue;

        do {
            stateValue = state.get();

            if (stateValue != Thread.currentThread().getId()) {
                if (state.compareAndSet(stateValue, stateValue)) {
                    throw new IllegalStateException();
                }
            }

            if (!state.compareAndSet(stateValue, 0L)) {
                break;
            }

        } while (true);
    }

    // called after a failed creation of the shared object
    public void onCreationFailed() {
        long stateValue;

        do {
            stateValue = state.get();

            if (stateValue != Thread.currentThread().getId()) {
                if (state.compareAndSet(stateValue, stateValue)) {
                    throw new IllegalStateException();
                }
            }

            if (!state.compareAndSet(stateValue, 1L)) {
                break;
            }

        } while (true);
    }
}