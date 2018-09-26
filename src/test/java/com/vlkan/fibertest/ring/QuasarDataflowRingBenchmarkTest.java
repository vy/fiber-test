package com.vlkan.fibertest.ring;

import org.junit.Test;

public class QuasarDataflowRingBenchmarkTest extends QuasarDataflowRingBenchmark {

    @Test
    public void testRingBenchmark() throws Exception {
        int[] sequences = ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(workerCount, ringSize, sequences);
    }

}
