package pc.serie1.ex1;

import java.util.Optional;

public class WorkerThread extends Thread {

    private BoundedLazy<Integer> boundedLazy;
    private final int id, cycles, timeout;
    private Optional<Integer> sumTotal = Optional.of(0);
    private Exception ex;

    public WorkerThread(int id, BoundedLazy<Integer> boundedLazy, int cycles, int timeout) {
        this.boundedLazy = boundedLazy;
        this.id = id;
        this.cycles = cycles;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        for (int i=0; i<cycles; ) {
            try {
                Optional<Integer> value = boundedLazy.get(timeout);
                if (value.isPresent()) ++i;

                System.out.println("Thread ID: " + id + " ---> " + value);

                sumTotal = Optional.of(sumTotal.get() + value.orElse(0));
            } catch (Exception e) {
                System.out.println("Thread ID: " + id + " ---> EXCEPTION: " + e.getClass().getSimpleName());
                ex = e;
                break;
            }
        }
    }

    public Optional<Integer> getSum() {
        return sumTotal;
    }

    public Exception getException() {
        return ex;
    }
}
