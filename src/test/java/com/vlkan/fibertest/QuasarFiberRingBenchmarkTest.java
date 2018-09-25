package com.vlkan.fibertest;

import org.junit.Test;

public class QuasarFiberRingBenchmarkTest extends QuasarFiberRingBenchmark {

    @Test
    public void testRingBenchmark() throws Exception {
        int[] sequences = ringBenchmark();
        Util.testRingBenchmark(workerCount, ringSize, sequences);
    }

}
