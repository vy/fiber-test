package com.vlkan.fibertest.ring;

import kilim.Cell;
import kilim.Pausable;
import kilim.Scheduler;
import kilim.Task;
import kilim.tools.Kilim;
import org.openjdk.jmh.annotations.Benchmark;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.THREAD_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Kilim {@link Task}s with message passing.
 */
public class KilimActorRingBenchmark implements RingBenchmark {

    static {
        Scheduler.defaultNumberThreads = THREAD_COUNT;
    }

    private static class Worker extends Task<Integer> {

        private final int _id;

        private final int[] sequences;

        private Worker next;

        private int sequence;

        private Cell<Integer> box = new Cell<>();

        private Worker(int id, int[] sequences) {
            this._id = id;
            this.sequences = sequences;
        }

        @Override
        public void execute() throws Pausable {
            do {
                sequence = box.get();
                next.box.putnb(sequence - 1);
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
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex, sequences);
        }

        // Set next worker pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        // Start workers.
        for (Worker worker : workers) {
            worker.start();
        }

        // Initiate the ring.
        Worker firstWorker = workers[0];
        firstWorker.box.putnb(MESSAGE_PASSING_COUNT);

        // Wait for scheduler to finish and shut it down.
        Task.idledown();

        // Return populated sequences.
        return sequences;

    }

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrace(String[] ignored) {
        new KilimActorRingBenchmark().ringBenchmark();
    }

    public static void main(String[] args) throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimActorRingBenchmark", "kilimEntrace", args);
    }

}
