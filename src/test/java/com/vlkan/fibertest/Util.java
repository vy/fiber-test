package com.vlkan.fibertest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

enum Util {;

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    static void testRingBenchmark(
            int workerCount,
            int ringSize,
            int[] sequences) {
        int offset = workerCount - ringSize % workerCount;
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            try {
                int expectedSequence = -((offset + workerIndex) % workerCount);
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
