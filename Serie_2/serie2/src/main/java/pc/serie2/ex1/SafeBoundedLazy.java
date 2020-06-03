package pc.serie2.ex1;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SafeBoundedLazy<E> {

    private static class ValueHolder<V> {
        V value;
        int availableLives;

        ValueHolder(V value, int lives) {
            this.value = value;
            availableLives = lives;
        }

        ValueHolder() {
        }
    }

    // Configuration arguments
    private final Supplier<E> supplier;
    private final int lives;

    /**
     * The possible states:
     * null: means UNCREATED
     * CREATING and ERROR: mean exactly that
     * != null && != ERROR && != CREATING: means CREATED
     */
    private final ValueHolder<E> ERROR = new ValueHolder<>();
    private final ValueHolder<E> CREATING = new ValueHolder<>();

    // The current state
    private final AtomicReference<ValueHolder<E>> state = new AtomicReference<>(null);

    // When the synchronizer is in ERROR state, the exception is hold here
    volatile Throwable errorException;

    // Construct a BoundedLazy
    public SafeBoundedLazy(Supplier<E> supplier, int lives) {
        if (lives < 1)
            throw new IllegalArgumentException();
        this.supplier = supplier;
        this.lives = lives;
    }

    // Returns an instance of the underlying type
    public Optional<E> get() throws Throwable {
        while (true) {
            // step 1
            ValueHolder<E> observedState = state.get();

            // step 2 (state == ERROR)
            if (observedState == ERROR) {
                throw errorException;
            }

            // step 2 (state == null)
            if (observedState == null) {
                // step 2.i
                if (state.compareAndSet(observedState, CREATING)) {
                    // Guaranteed to be the only thread creating
                    try {
                        E value = supplier.get();
                        // step 3
                        if (lives > 1) {
                            state.set(new ValueHolder<E>(value, lives - 1));  //lives remaining
                        } else {
                            state.set(null);   // the unique live was consumed
                        }
                        // step 3.i
                        return Optional.of(value);
                    } catch (Throwable ex) {
                        errorException = ex;
                        // step 3.i
                        state.set(ERROR);
                        throw ex;
                    }
                }
            }
            else if (observedState == CREATING) {
                do {
                    Thread.yield();
                } while (state.get()==CREATING); // spin until state != CREATING
            } else { // state is CREATED: we have at least one life
                ValueHolder<E> updatedState = observedState.availableLives > 1
                        ? new ValueHolder<>(observedState.value, observedState.availableLives - 1)
                        : null;
                if (state.compareAndSet(observedState, updatedState))
                    return Optional.of(observedState.value);
            }
        }
    }
}