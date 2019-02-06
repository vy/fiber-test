package com.vlkan.fibertest.ring;

public interface RingBenchmark extends AutoCloseable {

    int[] ringBenchmark() throws Exception;

    // TODO Remove once every implementation overrides.
    @Override
    default void close() throws Exception {}

}
