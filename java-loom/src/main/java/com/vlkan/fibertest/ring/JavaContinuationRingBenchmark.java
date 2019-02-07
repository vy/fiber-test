package com.vlkan.fibertest.ring;

import com.vlkan.fibertest.FifoQueue;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.Arrays;
import java.util.concurrent.Callable;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Java {@link Continuation}s.
 */
@State(Scope.Benchmark)
public class JavaContinuationRingBenchmark implements RingBenchmark {

    private static final class Worker implements Runnable {

        private final ContinuationScope scope;

        private final int id;

        private final FifoQueue<Continuation> executionQueue;

        private Continuation continuation;

        private Worker next;

        private int sequence;

        private Worker(ContinuationScope scope, int id, FifoQueue<Continuation> executionQueue) {
            this.scope = scope;
            this.id = id;
            this.executionQueue = executionQueue;
        }

        @Override
        public void run() {
            log("[%2d] started", id);
            // noinspection InfiniteLoopStatement
            for (;;) {
                log("[%2d] yielding", id);
                Continuation.yield(scope);
                if (sequence <= 0) {
                    log("[%2d] completed", id);
                } else {
                    log("[%2d] signaling sequence (sequence=%d)", () -> new Object[]{id, sequence});
                    next.sequence = sequence - 1;
                    executionQueue.enqueue(next.continuation);
                }
            }
        }

    }

    private static final class Context implements Callable<int[]> {

        private final FifoQueue<Continuation> executionQueue = new FifoQueue<>(1);

        private final int[] sequences = new int[WORKER_COUNT];

        private final Worker[] workers;

        private Context() {

            log("creating workers (WORKER_COUNT=%d)", WORKER_COUNT);
            this.workers = new Worker[WORKER_COUNT];
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                ContinuationScope scope = new ContinuationScope(String.format("W-%d", workerIndex));
                Worker worker = new Worker(scope, workerIndex, executionQueue);
                worker.continuation = new Continuation(scope, worker);
                workers[workerIndex] = worker;
            }

            log("setting next worker pointers");
            for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
                workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
            }

            log("starting workers");
            for (Worker worker : workers) {
                worker.continuation.run();
            }

        }

        @Override
        public int[] call() {

            log("initiating ring (MESSAGE_PASSING_COUNT=%d)", MESSAGE_PASSING_COUNT);
            Worker firstWorker = workers[0];
            firstWorker.sequence = MESSAGE_PASSING_COUNT;
            executionQueue.enqueue(firstWorker.continuation);

            log("executing scheduled continuations");
            for (Continuation continuation; (continuation = executionQueue.dequeue()) != null;) {
                if (!continuation.isDone()) {
                    continuation.run();
                }
            }

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
    public void close() {}

    @Override
    @Benchmark
    public int[] ringBenchmark() {
        return context.call();
    }

    public static void main(String[] args) {
        try (JavaContinuationRingBenchmark benchmark = new JavaContinuationRingBenchmark()) {
            benchmark.ringBenchmark();
        }
    }

}
