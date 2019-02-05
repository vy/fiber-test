package com.vlkan.fibertest.ring;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.JavaThreadRingBenchmark.Worker;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.*;

@State(Scope.Benchmark)
public class JavaFiberRingBenchmark implements RingBenchmark {

    private static final class Context implements AutoCloseable, Callable<int[]> {

        private final ExecutorService executorService;

        private final Worker[] workers;

        private final int[] sequences;

        private Context() {

            log("creating workers (WORKER_COUNT=%d)", WORKER_COUNT);
            this.workers = new Worker[WORKER_COUNT];
            this.sequences = new int[WORKER_COUNT];
            CountDownLatch startLatch = new CountDownLatch(WORKER_COUNT);
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                Worker worker = new Worker(workerIndex, startLatch);
                workers[workerIndex] = worker;
            }

            log("setting next worker pointers");
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
            }

            log("scheduling fibers (THREAD_COUNT=%d)", THREAD_COUNT);
            this.executorService = Executors.newFixedThreadPool(THREAD_COUNT);
            for (Worker worker : workers) {
                Fiber.schedule(executorService, worker);
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
            log("shutting down the executor service");
            executorService.shutdown();
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
    public void close() {
        context.close();
    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {
        return context.call();
    }

    public static void main(String[] args) {
        try (JavaFiberRingBenchmark benchmark = new JavaFiberRingBenchmark()) {
            benchmark.ringBenchmark();
        }
    }

}
