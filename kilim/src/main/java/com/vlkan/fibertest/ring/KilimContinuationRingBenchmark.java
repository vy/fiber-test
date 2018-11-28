package com.vlkan.fibertest.ring;

import kilim.Continuation;
import kilim.Fiber;
import kilim.Pausable;
import kilim.tools.Kilim;
import org.openjdk.jmh.annotations.Benchmark;

import java.util.LinkedList;
import java.util.Queue;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Kilim {@link Continuation}s.
 */
public class KilimContinuationRingBenchmark implements RingBenchmark {

    private static class Worker extends Continuation {

        private final int _id;

        private final int[] sequences;

        private final Queue<Continuation> continuations;

        private Worker next;

        private int sequence;

        private Worker(int id, int[] sequences, Queue<Continuation> continuations) {
            this._id = id;
            this.sequences = sequences;
            this.continuations = continuations;
        }

        @Override
        public void execute() throws Pausable {
            do {
                Fiber.yield();
                next.sequence = sequence - 1;
                continuations.add(next);
            } while (sequence > 0);
            sequences[_id] = sequence;
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {

        // Create workers.
        int[] sequences = new int[WORKER_COUNT];
        Worker[] workers = new Worker[WORKER_COUNT];
        LinkedList<Continuation> continuations = new LinkedList<>();
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex, sequences, continuations);
        }

        // Set next worker pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        // Start workers.
        for (Worker worker : workers) {
            worker.run();
        }

        // Initiate the ring.
        Worker firstWorker = workers[0];
        firstWorker.sequence = MESSAGE_PASSING_COUNT;
        continuations.add(firstWorker);

        // Execute scheduled continuations.
        for (Continuation continuation; (continuation = continuations.pollFirst()) != null;) {
            continuation.run();
        }

        // Return populated sequences.
        return sequences;

    }

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrace(String[] ignored) {
        new KilimContinuationRingBenchmark().ringBenchmark();
    }

    public static void main(String[] args) throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimContinuationRingBenchmark", "kilimEntrace", args);
    }

}
