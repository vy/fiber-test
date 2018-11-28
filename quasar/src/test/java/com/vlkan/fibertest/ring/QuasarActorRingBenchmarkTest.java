package com.vlkan.fibertest.ring;

import org.junit.Test;

public class QuasarActorRingBenchmarkTest extends QuasarActorRingBenchmark {

    @Test
    public void testRingBenchmark() throws Exception {
        RingBenchmark benchmark = new QuasarActorRingBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
    }

}
