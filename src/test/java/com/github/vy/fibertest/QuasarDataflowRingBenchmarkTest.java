package com.github.vy.fibertest;

import org.junit.Test;

public class QuasarDataflowRingBenchmarkTest extends QuasarDataflowRingBenchmark {

    @Test
    public void testRingBenchmark() throws Exception {
        int[] sequences = ringBenchmark();
        Util.testRingBenchmark(workerCount, ringSize, sequences);
    }

}
