package com.vlkan.fibertest.ring;

import org.junit.Test;

public class QuasarFiberRingBenchmarkTest {

    @Test
    public void testRingBenchmark() {
        RingBenchmarkTestUtil.test(QuasarFiberRingBenchmark::new);
    }

}
