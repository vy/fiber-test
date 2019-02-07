package com.vlkan.fibertest.ring;

import org.junit.Test;

public class JavaContinuationRingBenchmarkTest {

    @Test
    public void testRingBenchmark() {
        RingBenchmarkTestUtil.test(JavaContinuationRingBenchmark::new);
    }

}
