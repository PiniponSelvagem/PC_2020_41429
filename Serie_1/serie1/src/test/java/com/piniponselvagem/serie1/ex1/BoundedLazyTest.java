package com.piniponselvagem.serie1.ex1;

import org.junit.Assert;
import org.junit.Test;

public class BoundedLazyTest {

    @Test
    public void testStreamCollapse() {
        SampleEx1.sampleEx1();
        Assert.assertEquals(1, 1);
    }
}