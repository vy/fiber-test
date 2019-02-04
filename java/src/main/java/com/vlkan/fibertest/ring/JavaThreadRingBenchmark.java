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

    static class Worker implements Runnable {

        final Lock lock = new ReentrantLock();

        final Condition notWaiting = lock.newCondition();

        final int id;

        final int[] sequences;

        Worker next = null;

        volatile boolean waiting = true;

        int sequence;

        Worker(int id, int[] sequences) {
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
        Thread[] threads = new Thread[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            Worker worker = new Worker(workerIndex, sequences);
            workers[workerIndex] = worker;
            threads[workerIndex] = new Thread(worker, "Worker-" + workerIndex);
        }

        log("setting next worker pointers");
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        log("starting threads");
        for (Thread thread : threads) {
            thread.start();
        }

        log("ensuring threads are started and waiting");
        for (Thread thread : threads) {
            // noinspection LoopConditionNotUpdatedInsideLoop, StatementWithEmptyBody
            while (thread.getState() != Thread.State.WAITING);
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

        log("waiting for threads to complete");
        for (Thread thread : threads) {
            thread.join();
        }

        log("returning populated sequences");
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new JavaThreadRingBenchmark().ringBenchmark();
    }

}
