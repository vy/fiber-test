package com.github.vy.fibertest;

import org.junit.Test;

public class JavaThreadRingBenchmarkTest extends JavaThreadRingBenchmark {

    @Test
    public void testRingBenchmark() throws Exception {
        Util.testRingBenchmark(workerCount, ringSize, ringBenchmark());
    }

}
