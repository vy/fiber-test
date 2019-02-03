package com.vlkan.fibertest.ring;

import org.junit.Test;

import static com.vlkan.fibertest.DurationHelper.formatDurationSinceNanos;
import static com.vlkan.fibertest.StdoutLogger.log;

public class QuasarActorRingBenchmarkTest extends QuasarActorRingBenchmark {

    @Test
    public void testRingBenchmark() throws Exception {
        long startTimeNanos = System.nanoTime();
        RingBenchmark benchmark = new QuasarActorRingBenchmark();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
        log("duration: %s", formatDurationSinceNanos(startTimeNanos));
    }

}
