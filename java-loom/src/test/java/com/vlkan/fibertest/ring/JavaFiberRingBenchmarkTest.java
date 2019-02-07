package com.vlkan.fibertest.ring;

import org.junit.Test;

public class JavaFiberRingBenchmarkTest {

    @Test
    public void testRingBenchmark() {
        RingBenchmarkTestUtil.test(JavaFiberRingBenchmark::new);
    }

}
