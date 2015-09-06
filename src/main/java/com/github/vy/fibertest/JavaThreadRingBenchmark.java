package com.github.vy.fibertest;

import org.openjdk.jmh.annotations.Benchmark;

import java.util.concurrent.locks.LockSupport;

/**
 * Ring benchmark using Java threads.
 */
public class JavaThreadRingBenchmark extends AbstractRingBenchmark {

    protected static class Worker extends Thread {

        protected final int id;
        protected final Integer[] sequences;
        protected Worker next = null;
        protected volatile boolean waiting = true;
        protected int sequence;

        public Worker(final int id, final Integer[] sequences) {
            super(String.format("%s-%s-%d",
                    JavaThreadRingBenchmark.class.getSimpleName(),
                    Worker.class.getSimpleName(), id));
            this.id = id;
            this.sequences = sequences;
        }

        @Override
        public void run() {
            do {
                while (waiting) { LockSupport.park(); }
                waiting = true;
                next.sequence = sequence - 1;
                next.waiting = false;
                LockSupport.unpark(next);
            } while (sequence > 0);
            sequences[id] = sequence;
        }

    }

    @Override
    @Benchmark
    public Integer[] ringBenchmark() throws Exception {
        // Create worker threads.
        final Integer[] sequences = new Integer[workerCount];
        final Worker[] workers = new Worker[workerCount];
        for (int i = 0; i < workerCount; i++)
            workers[i] = new Worker(i, sequences);

        // Set next worker thread pointers.
        for (int i = 0; i < workerCount; i++)
            workers[i].next = workers[(i+1) % workerCount];

        // Start workers.
        for (final Worker worker : workers) worker.start();

        // Initiate the ring.
        final Worker first = workers[0];
        first.sequence = ringSize;
        first.waiting = false;
        LockSupport.unpark(first);

        // Wait for workers to complete.
        for (final Worker worker : workers) worker.join();
        return sequences;
    }

    public static void main(String[] args) throws Exception {
        new JavaThreadRingBenchmark().ringBenchmark();
    }

}
