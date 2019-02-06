package com.vlkan.fibertest.ring;

import org.junit.Test;

import static com.vlkan.fibertest.DurationHelper.formatDurationSinceNanos;
import static com.vlkan.fibertest.StdoutLogger.log;

public class JavaFiberRingBenchmarkTest {

    @Test
    public void testRingBenchmark() {
        try (JavaFiberRingBenchmark benchmark = new JavaFiberRingBenchmark()) {
            long startTimeNanos = System.nanoTime();
            int[] sequences = benchmark.ringBenchmark();
            RingBenchmarkTestUtil.verifyResult(sequences);
            log("duration: %s", formatDurationSinceNanos(startTimeNanos));
        }
    }

}
