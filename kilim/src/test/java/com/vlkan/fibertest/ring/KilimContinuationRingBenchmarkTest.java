package com.vlkan.fibertest.ring;

import kilim.tools.Kilim;
import org.junit.Test;

public class KilimContinuationRingBenchmarkTest {

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrance(String[] ignored) {
        KilimContinuationRingBenchmark benchmark = new KilimContinuationRingBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
    }

    @Test
    public void testRingBenchmark() throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimContinuationRingBenchmarkTest", "kilimEntrance");
    }

}
