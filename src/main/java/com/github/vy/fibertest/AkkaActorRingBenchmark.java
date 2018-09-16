package com.github.vy.fibertest;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import com.google.common.primitives.Ints;
import org.openjdk.jmh.annotations.Benchmark;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ring benchmark using Akka actors.
 *
 * Internally actors use a {@link scala.concurrent.Promise} to
 * notify the completion of the ring.
 */
public class AkkaActorRingBenchmark extends AbstractRingBenchmark {

    /** @noinspection deprecation (easier to use in Java) */
    private static class InternalActor extends UntypedActor {

        /** @noinspection unused (kept for debugging purposes) */
        private final int id;

        private final Promise<Integer> promise;

        private ActorRef next = null;

        private InternalActor(int id, Promise<Integer> promise) {
            this.id = id;
            this.promise = promise;
        }

        @Override
        public void onReceive(final Object message) {
            if (message instanceof Integer) {
                int sequence = (Integer) message;
                if (sequence < 1) {
                    promise.success(sequence);
                    getContext().stop(getSelf());
                }
                next.tell(sequence - 1, getSelf());
            } else if (message instanceof ActorRef) {
                next = (ActorRef) message;
            } else {
                unhandled(message);
            }
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() throws Exception {

        // Create the actor system.
        ActorSystem system = ActorSystem.create(AkkaActorRingBenchmark.class.getSimpleName() + "System");

        // Create actors.
        List<Future<Integer>> futures = new ArrayList<>(workerCount);
        ActorRef[] actors = new ActorRef[workerCount];
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            Promise<Integer> promise = Futures.promise();
            futures.add(promise.future());
            Props actorProps = Props.create(InternalActor.class, workerIndex, promise);
            String actorName = String.format("%s-%d", AkkaActorRingBenchmark.class.getSimpleName(), workerIndex);
            actors[workerIndex] = system.actorOf(
                    actorProps,
                    actorName);
        }

        // Set next actor pointers.
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            actors[workerIndex].tell(actors[(workerIndex + 1) % workerCount], null);
        }

        // Initiate the ring.
        actors[0].tell(ringSize, null);

        // Wait for the latch.
        Iterable<Integer> sequences = Await.result(
                Futures.sequence(futures, system.dispatcher()),
                Duration.apply(10, TimeUnit.MINUTES));
        system.terminate();
        return Ints.toArray((Collection<Integer>) sequences);

    }

    public static void main(String[] args) throws Exception {
        new AkkaActorRingBenchmark().ringBenchmark();
    }

}
