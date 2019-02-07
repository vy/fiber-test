package com.vlkan.fibertest.ring;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vlkan.fibertest.SingletonSynchronizer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.*;

/**
 * Ring benchmark using Akka {@link akka.actor.Actor}s.
 */
@State(Scope.Benchmark)
public class AkkaActorRingBenchmark implements RingBenchmark {

    /** @noinspection deprecation (easier to use in Java) */
    private static final class Worker extends UntypedActor {

        private final int id;

        private final int[] sequences;

        private final SingletonSynchronizer completionSynchronizer;

        private ActorRef next = null;

        private Worker(int id, int[] sequences, SingletonSynchronizer completionSynchronizer) {
            this.id = id;
            this.sequences = sequences;
            this.completionSynchronizer = completionSynchronizer;
        }

        @Override
        public void onReceive(final Object message) {
            log("[%2d] started", id);
            if (message instanceof Integer) {
                int sequence = sequences[id] = (Integer) message;
                if (sequence <= 0) {
                    log("[%2d] signaling completion (sequence=%d)", () -> new Object[]{id, sequence});
                    completionSynchronizer.signal();
                } else {
                    log("[%2d] signaling next (sequence=%d)", () -> new Object[]{id, sequence});
                    next.tell(sequence - 1, getSelf());
                }
            } else if (message instanceof ActorRef) {
                next = (ActorRef) message;
            } else {
                unhandled(message);
            }
        }

    }

    private static final class Context implements AutoCloseable, Callable<int[]> {

        private final int[] sequences = new int[WORKER_COUNT];

        private final SingletonSynchronizer completionSynchronizer = new SingletonSynchronizer();

        private final ActorSystem system;

        private final ActorRef[] actors;

        private Context() {

            log("creating the actor system (THREAD_COUNT=%d)", THREAD_COUNT);
            String configText = Stream
                    .of(
                            "akka.log-dead-letters-during-shutdown = off",
                            "akka.log-dead-letters = off",
                            String.format(
                                    "akka.actor.default-dispatcher.fork-join-executor { parallelism-min = %d, parallelism-max = %d }",
                                    THREAD_COUNT, THREAD_COUNT))
                    .collect(Collectors.joining(", "));
            Config config = ConfigFactory.parseString(configText);
            this.system = ActorSystem.create("AkkaActorRingBenchmarkSystem", config);

            log("creating actors (WORKER_COUNT=%d)", WORKER_COUNT);
            this.actors = new ActorRef[WORKER_COUNT];
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                Props actorProps = Props.create(Worker.class, workerIndex, sequences, completionSynchronizer);
                String actorName = "AkkaActor-" + workerIndex;
                actors[workerIndex] = system.actorOf(actorProps, actorName);
            }

            log("setting next actor pointers");
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                ActorRef nextActorRef = actors[(workerIndex + 1) % WORKER_COUNT];
                actors[workerIndex].tell(nextActorRef, null);
            }

        }

        @Override
        public void close() {
            log("terminating");
            system.terminate();
        }

        @Override
        public int[] call() {

            log("initiating the ring (MESSAGE_PASSING_COUNT=%d)", MESSAGE_PASSING_COUNT);
            ActorRef firstActor = actors[0];
            firstActor.tell(MESSAGE_PASSING_COUNT, null);

            log("waiting for completion");
            completionSynchronizer.await();

            log("returning populated sequences (sequences=%s)", () -> new Object[]{Arrays.toString(sequences)});
            return sequences;

        }

    }

    private final Context context = new Context();

    @Override
    @TearDown
    public void close() {
        context.close();
    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {
        return context.call();
    }

    public static void main(String[] args) {
        try (AkkaActorRingBenchmark benchmark = new AkkaActorRingBenchmark()) {
            benchmark.ringBenchmark();
        }
    }

}
