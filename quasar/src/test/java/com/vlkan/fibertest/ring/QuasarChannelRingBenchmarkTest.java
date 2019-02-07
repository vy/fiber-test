package com.vlkan.fibertest.ring;

import org.junit.Test;

public class QuasarChannelRingBenchmarkTest {

    @Test
    public void testRingBenchmark() {
        RingBenchmarkTestUtil.test(QuasarChannelRingBenchmark::new);
    }

}
