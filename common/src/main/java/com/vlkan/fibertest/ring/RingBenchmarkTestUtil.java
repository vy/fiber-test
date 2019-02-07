package com.vlkan.fibertest.ring;

import org.junit.Assert;

import java.util.function.Supplier;

import static com.vlkan.fibertest.DurationHelper.formatDurationSinceNanos;
import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

enum RingBenchmarkTestUtil {;

    static void test(Supplier<RingBenchmark> benchmarkSupplier) {
        try (RingBenchmark benchmark = benchmarkSupplier.get()) {
            // Running benchmark multiple times to check if the worker reuse is causing any discrepancies.
            for (int trialIndex = 0; trialIndex < 5; trialIndex++) {
                test(trialIndex, benchmark);
            }
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    private static void test(int trialIndex, RingBenchmark benchmark) throws Exception {
        long startTimeNanos = System.nanoTime();
        int[] sequences = benchmark.ringBenchmark();
        RingBenchmarkTestUtil.verifyResult(sequences);
        log("[%d] duration: %s", () -> new Object[]{trialIndex, formatDurationSinceNanos(startTimeNanos)});
    }

    private static void verifyResult(int[] sequences) {
        int completedWorkerIndex = MESSAGE_PASSING_COUNT % WORKER_COUNT;
        int workerIndex = completedWorkerIndex;
        int expectedSequence = 0;
        do {
            int actualSequence = sequences[workerIndex];
            Assert.assertEquals(
                    "sequence returned by Worker#" + workerIndex,
                    expectedSequence, actualSequence);
            expectedSequence++;
            workerIndex = (workerIndex - 1 + WORKER_COUNT) % WORKER_COUNT;
        } while (workerIndex != completedWorkerIndex && expectedSequence <= MESSAGE_PASSING_COUNT);
    }

}
