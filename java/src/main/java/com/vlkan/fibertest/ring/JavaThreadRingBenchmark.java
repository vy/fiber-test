package com.vlkan.fibertest.ring;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Java {@link Thread}s.
 */
@State(Scope.Benchmark)
public class JavaThreadRingBenchmark implements RingBenchmark {

    static class Worker implements Runnable {

        final Lock lock = new ReentrantLock();

        final Condition waitingCondition = lock.newCondition();

        final Condition completedCondition = lock.newCondition();

        final int id;

        final CountDownLatch startLatch;

        Worker next = null;

        boolean waiting = true;

        boolean completed = false;

        int sequence;

        Worker(int id, CountDownLatch startLatch) {
            this.id = id;
            this.startLatch = startLatch;
        }

        @Override
        public void run() {
            startLatch.countDown();
            log("[%2d] locking", id);
            lock.lock();
            try {
                // noinspection InfiniteLoopStatement
                for (;;) {
                    if (!waiting && !completed) {
                        log("[%2d] locking next", id);
                        next.lock.lock();
                        try {
                            if (!next.completed) {
                                log("[%2d] signaling next", id);
                                if (!next.waiting) {
                                    String message = String.format("%s was expecting %s to be waiting", id, next.id);
                                    throw new IllegalStateException(message);
                                }
                                next.sequence = sequence - 1;
                                next.waiting = false;
                                waiting = true;
                                next.waitingCondition.signal();
                            }
                        } finally {
                            log("[%2d] unlocking next", id);
                            next.lock.unlock();
                        }
                        if (sequence <= 0) {
                            log("[%2d] signaling completion (sequence=%d)", id, sequence);
                            waiting = true;
                            completed = true;
                            completedCondition.signal();
                        }
                    }
                    await();
                }
            } catch (InterruptedException ignored) {
                log("[%2d] interrupted", id);
                Thread.currentThread().interrupt();
            } finally {
                log("[%2d] unlocking", id);
                lock.unlock();
            }
        }

        private void await() throws InterruptedException {
            while (waiting) {
                log("[%2d] awaiting", id);
                waitingCondition.await();
                log("[%2d] woke up (sequence=%d)", id, sequence);
            }
        }

    }

    private static final class Context implements AutoCloseable, Callable<int[]> {

        private final int[] sequences;

        private final Worker[] workers;

        private final Thread[] threads;

        private Context() {

            log("creating worker threads (WORKER_COUNT=%d)", WORKER_COUNT);
            this.sequences = new int[WORKER_COUNT];
            this.workers = new Worker[WORKER_COUNT];
            this.threads = new Thread[WORKER_COUNT];
            CountDownLatch startLatch = new CountDownLatch(WORKER_COUNT);
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                Worker worker = new Worker(workerIndex, startLatch);
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

            log("waiting for threads to start");
            try {
                startLatch.await();
            } catch (InterruptedException ignored) {
                log("start latch wait interrupted");
                Thread.currentThread().interrupt();
            }

        }

        @Override
        public void close() throws Exception {

            log("interrupting threads");
            for (Thread thread : threads) {
                thread.interrupt();
            }

            log("waiting for threads to complete");
            for (Thread thread : threads) {
                thread.join();
            }

        }

        @Override
        public int[] call() {

            log("initiating the ring (MESSAGE_PASSING_COUNT=%d)", MESSAGE_PASSING_COUNT);
            Worker firstWorker = workers[0];
            firstWorker.lock.lock();
            try {
                firstWorker.sequence = MESSAGE_PASSING_COUNT;
                firstWorker.waiting = false;
                firstWorker.waitingCondition.signal();
            } finally {
                firstWorker.lock.unlock();
            }

            for (Worker worker : workers) {
                log("waiting for completion (id=%d)", worker.id);
                worker.lock.lock();
                try {
                    while (!worker.completed) {
                        worker.completedCondition.await();
                    }
                    sequences[worker.id] = worker.sequence;
                } catch (InterruptedException ignored) {
                    log("interrupted (id=%d)", worker.id);
                    Thread.currentThread().interrupt();
                } finally {
                    worker.lock.unlock();
                }
            }

            log("resetting workers");
            for (Worker worker : workers) {
                worker.completed = false;
            }

            log("returning populated sequences");
            return sequences;

        }

    }

    private final Context context = new Context();

    @Override
    @TearDown
    public void close() throws Exception {
        context.close();
    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {
        return context.call();
    }

    public static void main(String[] args) throws Exception {
        try (JavaThreadRingBenchmark benchmark = new JavaThreadRingBenchmark()) {
            benchmark.ringBenchmark();
        }
    }

}
