package com.vlkan.fibertest.ring;

import kilim.*;
import kilim.tools.Kilim;
import org.openjdk.jmh.annotations.Benchmark;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.THREAD_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Kilim {@link Cell}s for message passing.
 */
public class KilimCellRingBenchmark implements RingBenchmark {

    private static class Worker extends Task<Integer> {

        private final int _id;

        private Worker next;

        private int sequence;

        private Cell<Integer> box = new Cell<>();

        private Worker(int id, Scheduler scheduler) {
            this._id = id;
            setScheduler(scheduler);
        }

        @Override
        public void execute() throws Pausable {
            log("[%2d] started", _id);
            do {
                log("[%2d] polling", _id);
                sequence = box.get();
                log("[%2d] signaling next (sequence=%d)", () -> new Object[] { _id, sequence });
                next.box.putnb(sequence - 1);
            } while (sequence > 0);
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {

        log("creating workers (THREAD_COUNT=%d, WORKER_COUNT=%d)", () -> new Object[] { THREAD_COUNT, WORKER_COUNT });
        Scheduler scheduler = new ForkJoinScheduler(THREAD_COUNT);
        int[] sequences = new int[WORKER_COUNT];
        Worker[] workers = new Worker[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex, scheduler);
        }

        log("setting next worker pointers");
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        log("starting workers");
        for (Worker worker : workers) {
            worker.start();
        }

        log("initiating ring (MESSAGE_PASSING_COUNT=%d)", MESSAGE_PASSING_COUNT);
        Worker firstWorker = workers[0];
        firstWorker.box.putnb(MESSAGE_PASSING_COUNT);

        log("waiting for scheduler to finish and shutting it down");
        scheduler.idledown();

        log("returning populated sequences");
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            sequences[workerIndex] = workers[workerIndex].sequence;
        }
        return sequences;

    }

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrace(String[] ignored) {
        new KilimCellRingBenchmark().ringBenchmark();
    }

    public static void main(String[] args) throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimCellRingBenchmark", "kilimEntrace", args);
    }

}
