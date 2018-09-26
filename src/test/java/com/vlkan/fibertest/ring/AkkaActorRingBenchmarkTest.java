package com.vlkan.fibertest.ring;

import org.junit.Test;

public class AkkaActorRingBenchmarkTest {

    @Test
    public void testRingBenchmark() throws Exception {
        RingBenchmark benchmark = new AkkaActorRingBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
    }

}
