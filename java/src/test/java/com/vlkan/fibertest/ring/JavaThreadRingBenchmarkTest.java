package com.vlkan.fibertest.ring;

import org.junit.Test;

public class JavaThreadRingBenchmarkTest {

    @Test
    public void testRingBenchmark() {
        RingBenchmarkTestUtil.test(JavaThreadRingBenchmark::new);
    }

}
