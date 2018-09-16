package com.github.vy.fibertest;

import org.openjdk.jmh.annotations.Benchmark;

import java.util.concurrent.locks.LockSupport;

/**
 * Ring benchmark using Java threads.
 */
public class JavaThreadRingBenchmark extends AbstractRingBenchmark {

    private static class Worker extends Thread {

        private final int id;

        private final int[] sequences;

        private Worker next = null;

        private volatile boolean waiting = true;

        private int sequence;

        private Worker(int id, int[] sequences) {
            super(String.format("%s-%s-%d",
                    JavaThreadRingBenchmark.class.getSimpleName(),
                    Worker.class.getSimpleName(), id));
            this.id = id;
            this.sequences = sequences;
        }

        @Override
        public void run() {
            do {
                while (waiting) {
                    LockSupport.park();
                }
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
    public int[] ringBenchmark() throws Exception {

        // Create worker threads.
        int[] sequences = new int[workerCount];
        Worker[] workers = new Worker[workerCount];
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex, sequences);
        }

        // Set next worker thread pointers.
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % workerCount];
        }

        // Start workers.
        for (Worker worker : workers) {
            worker.start();
        }

        // Initiate the ring.
        Worker first = workers[0];
        first.sequence = ringSize;
        first.waiting = false;
        LockSupport.unpark(first);

        // Wait for workers to complete.
        for (Worker worker : workers) {
            worker.join();
        }
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new JavaThreadRingBenchmark().ringBenchmark();
    }

}
