package com.vlkan.fibertest.ring;

import org.junit.Test;

public class AkkaActorRingBenchmarkTest {

    @Test
    public void testRingBenchmark() {
        RingBenchmarkTestUtil.test(AkkaActorRingBenchmark::new);
    }

}
