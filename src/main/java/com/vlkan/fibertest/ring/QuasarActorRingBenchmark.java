package com.vlkan.fibertest.ring;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.SuspendExecution;
import org.openjdk.jmh.annotations.Benchmark;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Quasar {@link Actor}s.
 */
public class QuasarActorRingBenchmark implements RingBenchmark {

    private static class InternalActor extends Actor<Integer, Integer> {

        private ActorRef<Integer> next = null;

        private InternalActor(int id) {
            super(String.format("%s-%s-%d",
                    QuasarActorRingBenchmark.class.getSimpleName(),
                    InternalActor.class.getSimpleName(), id), null);
        }

        @Override
        protected Integer doRun() throws InterruptedException, SuspendExecution {
            Integer sequence;
            do {
                sequence = receive();
                next.send(sequence - 1);
            } while (sequence > 0);
            return sequence;
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() throws Exception {

        // Create and start actors.
        InternalActor[] actors = new InternalActor[WORKER_COUNT];
        @SuppressWarnings("unchecked") ActorRef<Integer>[] actorRefs = new ActorRef[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            InternalActor actor = new InternalActor(workerIndex);
            actors[workerIndex] = actor;
            actorRefs[workerIndex] = actor.spawn();
        }

        // Set next actor pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            actors[workerIndex].next = actorRefs[(workerIndex + 1) % WORKER_COUNT];
        }

        // Initiate the ring.
        actorRefs[0].send(MESSAGE_PASSING_COUNT);

        // Wait for actors to finish and collect the results.
        int[] sequences = new int[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            sequences[workerIndex] = actors[workerIndex].get();
        }

        // Return populated sequences.
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new QuasarActorRingBenchmark().ringBenchmark();
    }

}
