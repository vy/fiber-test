package com.vlkan.fibertest.ring;

import org.junit.Test;

public class QuasarDataflowRingBenchmarkTest {

    @Test
    public void testRingBenchmark() throws Exception {
        RingBenchmark benchmark = new QuasarDataflowRingBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
    }

}
