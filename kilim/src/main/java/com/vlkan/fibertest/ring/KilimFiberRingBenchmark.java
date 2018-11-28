package com.vlkan.fibertest.ring;

import kilim.Pausable;
import kilim.PauseReason;
import kilim.Scheduler;
import kilim.Task;
import kilim.tools.Kilim;
import org.openjdk.jmh.annotations.Benchmark;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Kilim {@link Task}s with pause-and-resume.
 */
public class KilimFiberRingBenchmark implements RingBenchmark {

    static {
        Scheduler.defaultNumberThreads = 1;
    }

    private static final PauseReason PAUSE_REASON = task -> true;

    private static class Worker extends Task<Integer> {

        private final int _id;

        private final int[] sequences;

        private Worker next;

        private int sequence;

        private Worker(int id, int[] sequences) {
            this._id = id;
            this.sequences = sequences;
        }

        @Override
        public void execute() throws Pausable {
            do {
                Task.pause(PAUSE_REASON);
                next.sequence = sequence - 1;
                next.resume();
            } while (sequence > 0);
            sequences[_id] = sequence;
        }

        boolean started() {
            return running.get();
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {

        // Create workers.
        int[] sequences = new int[WORKER_COUNT];
        Worker[] workers = new Worker[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex, sequences);
        }

        // Set next worker pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        // Start workers.
        for (Worker worker : workers) {
            worker.start();
        }

        // Wait for workers to start.
        for (Worker worker : workers) {
            while (worker.started()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Initiate the ring.
        Worker firstWorker = workers[0];
        firstWorker.sequence = MESSAGE_PASSING_COUNT;
        firstWorker.resume();
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].joinb();
        }

        // Shutdown scheduler.
        Scheduler.getDefaultScheduler().shutdown();

        // Return collected sequences.
        return sequences;

    }

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrace(String[] ignored) {
        new KilimFiberRingBenchmark().ringBenchmark();
    }

    public static void main(String[] args) throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimFiberRingBenchmark", "kilimEntrace", args);
    }

}
