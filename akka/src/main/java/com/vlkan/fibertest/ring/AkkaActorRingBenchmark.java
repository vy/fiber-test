package com.vlkan.fibertest.ring;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.openjdk.jmh.annotations.Benchmark;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.*;

/**
 * Ring benchmark using Akka {@link akka.actor.Actor}s.
 */
public class AkkaActorRingBenchmark implements RingBenchmark {

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
        String configText = Stream
                .of(
                        "akka.log-dead-letters-during-shutdown = off",
                        "akka.log-dead-letters = off",
                        String.format(
                                "akka.actor.default-dispatcher.fork-join-executor { parallelism-min = %d, parallelism-max = %d }",
                                THREAD_COUNT, THREAD_COUNT))
                .collect(Collectors.joining(", "));
        Config config = ConfigFactory.parseString(configText);
        ActorSystem system = ActorSystem.create(AkkaActorRingBenchmark.class.getSimpleName() + "System", config);

        // Create actors.
        List<Future<Integer>> futures = new ArrayList<>(WORKER_COUNT);
        ActorRef[] actors = new ActorRef[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            Promise<Integer> promise = Futures.promise();
            futures.add(promise.future());
            Props actorProps = Props.create(InternalActor.class, workerIndex, promise);
            String actorName = "AkkaActor-" + workerIndex;
            actors[workerIndex] = system.actorOf(
                    actorProps,
                    actorName);
        }

        // Set next actor pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            actors[workerIndex].tell(actors[(workerIndex + 1) % WORKER_COUNT], null);
        }

        // Initiate the ring.
        actors[0].tell(MESSAGE_PASSING_COUNT, null);

        // Wait for the latch.
        Iterable<Integer> sequences = Await.result(
                Futures.sequence(futures, system.dispatcher()),
                Duration.apply(10, TimeUnit.MINUTES));

        // Shutdown actors.
        system.terminate();

        // Return populated sequences.
        return StreamSupport
                .stream(sequences.spliterator(), false)
                .mapToInt(sequence -> sequence)
                .toArray();

    }

    public static void main(String[] args) throws Exception {
        new AkkaActorRingBenchmark().ringBenchmark();
    }

}
