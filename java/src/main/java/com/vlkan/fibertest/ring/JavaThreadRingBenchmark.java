package com.vlkan.fibertest.ring;

import org.openjdk.jmh.annotations.Benchmark;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Java {@link Thread}s.
 */
public class JavaThreadRingBenchmark implements RingBenchmark {

    private static class Worker extends Thread {

        private final Lock lock = new ReentrantLock();

        private final Condition notWaiting = lock.newCondition();

        private final int id;

        private final int[] sequences;

        private Worker next = null;

        private volatile boolean waiting = true;

        private int sequence;

        private Worker(int id, int[] sequences) {
            super("Worker-"  + id);
            this.id = id;
            this.sequences = sequences;
        }

        @Override
        public void run() {
            for (;;) {
                log("[%2d] locking", id);
                lock.lock();
                try {
                    if (!waiting) {
                        log("[%2d] locking next", id);
                        next.lock.lock();
                        try {
                            log("[%2d] signaling next (sequence=%d)", id, sequence);
                            if (!next.waiting) {
                                String message = String.format("%s was expecting %s to be waiting", id, next.id);
                                throw new IllegalStateException(message);
                            }
                            next.sequence = sequence - 1;
                            next.waiting = false;
                            waiting = true;
                            next.notWaiting.signal();
                        } finally {
                            log("[%2d] unlocking next", id);
                            next.lock.unlock();
                        }
                        if (sequence <= 0) {
                            sequences[id] = sequence;
                            break;
                        }
                    }
                    await();
                } finally {
                    log("[%2d] unlocking", id);
                    lock.unlock();
                }
            }
        }

        private void await() {
            while (waiting) {
                try {
                    log("[%2d] awaiting", id);
                    notWaiting.await();
                    log("[%2d] woke up", id);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() throws Exception {

        log("creating worker threads (WORKER_COUNT=%d)", WORKER_COUNT);
        int[] sequences = new int[WORKER_COUNT];
        Worker[] workers = new Worker[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex, sequences);
        }

        log("setting next worker thread pointers");
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        log("starting workers");
        for (Worker worker : workers) {
            worker.start();
        }

        log("ensuring workers are started and waiting");
        for (Worker worker : workers) {
            // noinspection LoopConditionNotUpdatedInsideLoop, StatementWithEmptyBody
            while (worker.getState() != Thread.State.WAITING);
        }

        log("initiating the ring (MESSAGE_PASSING_COUNT=%d)", MESSAGE_PASSING_COUNT);
        Worker firstWorker = workers[0];
        firstWorker.lock.lock();
        try {
            firstWorker.sequence = MESSAGE_PASSING_COUNT;
            firstWorker.waiting = false;
            firstWorker.notWaiting.signal();
        } finally {
            firstWorker.lock.unlock();
        }

        log("waiting for workers to complete");
        for (Worker worker : workers) {
            worker.join();
        }

        log("returning populated sequences");
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new JavaThreadRingBenchmark().ringBenchmark();
    }

}
