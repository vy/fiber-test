package com.vlkan.fibertest.ring;

import org.junit.Test;

public class QuasarActorRingBenchmarkTest extends QuasarActorRingBenchmark {

    @Test
    public void testRingBenchmark() {
        RingBenchmarkTestUtil.test(QuasarActorRingBenchmark::new);
    }

}
