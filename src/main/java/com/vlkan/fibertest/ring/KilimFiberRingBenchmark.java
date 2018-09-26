package com.vlkan.fibertest.ring;

import kilim.Pausable;
import kilim.Task;
import kilim.tools.Kilim;
import org.openjdk.jmh.annotations.Benchmark;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Kilim actors.
 */
public class KilimFiberRingBenchmark implements RingBenchmark {

    public static class InternalFiber extends Task<Integer> {

        private final int id;

        private final int[] sequences;

        private InternalFiber next;

        private volatile boolean waiting = true;

        private int sequence;

        private InternalFiber(int id, int[] sequences) {
            this.id = id;
            this.sequences = sequences;
        }

        @Override
        public void execute() throws Pausable {
            do {
                while (waiting) {
                    pause(getPauseReason());
                }
                waiting = true;
                next.sequence = sequence - 1;
                next.waiting = false;
                next.resume();
            } while (sequence > 0);
            sequences[id] = sequence;
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {

        // Create fibers.
        int[] sequences = new int[WORKER_COUNT];
        InternalFiber[] fibers = new InternalFiber[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            fibers[workerIndex] = new InternalFiber(workerIndex, sequences);
        }

        // Set next fiber pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            fibers[workerIndex].next = fibers[(workerIndex + 1) % WORKER_COUNT];
        }

        // Start fibers.
        for (InternalFiber fiber : fibers) {
            fiber.start();
        }

        // Initiate the ring.
        InternalFiber firstFiber = fibers[0];
        firstFiber.sequence = MESSAGE_PASSING_COUNT;
        firstFiber.waiting = false;
        firstFiber.resume();

        // Wait for workers to complete.
        Task.idledown();
        return sequences;

    }

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrace(String[] ignored) {
        new KilimFiberRingBenchmark().ringBenchmark();
    }

    public static void main(String[] args) throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimFiberRingBenchmark", "kilimEntrace", args);
    }

}
