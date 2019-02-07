package com.vlkan.fibertest.ring;

import org.junit.Test;

public class QuasarDataflowRingBenchmarkTest {

    @Test
    public void testRingBenchmark() {
        RingBenchmarkTestUtil.test(QuasarDataflowRingBenchmark::new);
    }

}
