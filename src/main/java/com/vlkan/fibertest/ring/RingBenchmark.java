package com.vlkan.fibertest.ring;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public interface RingBenchmark {

    int[] ringBenchmark() throws Exception;

}
