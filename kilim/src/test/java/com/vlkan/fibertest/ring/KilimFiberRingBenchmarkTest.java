package com.vlkan.fibertest.ring;

import kilim.tools.Kilim;
import org.junit.Test;

public class KilimFiberRingBenchmarkTest {

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrance(String[] ignored) {
        KilimFiberRingBenchmark benchmark = new KilimFiberRingBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
    }

    @Test
    public void testRingBenchmark() throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimFiberRingBenchmarkTest", "kilimEntrance");
    }

}
