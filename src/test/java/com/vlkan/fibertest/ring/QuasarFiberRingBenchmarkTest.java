package com.vlkan.fibertest.ring;

import org.junit.Test;

public class QuasarFiberRingBenchmarkTest {

    @Test
    public void testRingBenchmark() throws Exception {
        RingBenchmark benchmark = new QuasarFiberRingBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
    }

}
