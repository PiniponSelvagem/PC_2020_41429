package pc.serie1.ex5;

import org.junit.Test;

import java.util.concurrent.Callable;

import static org.junit.Assert.*;

public class ThreadPoolExecutorTest {

    @Test
    public void test() {

        Callable<Integer> c = () -> {
            Thread.sleep(5000);
            return 100;
        };

        System.out.println("BEFORE");
        try {
            System.out.println(c.call());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("AFTER");
    }
}