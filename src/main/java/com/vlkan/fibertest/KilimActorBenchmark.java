package com.vlkan.fibertest;

import kilim.Pausable;
import kilim.Task;
import kilim.tools.Kilim;
import org.openjdk.jmh.annotations.Benchmark;

/**
 * Ring benchmark using Kilim actors.
 */
public class KilimActorBenchmark extends AbstractRingBenchmark {

    public static class InternalActor extends Task<Integer> {

        private final int id;

        private final int[] sequences;

        private InternalActor next;

        private volatile boolean waiting = true;

        private int sequence;

        private InternalActor(int id, int[] sequences) {
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

        // Create actors.
        int[] sequences = new int[workerCount];
        InternalActor[] actors = new InternalActor[workerCount];
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            actors[workerIndex] = new InternalActor(workerIndex, sequences);
        }

        // Set next actor pointers.
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            actors[workerIndex].next = actors[(workerIndex + 1) % workerCount];
        }

        // Start actors.
        for (InternalActor fiber : actors) {
            fiber.start();
        }

        // Initiate the ring.
        InternalActor firstActor = actors[0];
        firstActor.sequence = ringSize;
        firstActor.waiting = false;
        firstActor.resume();

        // Wait for workers to complete.
        Task.idledown();
        return sequences;

    }

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrace(String[] ignored) {
        new KilimActorBenchmark().ringBenchmark();
    }

    public static void main(String[] args) throws Exception {
        Kilim.run("com.vlkan.fibertest.KilimActorBenchmark", "kilimEntrace", args);
    }

}
