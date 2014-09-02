package com.github.vy.fibertest;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.SuspendExecution;
import org.openjdk.jmh.annotations.Benchmark;

public class QuasarActorRingBenchmark extends AbstractRingBenchmark {

    protected static class InternalActor extends Actor {

        protected ActorRef next = null;

        public InternalActor(int id) {
            super(String.format("%s-%s-%d",
                    QuasarActorRingBenchmark.class.getSimpleName(),
                    InternalActor.class.getSimpleName(), id), null);
        }

        @Override
        protected Object doRun() throws InterruptedException, SuspendExecution {
            int sequence = Integer.MAX_VALUE;
            while (sequence > 0) {
                Object message = receive();
                if (message instanceof Integer) {
                    sequence = (Integer) message;
                    next.send(sequence - 1);
                }
            }
            return sequence;
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() throws Exception {
        // Create and start actors.
        final InternalActor[] actors = new InternalActor[workerCount];
        final ActorRef[] actorRefs = new ActorRef[workerCount];
        for (int i = 0; i < workerCount; i++) {
            InternalActor actor = new InternalActor(i);
            actors[i] = actor;
            actorRefs[i] = actor.spawn();
        }

        // Set next actor pointers.
        for (int i = 0; i < workerCount; i++)
            actors[i].next = actorRefs[(i+1) % workerCount];

        // Initiate the ring.
        actorRefs[0].send(ringSize);

        // Wait for actors to finish and collect the results.
        int[] sequences = new int[workerCount];
        for (int i = 0; i < workerCount; i++)
            sequences[i] = (int) actors[i].get();
        return sequences;
    }

    public static void main(String[] args) throws Exception {
        new QuasarActorRingBenchmark().ringBenchmark();
    }

}
