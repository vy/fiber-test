package com.vlkan.fibertest.ring;

import kilim.tools.Kilim;
import org.junit.Test;

import static com.vlkan.fibertest.DurationHelper.formatDurationSinceNanos;
import static com.vlkan.fibertest.StdoutLogger.log;

public class KilimActorRingBenchmarkTest {

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrance(String[] ignored) {
        long startTimeNanos = System.nanoTime();
        KilimActorRingBenchmark benchmark = new KilimActorRingBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
        log("duration: %s", formatDurationSinceNanos(startTimeNanos));
    }

    @Test
    public void testRingBenchmark() throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimActorRingBenchmarkTest", "kilimEntrance");
    }

}
