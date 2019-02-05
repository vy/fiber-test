package com.vlkan.fibertest.ring;

import kilim.Pausable;
import kilim.tools.Kilim;
import org.junit.Test;

import static com.vlkan.fibertest.DurationHelper.formatDurationSinceNanos;
import static com.vlkan.fibertest.StdoutLogger.log;

public class KilimFiberRingBenchmarkTest {

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrance(String[] ignored) throws Pausable {
        long startTimeNanos = System.nanoTime();
        KilimFiberRingBenchmark benchmark = new KilimFiberRingBenchmark();
        int[] sequences = benchmark.kilimRingBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
        log("duration: %s", formatDurationSinceNanos(startTimeNanos));
    }

    @Test
    public void testRingBenchmark() throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimFiberRingBenchmarkTest", "kilimEntrance");
    }

}
