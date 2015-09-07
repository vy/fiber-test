package com.github.vy.fibertest;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Abstract ring benchmark class to load {@code workerCount} and {@code ringSize} properties.
 */
@State(Scope.Benchmark)
abstract class AbstractRingBenchmark {

    protected final int workerCount;
    protected final int ringSize;

    public AbstractRingBenchmark() {
        this.workerCount = Integer.parseInt(System.getProperty("workerCount"));
        this.ringSize = Integer.parseInt(System.getProperty("ringSize"));
    }

    abstract public int[] ringBenchmark() throws Exception;

}
