package com.vlkan.fibertest;

import kilim.tools.Kilim;
import org.junit.Test;

public class KilimFiberRingBenchmarkTest {

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrance(String[] ignored) {
        doRing(new KilimContinuationRingBenchmark());
        doRing(new KilimFiberRingBenchmark());
        doRing(new KilimFiberRingBenchmark.Fork());
        doRing(new KilimActorRingBenchmark());
        doRing(new KilimActorRingBenchmark.Fork());
    }

    static void doRing(AbstractRingBenchmark benchmark) {
        int[] sequences = null;
        try {
            sequences = benchmark.ringBenchmark();
        }
        catch (Exception ex) {}
        Util.testRingBenchmark(benchmark.workerCount, benchmark.ringSize, sequences);
    }
    
    
    @Test
    public void testRingBenchmark() throws Exception {
        Kilim.run("com.vlkan.fibertest.KilimFiberRingBenchmarkTest", "kilimEntrance");
    }

}
