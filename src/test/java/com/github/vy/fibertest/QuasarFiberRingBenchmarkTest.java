package com.github.vy.fibertest;

import org.junit.Test;

public class QuasarFiberRingBenchmarkTest extends QuasarFiberRingBenchmark {

    @Test
    public void testRingBenchmark() throws Exception {
        Util.testRingBenchmark(workerCount, ringSize, ringBenchmark());
    }

}
