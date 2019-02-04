package com.vlkan.fibertest.ring;

import org.openjdk.jmh.annotations.Benchmark;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.JavaThreadRingBenchmark.Worker;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.*;

public class JavaFiberRingBenchmark implements RingBenchmark {

    @Override
    @Benchmark
    public int[] ringBenchmark() {

        log("creating executor service (THREAD_COUNT=%d)", THREAD_COUNT);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        log("creating workers (WORKER_COUNT=%d)", WORKER_COUNT);
        Worker[] workers = new Worker[WORKER_COUNT];
        Fiber[] fibers = new Fiber[WORKER_COUNT];
        int[] sequences = new int[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex, sequences);
            fibers[workerIndex] = Fiber.schedule(executorService, workers[workerIndex]);
        }

        log("setting \"next\" worker pointers");
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
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
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            Fiber fiber = fibers[workerIndex];
            fiber.awaitTermination();
        }

        log("shutting down the executor service");
        executorService.shutdown();

        log("returning populated sequences");
        return sequences;

    }

    public static void main(String[] args) {
        new JavaFiberRingBenchmark().ringBenchmark();
    }

}
