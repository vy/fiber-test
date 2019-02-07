package com.vlkan.fibertest.ring;

import org.junit.Assert;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

enum RingBenchmarkTestUtil {;

    static void verifyResult(int[] sequences) {
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
