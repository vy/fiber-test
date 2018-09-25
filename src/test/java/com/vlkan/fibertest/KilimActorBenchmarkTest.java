package com.vlkan.fibertest;

import kilim.tools.Kilim;
import org.junit.Test;

public class KilimActorBenchmarkTest {

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrance(String[] ignored) {
        KilimActorBenchmark benchmark = new KilimActorBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        Util.testRingBenchmark(benchmark.workerCount, benchmark.ringSize, sequences);
    }

    @Test
    public void testRingBenchmark() throws Exception {
        Kilim.run("com.vlkan.fibertest.KilimActorBenchmarkTest", "kilimEntrance");
    }

}
