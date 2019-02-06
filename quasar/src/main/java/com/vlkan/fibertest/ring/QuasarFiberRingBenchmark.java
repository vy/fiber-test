package com.vlkan.fibertest.ring;

import co.paralleluniverse.common.monitoring.MonitorType;
import co.paralleluniverse.fibers.*;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.*;

/**
 * Ring benchmark using Quasar {@link Fiber}s.
 */
@State(Scope.Benchmark)
public class QuasarFiberRingBenchmark implements RingBenchmark {

    private static class Worker implements SuspendableCallable<Void> {

        private final Lock lock = new ReentrantLock();

        private final Condition waitingCondition = lock.newCondition();

        private final Condition completedCondition = lock.newCondition();

        private final int id;

        private final CountDownLatch startLatch;

        private Worker next;

        private boolean waiting = true;

        private boolean completed = false;

        private int sequence;

        private Worker(int id, CountDownLatch startLatch) {
            this.id = id;
            this.startLatch = startLatch;
        }

        @Override
        @Suspendable
        public Void run() {
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
                                log("[%2d] signaling next (sequence=%d)", () -> new Object[] { id, sequence });
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
                            log("[%2d] signaling completion (sequence=%d)", () -> new Object[] { id, sequence });
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
            return null;
        }

        @Suspendable
        private void await() throws InterruptedException {
            while (waiting) {
                log("[%2d] awaiting", id);
                waitingCondition.await();
                log("[%2d] woke up (sequence=%d)", () -> new Object[] { id, sequence });
            }
        }

    }

    private static final class Context implements AutoCloseable, Callable<int[]> {

        private final FiberScheduler scheduler;

        private final int[] sequences;

        private final Worker[] workers;

        private Context() {

            log("creating executor service (THREAD_COUNT=%d)", THREAD_COUNT);

            log("creating workers (WORKER_COUNT=%d)", WORKER_COUNT);
            this.workers = new Worker[WORKER_COUNT];
            this.sequences = new int[WORKER_COUNT];
            CountDownLatch startLatch = new CountDownLatch(WORKER_COUNT);
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                workers[workerIndex] = new Worker(workerIndex, startLatch);
            }

            log("setting next worker pointers");
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
            }

            log("starting fibers (THREAD_COUNT=%d)", THREAD_COUNT);
            this.scheduler = new FiberForkJoinScheduler("RingBenchmark", THREAD_COUNT, null, MonitorType.NONE, false);
            for (Worker worker : workers) {
                scheduler.newFiber(worker).start();
            }

            log("waiting for fibers to start");
            try {
                startLatch.await();
            } catch (InterruptedException ignored) {
                log("start latch wait interrupted");
                Thread.currentThread().interrupt();
            }

        }

        @Override
        public void close() {
            log("shutting down the scheduler");
            scheduler.shutdown();
        }

        @Override
        @Suspendable
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
    public void close() {
        context.close();
    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {
        return context.call();
    }

    public static void main(String[] args) {
        try (QuasarFiberRingBenchmark benchmark = new QuasarFiberRingBenchmark()) {
            benchmark.ringBenchmark();
        }
    }

}
