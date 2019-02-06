package com.vlkan.fibertest.ring;

import kilim.tools.Kilim;
import org.junit.Test;

import static com.vlkan.fibertest.DurationHelper.formatDurationSinceNanos;
import static com.vlkan.fibertest.StdoutLogger.log;

public class KilimCellRingBenchmarkTest {

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrance(String[] ignored) {
        long startTimeNanos = System.nanoTime();
        KilimCellRingBenchmark benchmark = new KilimCellRingBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
        log("duration: %s", formatDurationSinceNanos(startTimeNanos));
    }

    @Test
    public void testRingBenchmark() throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimCellRingBenchmarkTest", "kilimEntrance");
    }

}
