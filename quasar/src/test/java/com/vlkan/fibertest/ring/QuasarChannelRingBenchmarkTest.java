package com.vlkan.fibertest.ring;

import org.junit.Test;

public class QuasarChannelRingBenchmarkTest {

    @Test
    public void testRingBenchmark() throws Exception {
        RingBenchmark benchmark = new QuasarChannelRingBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
    }

}
