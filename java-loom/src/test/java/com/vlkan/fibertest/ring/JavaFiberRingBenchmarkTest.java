package com.vlkan.fibertest.ring;

import org.junit.Test;

public class JavaFiberRingBenchmarkTest {

    @Test
    public void testRingBenchmark() throws Exception {
        RingBenchmark benchmark = new JavaFiberRingBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
    }

}
