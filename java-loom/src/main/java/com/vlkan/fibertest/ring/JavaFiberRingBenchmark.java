package com.vlkan.fibertest.ring;

import java.lang.Fiber;
import java.util.concurrent.locks.LockSupport;

import org.openjdk.jmh.annotations.Benchmark;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

public class JavaFiberRingBenchmark implements RingBenchmark {

    private static class Worker implements Runnable {

        /** @noinspection unused (kept for debugging purposes) */
        private final int id;

        private Worker next;

        private Fiber fiber;

        private volatile boolean waiting = true;

        private int sequence;

        private Worker(int id) {
            this.id = id;
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
                LockSupport.unpark(next.fiber);
            } while (sequence > 0);
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {

        // Create workers.
        Worker[] workers = new Worker[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex);
            workers[workerIndex].fiber = new Fiber(workers[workerIndex]);
        }

        // Set next worker pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        // Start fibers.
        for (Worker worker : workers) {
            worker.fiber.schedule();
        }

        // Initiate the ring.
        Worker firstWorker = workers[0];
        firstWorker.sequence = MESSAGE_PASSING_COUNT;
        firstWorker.waiting = false;
        LockSupport.unpark(firstWorker.fiber);

        // Wait for workers to complete.
        int[] sequences = new int[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            Worker worker = workers[workerIndex];
            worker.fiber.await();
            sequences[workerIndex] = worker.sequence;
        }

        // Return populated sequences.
        return sequences;

    }

    public static void main(String[] args) {
        new JavaFiberRingBenchmark().ringBenchmark();
    }

}
