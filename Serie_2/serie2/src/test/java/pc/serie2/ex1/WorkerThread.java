package pc.serie2.ex1;

import java.util.Optional;

public class WorkerThread extends Thread {

    private SafeBoundedLazy<Integer> boundedLazy;
    private final int id, cycles, timeout;
    private Optional<Integer> sumTotal = Optional.of(0);
    private Throwable e;

    public WorkerThread(int id, SafeBoundedLazy<Integer> boundedLazy, int cycles, int timeout) {
        this.boundedLazy = boundedLazy;
        this.id = id;
        this.cycles = cycles;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        for (int i=0; i<cycles; ) {
            try {
                Optional<Integer> value = boundedLazy.get();
                if (value.isPresent()) ++i;

                System.out.println("Thread ID: " + id + " ---> " + value);

                sumTotal = Optional.of(sumTotal.get() + value.orElse(0));
            } catch (Throwable e) {
                System.out.println("Thread ID: " + id + " ---> EXCEPTION: " + e.getClass().getSimpleName());
                this.e = e;
                break;
            }
        }
    }

    public Optional<Integer> getSum() {
        return sumTotal;
    }

    public Throwable getThrowable() {
        return e;
    }
}
