package com.vlkan.fibertest.ring;

import org.openjdk.jmh.annotations.Benchmark;

import java.util.LinkedList;
import java.util.Queue;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

public class JavaContinuationRingBenchmark implements RingBenchmark {

    private static class Worker implements Runnable {

        private final ContinuationScope scope;

        private final int id;

        private final int[] sequences;

        private final Queue<Continuation> executionQueue;

        private Continuation continuation;

        private Worker next;

        private int sequence;

        private Worker(ContinuationScope scope, int id, int[] sequences, Queue<Continuation> executionQueue) {
            this.scope = scope;
            this.id = id;
            this.sequences = sequences;
            this.executionQueue = executionQueue;
        }

        @Override
        public void run() {
            do {
                Continuation.yield(scope);
                next.sequence = sequence - 1;
                executionQueue.add(next.continuation);
            } while (sequence > 0);
            sequences[id] = sequence;
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {

        // Create workers.
        int[] sequences = new int[WORKER_COUNT];
        Worker[] workers = new Worker[WORKER_COUNT];
        Queue<Continuation> executionQueue = new LinkedList<>();
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            ContinuationScope scope = new ContinuationScope(String.format("W-%d", workerIndex));
            Worker worker = new Worker(scope, workerIndex, sequences, executionQueue);
            worker.continuation = new Continuation(scope, worker);
            workers[workerIndex] = worker;
        }

        // Set next worker pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        // Start workers.
        for (Worker worker : workers) {
            worker.continuation.run();
        }

        // Initiate the ring.
        Worker firstWorker = workers[0];
        firstWorker.sequence = MESSAGE_PASSING_COUNT;
        executionQueue.add(firstWorker.continuation);

        // Execute scheduled continuations.
        for (Continuation continuation; (continuation = executionQueue.poll()) != null;) {
            if (!continuation.isDone()) {
                continuation.run();
            }
        }

        // Return populated sequences.
        return sequences;

    }

    public static void main(String[] args) {
        new JavaContinuationRingBenchmark().ringBenchmark();
    }

}
