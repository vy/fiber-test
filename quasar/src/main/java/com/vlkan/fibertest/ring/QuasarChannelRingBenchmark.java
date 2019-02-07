package com.vlkan.fibertest.ring;

import co.paralleluniverse.common.monitoring.MonitorType;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.IntChannel;
import com.vlkan.fibertest.SingletonSynchronizer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.Arrays;
import java.util.concurrent.Callable;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.THREAD_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Quasar {@link Fiber}s with {@link co.paralleluniverse.strands.channels.Channel}s.
 */
@State(Scope.Benchmark)
public class QuasarChannelRingBenchmark implements RingBenchmark {

    private static final class Worker extends Fiber<Integer> {

        private final IntChannel subscriberChannel = Channels.newIntChannel(1);

        private final int id;

        private final SingletonSynchronizer completionSynchronizer;

        private IntChannel publisherChannel;

        private int sequence;

        private Worker(int id, FiberScheduler scheduler, SingletonSynchronizer completionSynchronizer) {
            super("QuasarFiber-" + id, scheduler);
            this.id = id;
            this.completionSynchronizer = completionSynchronizer;
        }

        @Override
        protected Integer run() throws SuspendExecution, InterruptedException {
            log("[%2d] started", id);
            // noinspection InfiniteLoopStatement
            for (;;) {
                sequence = subscriberChannel.receive();
                if (sequence <= 0) {
                    log("[%2d] completed (sequence=%d)", () -> new Object[]{id, sequence});
                    completionSynchronizer.signal();
                } else {
                    log("[%2d] signaling next (sequence=%d)", () -> new Object[]{id, sequence});
                    publisherChannel.send(sequence - 1);
                }
            }
        }

    }

    private static final class Context implements AutoCloseable, Callable<int[]> {

        private final FiberScheduler scheduler = new FiberForkJoinScheduler("ChannelRingBenchmark", THREAD_COUNT, null, MonitorType.NONE, false);

        private final SingletonSynchronizer completionSynchronizer = new SingletonSynchronizer();

        private final int[] sequences = new int[WORKER_COUNT];

        private final Worker[] workers;

        private Context() {

            log("creating workers (WORKER_COUNT=%d)", WORKER_COUNT);
            this.workers = new Worker[WORKER_COUNT];
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                workers[workerIndex] = new Worker(workerIndex, scheduler, completionSynchronizer);
            }

            log("setting next worker pointers");
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                workers[workerIndex].publisherChannel = workers[(workerIndex + 1) % WORKER_COUNT].subscriberChannel;
            }

            log("starting fibers");
            for (Worker fiber : workers) {
                fiber.start();
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
            Worker firstWorker = workers[0];
            firstWorker.subscriberChannel.send(MESSAGE_PASSING_COUNT);

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
        try (QuasarChannelRingBenchmark benchmark = new QuasarChannelRingBenchmark()) {
            benchmark.ringBenchmark();
        }
    }

}
