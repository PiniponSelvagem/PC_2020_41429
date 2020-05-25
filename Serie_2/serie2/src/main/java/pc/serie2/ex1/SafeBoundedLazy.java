package pc.serie2.ex1;

import java.util.Optional;
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
    private ValueHolder<E> state = null;

    // When the synchronizer is in ERROR state, the exception is hold here
    Throwable errorException;

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
            if (state == ERROR)
                throw errorException;
            if (state == null) {
                state = CREATING;
                try {
                    E value = supplier.get();
                    if (lives > 1) {
                        state = new ValueHolder<E>(value, lives - 1);  //lives remaining
                    } else {
                        state = null;   // the unique live was consumed
                    }
                    return Optional.of(value);
                } catch (Throwable ex) {
                    errorException = ex;
                    state = ERROR;
                    throw ex;
                }
            } else if (state == CREATING) {
                do {
                    Thread.yield();
                } while (state == CREATING); // spin until state != CREATING
            } else { // state is CREATED: we have at least one life
                Optional<E> retValue = Optional.of(state.value);
                if (--state.availableLives == 0)
                    state = null;
                return retValue;
            }
        }
    }
}