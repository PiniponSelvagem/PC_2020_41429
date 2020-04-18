package com.piniponselvagem.serie1.utils;

import org.junit.Assert;
import org.junit.Test;

public class TimeoutHolderTest {

    @Test
    public void timeout_isDone() throws InterruptedException {
        TimeoutHolder th = new TimeoutHolder(1000);
        Thread.sleep(1500);
        Assert.assertTrue(th.getTimeoutLeft() <= 0);
    }

    @Test
    public void timeout_isNotDone() throws InterruptedException {
        TimeoutHolder th = new TimeoutHolder(1000);
        Thread.sleep(100);
        Assert.assertFalse(th.getTimeoutLeft() <= 0);
    }
}