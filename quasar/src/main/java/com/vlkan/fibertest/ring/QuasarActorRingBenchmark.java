package com.vlkan.fibertest.ring;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.common.monitoring.MonitorType;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import com.vlkan.fibertest.SingletonSynchronizer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.Arrays;
import java.util.concurrent.Callable;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.*;

/**
 * Ring benchmark using Quasar {@link Actor}s.
 */
@State(Scope.Benchmark)
public class QuasarActorRingBenchmark implements RingBenchmark {

    private static class Worker extends Actor<Integer, Void> {

        private final int id;

        private final SingletonSynchronizer completionSynchronizer;

        private ActorRef<Integer> next = null;

        private int sequence;

        private Worker(int id, SingletonSynchronizer completionSynchronizer) {
            super("QuasarActor-" + id, null);
            this.id = id;
            this.completionSynchronizer = completionSynchronizer;
        }

        @Override
        protected Void doRun() throws InterruptedException, SuspendExecution {
            log("[%2d] started", id);
            // noinspection InfiniteLoopStatement
            for (;;) {
                sequence = receive();
                if (sequence <= 0) {
                    log("[%2d] signaling completion (sequence=%d)", () -> new Object[]{id, sequence});
                    completionSynchronizer.signal();
                } else {
                    log("[%2d] signaling next (sequence=%d)", () -> new Object[]{id, sequence});
                    next.send(sequence - 1);
                }
            }
        }

    }

    private static final class Context implements AutoCloseable, Callable<int[]> {

        private final FiberScheduler scheduler = new FiberForkJoinScheduler("ChannelRingBenchmark", THREAD_COUNT, null, MonitorType.NONE, false);

        private final SingletonSynchronizer completionSynchronizer = new SingletonSynchronizer();

        private final int[] sequences = new int[WORKER_COUNT];

        private final Worker[] workers;

        private final ActorRef<Integer>[] actorRefs;

        private Context() {

            log("creating workers (WORKER_COUNT=%d)", WORKER_COUNT);
            this.workers = new Worker[WORKER_COUNT];
            // noinspection unchecked
            this.actorRefs = new ActorRef[WORKER_COUNT];
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                Worker worker = new Worker(workerIndex, completionSynchronizer);
                workers[workerIndex] = worker;
                actorRefs[workerIndex] = worker.spawn((FiberFactory) scheduler);
            }

            log("setting next worker pointers");
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                workers[workerIndex].next = actorRefs[(workerIndex + 1) % WORKER_COUNT];
            }

        }

        @Override
        public void close() {
            log("shutting down scheduler");
            scheduler.shutdown();
        }

        @Override
        public int[] call() throws Exception {

            log("initiating the ring (MESSAGE_PASSING_COUNT=%d)", MESSAGE_PASSING_COUNT);
            ActorRef<Integer> firstActorRef = actorRefs[0];
            firstActorRef.send(MESSAGE_PASSING_COUNT);

            log("waiting for completion");
            completionSynchronizer.await();

            log("collecting sequences");
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                sequences[workerIndex] = workers[workerIndex].sequence;
            }

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
    public int[] ringBenchmark() throws Exception {
        return context.call();
    }

    public static void main(String[] args) throws Exception {
        try (QuasarActorRingBenchmark benchmark = new QuasarActorRingBenchmark()) {
            benchmark.ringBenchmark();
        }
    }

}
