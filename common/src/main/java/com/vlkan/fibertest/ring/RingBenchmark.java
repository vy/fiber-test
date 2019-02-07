package com.vlkan.fibertest.ring;

public interface RingBenchmark extends AutoCloseable {

    int[] ringBenchmark() throws Exception;

}
