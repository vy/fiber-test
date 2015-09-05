package com.github.vy.fibertest;

import org.junit.Test;

public class QuasarChannelRingBenchmarkTest extends QuasarChannelRingBenchmark {

    @Test
    public void testRingBenchmark() throws Exception {
        Util.testRingBenchmark(workerCount, ringSize, ringBenchmark());
    }

}
