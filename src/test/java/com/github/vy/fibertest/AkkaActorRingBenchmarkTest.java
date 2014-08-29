package com.github.vy.fibertest;

import org.junit.Test;

public class AkkaActorRingBenchmarkTest extends AkkaActorRingBenchmark {

    @Test
    public void testRingBenchmark() throws Exception {
        Util.testRingBenchmark(workerCount, ringSize, ringBenchmark());
    }

}
