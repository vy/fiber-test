package com.vlkan.fibertest.ring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.*;
import static org.junit.Assert.assertEquals;

enum RingBenchmarkTestUtil {;

    private static final Logger LOGGER = LoggerFactory.getLogger(RingBenchmarkTestUtil.class);

    static void verifyResult(int[] sequences) {
        int offset = WORKER_COUNT - MESSAGE_PASSING_COUNT % WORKER_COUNT;
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            try {
                int expectedSequence = -((offset + workerIndex) % WORKER_COUNT);
                int actualSequence = sequences[workerIndex];
                assertEquals(
                        "sequence returned by Worker#" + workerIndex,
                        expectedSequence, actualSequence);
            } catch (AssertionError ae) {
                LOGGER.trace("sequences[] = {}", Arrays.toString(sequences));
                throw ae;
            }
        }
    }

}
