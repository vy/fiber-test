package com.vlkan.fibertest.ring;

import org.junit.Test;

import static com.vlkan.fibertest.DurationHelper.formatDurationSinceNanos;
import static com.vlkan.fibertest.StdoutLogger.log;

public class QuasarActorRingBenchmarkTest extends QuasarActorRingBenchmark {

    @Test
    public void testRingBenchmark() throws Exception {
        try (QuasarActorRingBenchmark benchmark = new QuasarActorRingBenchmark()) {
            long startTimeNanos = System.nanoTime();
            int[] sequences = benchmark.ringBenchmark();
            RingBenchmarkTestUtil.verifyResult(sequences);
            log("duration: %s", formatDurationSinceNanos(startTimeNanos));
        }
    }

}
