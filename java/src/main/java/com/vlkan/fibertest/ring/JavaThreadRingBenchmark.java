package com.vlkan.fibertest.ring;

import org.openjdk.jmh.annotations.Benchmark;

import java.util.concurrent.locks.LockSupport;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Java {@link Thread}s.
 */
public class JavaThreadRingBenchmark implements RingBenchmark {

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
                while (next.getState() == State.RUNNABLE);
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
        int[] sequences = new int[WORKER_COUNT];
        Worker[] workers = new Worker[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex, sequences);
        }

        // Set next worker thread pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        // Start workers.
        for (Worker worker : workers) {
            worker.start();
        }

        // Initiate the ring.
        Worker first = workers[0];
        first.sequence = MESSAGE_PASSING_COUNT;
        first.waiting = false;
        LockSupport.unpark(first);

        // Wait for workers to complete.
        for (Worker worker : workers) {
            worker.join();
        }

        // Return populated sequences.
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new JavaThreadRingBenchmark().ringBenchmark();
    }

}
