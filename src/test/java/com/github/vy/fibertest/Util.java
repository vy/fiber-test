package com.github.vy.fibertest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

class Util {

    private static final Logger log = LoggerFactory.getLogger(Util.class);

    static void testRingBenchmark(
            final int workerCount,
            final int ringSize,
            final Integer[] sequences) {
        final int offset = workerCount - ringSize % workerCount;
        for (int i = 0; i < workerCount; i++)
            try {
                assertEquals(
                        "sequence returned by Worker#" + i,
                        -((offset + i) % workerCount), sequences[i].intValue());
            } catch (AssertionError ae) {
                log.trace("sequences[] = {}", Arrays.toString(sequences));
                throw ae;
            }
    }

}
