package com.vlkan.fibertest.ring;

import com.vlkan.fibertest.FifoQueue;
import kilim.Continuation;
import kilim.Fiber;
import kilim.Pausable;
import kilim.tools.Kilim;
import org.openjdk.jmh.annotations.Benchmark;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Kilim {@link Continuation}s.
 */
public class KilimContinuationRingBenchmark implements RingBenchmark {

    private static class Worker extends Continuation {

        private final int _id;

        private final FifoQueue<Continuation> continuations;

        private Worker next;

        private int sequence;

        private Worker(int id, FifoQueue<Continuation> continuations) {
            this._id = id;
            this.continuations = continuations;
        }

        @Override
        public void execute() throws Pausable {
            log("[%2d] started", _id);
            do {
                log("[%2d] signaling sequence (sequence=%d)", () -> new Object[] { _id, sequence });
                next.sequence = sequence - 1;
                continuations.enqueue(next);
                log("[%2d] yielding", _id);
                Fiber.yield();
            } while (sequence > 0);
            log("[%2d] completed", _id);
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {

        log("creating workers (WORKER_COUNT=%d)", WORKER_COUNT);
        Worker[] workers = new Worker[WORKER_COUNT];
        FifoQueue<Continuation> continuations = new FifoQueue<>(1);
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex, continuations);
        }

        log("setting next worker pointers");
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        log("initiating the ring (MESSAGE_PASSING_COUNT=%d)", MESSAGE_PASSING_COUNT);
        Worker firstWorker = workers[0];
        firstWorker.sequence = MESSAGE_PASSING_COUNT;
        continuations.enqueue(firstWorker);

        log("executing scheduled continuations");
        for (Continuation continuation; (continuation = continuations.dequeue()) != null;) {
            continuation.run();
        }

        log("returning populated sequences");
        int[] sequences = new int[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            sequences[workerIndex] = workers[workerIndex].sequence;
        }
        return sequences;

    }

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrace(String[] ignored) {
        new KilimContinuationRingBenchmark().ringBenchmark();
    }

    public static void main(String[] args) throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimContinuationRingBenchmark", "kilimEntrace", args);
    }

}
