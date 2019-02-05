package com.vlkan.fibertest.ring;

import kilim.*;
import kilim.tools.Kilim;
import org.openjdk.jmh.annotations.Benchmark;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.*;

/**
 * Ring benchmark using Kilim {@link Task}s.
 */
public class KilimFiberRingBenchmark implements RingBenchmark {

    static {
        Scheduler.defaultNumberThreads = THREAD_COUNT;
    }

    private static class Worker extends Task<Integer> {

        private final Lock lock = new ReentrantLock();

        private final Cell<Object> notWaiting = new Cell<>();

        private final int _id;

        private final int[] sequences;

        private final CountDownLatch startLatch;

        private Worker next;

        private int sequence;

        volatile boolean waiting = true;

        private Worker(int id, int[] sequences, CountDownLatch startLatch) {
            this._id = id;
            this.sequences = sequences;
            this.startLatch = startLatch;
        }

        @Override
        public void execute() throws Pausable {
            startLatch.countDown();
            for (;;) {
                log("[%2d] locking", id);
                lock.lock();
                try {
                    if (!waiting) {
                        log("[%2d] locking next", _id);
                        next.lock.lock();
                        try {
                            log("[%2d] signaling next (sequence=%d)", _id, sequence);
                            if (!next.waiting) {
                                String message = String.format("%s was expecting %s to be waiting", _id, next.id);
                                throw new IllegalStateException(message);
                            }
                            next.sequence = sequence - 1;
                            next.waiting = false;
                            waiting = true;
                            next.notWaiting.put(null);
                        } finally {
                            log("[%2d] unlocking next", _id);
                            next.lock.unlock();
                        }
                        if (sequence <= 0) {
                            sequences[id] = sequence;
                            break;
                        }
                    }
                    await();
                } finally {
                    log("[%2d] unlocking", _id);
                    lock.unlock();
                }
            }
        }

        private void await() throws Pausable {
            while (waiting) {
                log("[%2d] awaiting", _id);
                notWaiting.get();
                log("[%2d] woke up", _id);
            }
        }

    }

    @Override
    public int[] ringBenchmark() {
        throw new UnsupportedOperationException("use kilimRingBenchmark() instead");
    }

    /**
     * Fork of {@link KilimFiberRingBenchmark#ringBenchmark()} to overcome Kilim's
     * complaint about incompatible footprint due to thrown {@link kilim.Pausable}.
     */
    @Benchmark
    public int[] kilimRingBenchmark() throws Pausable {

        log("creating workers");
        int[] sequences = new int[WORKER_COUNT];
        Worker[] workers = new Worker[WORKER_COUNT];
        CountDownLatch startLatch = new CountDownLatch(WORKER_COUNT);
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex, sequences, startLatch);
        }

        log("setting next worker pointers");
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        log("starting workers");
        for (Worker worker : workers) {
            worker.start();
        }

        log("waiting for workers to start");
        try {
            startLatch.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        log("initiating the ring (MESSAGE_PASSING_COUNT=%d)", MESSAGE_PASSING_COUNT);
        Worker firstWorker = workers[0];
        firstWorker.lock.lock();
        try {
            firstWorker.sequence = MESSAGE_PASSING_COUNT;
            firstWorker.waiting = false;
            firstWorker.notWaiting.put(null);
        } finally {
            firstWorker.lock.unlock();
        }

        log("shutting down the scheduler");
        Scheduler.getDefaultScheduler().shutdown();

        log("returning populated sequences");
        return sequences;

    }

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrace(String[] ignored) throws Pausable {
        new KilimFiberRingBenchmark().kilimRingBenchmark();
    }

    public static void main(String[] args) throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimFiberRingBenchmark", "kilimEntrace", args);
    }

}
