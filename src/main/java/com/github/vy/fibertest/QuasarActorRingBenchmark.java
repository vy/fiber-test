package com.github.vy.fibertest;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.SuspendExecution;
import org.openjdk.jmh.annotations.Benchmark;

public class QuasarActorRingBenchmark extends AbstractRingBenchmark {

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
        InternalActor[] actors = new InternalActor[workerCount];
        ActorRef<Integer>[] actorRefs = new ActorRef[workerCount];
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            InternalActor actor = new InternalActor(workerIndex);
            actors[workerIndex] = actor;
            actorRefs[workerIndex] = actor.spawn();
        }

        // Set next actor pointers.
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            actors[workerIndex].next = actorRefs[(workerIndex + 1) % workerCount];
        }

        // Initiate the ring.
        actorRefs[0].send(ringSize);

        // Wait for actors to finish and collect the results.
        int[] sequences = new int[workerCount];
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            sequences[workerIndex] = actors[workerIndex].get();
        }
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new QuasarActorRingBenchmark().ringBenchmark();
    }

}
