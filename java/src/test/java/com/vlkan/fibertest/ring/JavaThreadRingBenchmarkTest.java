package com.vlkan.fibertest.ring;

import org.junit.Test;

import static com.vlkan.fibertest.DurationHelper.formatDurationSinceNanos;
import static com.vlkan.fibertest.StdoutLogger.log;

public class JavaThreadRingBenchmarkTest {

    @Test
    public void testRingBenchmark() throws Exception {
        try (RingBenchmark benchmark = new JavaThreadRingBenchmark()) {
            long startTimeNanos = System.nanoTime();
            int[] sequences = benchmark.ringBenchmark();
            RingBenchmarkTestUtil.verifyResult(sequences);
            log("duration: %s", formatDurationSinceNanos(startTimeNanos));
        }
    }

}
