package com.vlkan.fibertest.ring;

import kilim.*;
import kilim.tools.Kilim;
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
 * Ring benchmark using Kilim {@link Cell}s for message passing.
 */
@State(Scope.Benchmark)
public class KilimCellRingBenchmark implements RingBenchmark {

    private enum Completed { INSTANCE }

    private static final class Worker extends Task<Integer> {

        private final int _id;

        private final Cell<Completed> completedCell;

        private Worker next;

        private int sequence;

        private Cell<Integer> sequenceCell = new Cell<>();

        private Worker(int id, Cell<Completed> completedCell, Scheduler scheduler) {
            this._id = id;
            this.completedCell = completedCell;
            setScheduler(scheduler);
        }

        @Override
        public void execute() throws Pausable {
            log("[%2d] started", _id);
            // noinspection InfiniteLoopStatement
            for (;;) {
                log("[%2d] polling", _id);
                sequence = sequenceCell.get();
                if (sequence <= 0) {
                    log("[%2d] completed (sequence=%d)", () -> new Object[] { _id, sequence });
                    completedCell.put(Completed.INSTANCE);
                } else {
                    log("[%2d] signaling next (sequence=%d)", () -> new Object[]{_id, sequence});
                    next.sequenceCell.putnb(sequence - 1);
                }
            }
        }

        @Override
        public String toString() {
            return "Worker-" + _id;
        }

    }

    private static final class Context implements AutoCloseable, Callable<int[]> {

        private final Cell<Completed> completedCell = new Cell<>();

        private final Scheduler scheduler = new ForkJoinScheduler(THREAD_COUNT);

        private final int[] sequences = new int[WORKER_COUNT];

        private final Worker[] workers;

        private Context() {

            log("creating workers (WORKER_COUNT=%d)", WORKER_COUNT);
            this.workers = new Worker[WORKER_COUNT];
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                workers[workerIndex] = new Worker(workerIndex, completedCell, scheduler);
            }

            log("setting next worker pointers");
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
            }

            log("starting workers");
            for (Worker worker : workers) {
                worker.start();
            }

        }

        @Override
        public void close() {
            log("shutting down scheduler");
            scheduler.shutdown();
        }

        @Override
        public int[] call() {

            log("initiating ring (MESSAGE_PASSING_COUNT=%d)", MESSAGE_PASSING_COUNT);
            Worker firstWorker = workers[0];
            firstWorker.sequenceCell.putnb(MESSAGE_PASSING_COUNT);

            log("waiting for completion");
            completedCell.getb();

            log("collecting sequences");
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                sequences[workerIndex] = workers[workerIndex].sequence;
            }

            log("returning collected sequences (sequences=%s)", () -> new Object[] { Arrays.toString(sequences) });
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

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrace(String[] ignored) {
        try (KilimCellRingBenchmark benchmark = new KilimCellRingBenchmark()) {
            benchmark.ringBenchmark();
        }
    }

    public static void main(String[] args) throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimCellRingBenchmark", "kilimEntrace", args);
    }

}
