package pc.serie1.ex5;

import java.util.Optional;
import java.util.concurrent.Callable;

public class ExecutorThread<T> extends Thread {

    private boolean isAlive = true;
    private Callable<T> command;
    private Optional<T> result;

    public ExecutorThread() {
    }

    @Override
    public void run() {
        while(isAlive) {
            if (command != null) {
                try {
                    result = Optional.of(command.call());
                    command = null;
                } catch (Exception e) {
                    // TODO: PROPAGATE EXCEPTION
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean submitCommand(Callable<T> command) {
        if (this.command != null)
            return false;

        this.command = command;
        return true;
    }

    public Optional<T> getResult() {
        return result;
    }
}
