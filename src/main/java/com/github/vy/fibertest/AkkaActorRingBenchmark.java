package com.github.vy.fibertest;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import org.openjdk.jmh.annotations.Benchmark;

import java.util.concurrent.CountDownLatch;

/**
 * Ring benchmark using Akka actors.
 *
 * Internally actors use a {@link java.util.concurrent.CountDownLatch} to
 * notify the completion of the ring.
 */
public class AkkaActorRingBenchmark extends AbstractRingBenchmark {

    protected static class InternalActor extends UntypedActor {

        protected final int id;
        protected final int[] sequences;
        protected final CountDownLatch latch;
        protected ActorRef next = null;

        public InternalActor(final int id, final int[] sequences, final CountDownLatch latch) {
            this.id = id;
            this.sequences = sequences;
            this.latch = latch;
        }

        @Override
        public void onReceive(final Object message) throws Exception {
            if (message instanceof Integer) {
                final int sequence = (Integer) message;
                if (sequence < 1) {
                    sequences[id] = sequence;
                    latch.countDown();
                    getContext().stop(getSelf());
                }
                next.tell(sequence - 1, getSelf());
            }
            else if (message instanceof ActorRef) next = (ActorRef) message;
            else unhandled(message);
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() throws Exception {
        // Create an actor system and a shutdown latch.
        final ActorSystem system = ActorSystem.create(AkkaActorRingBenchmark.class.getSimpleName() + "System");
        final CountDownLatch latch = new CountDownLatch(workerCount);

        // Create actors.
        final int[] sequences = new int[workerCount];
        final ActorRef[] actors = new ActorRef[workerCount];
        for (int i = 0; i < workerCount; i++)
            actors[i] = system.actorOf(
                    Props.create(InternalActor.class, i, sequences, latch),
                    String.format("%s-%d", AkkaActorRingBenchmark.class.getSimpleName(), i));

        // Set next actor pointers.
        for (int i = 0; i < workerCount; i++)
            actors[i].tell(actors[(i+1) % workerCount], null);

        // Initiate the ring.
        actors[0].tell(ringSize, null);

        // Wait for the latch.
        latch.await();
        system.shutdown();
        return sequences;
    }

}
