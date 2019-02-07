package com.vlkan.fibertest.ring;

import com.vlkan.fibertest.SingletonSynchronizer;
import org.openjdk.jmh.annotations.*;

import java.util.Arrays;
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

        private final SingletonSynchronizer completionSynchronizer = new SingletonSynchronizer();

        private final int[] sequences = new int[WORKER_COUNT];

        private final ExecutorService executorService;

        private final Worker[] workers;

        private Context() {

            log("creating workers (WORKER_COUNT=%d)", WORKER_COUNT);
            this.workers = new Worker[WORKER_COUNT];
            CountDownLatch startLatch = new CountDownLatch(WORKER_COUNT);
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                Worker worker = new Worker(workerIndex, startLatch, completionSynchronizer);
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

            log("waiting for completion");
            completionSynchronizer.await();

            log("collecting sequences");
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                sequences[workerIndex] = workers[workerIndex].sequence;
            }

            log("returning populated sequences (sequences=%s)", () -> new Object[] {Arrays.toString(sequences) });
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
