package com.vlkan.fibertest.ring;

import org.junit.Assert;

import java.util.Arrays;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

enum RingBenchmarkTestUtil {;

    static void verifyResult(int[] sequences) {
        int offset = WORKER_COUNT - MESSAGE_PASSING_COUNT % WORKER_COUNT;
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            try {
                int expectedSequence = (offset + workerIndex) % WORKER_COUNT;
                int actualSequence = sequences[workerIndex];
                Assert.assertEquals(
                        "sequence returned by Worker#" + workerIndex,
                        expectedSequence, actualSequence);
            } catch (AssertionError error) {
                log("sequences[] = %s", () -> new Object[] { Arrays.toString(sequences) });
                throw error;
            }
        }
    }

}
