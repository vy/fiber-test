package com.vlkan.fibertest.ring;

import org.openjdk.jmh.annotations.Benchmark;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Java {@link Thread}s.
 */
public class JavaThreadRingBenchmark implements RingBenchmark {

    private static class Worker extends Thread {

        private final int id;

        private final int[] sequences;

        private Worker next = null;

        private volatile boolean waiting = true;

        private int sequence;

        private Worker(int id, int[] sequences) {
            super("Worker-"  + id);
            this.id = id;
            this.sequences = sequences;
        }

        @Override
        public void run() {
            for (;;) {
                synchronized (this) {
                    if (!waiting) {
                        // noinspection SynchronizeOnNonFinalField
                        synchronized (next) {
                            if (!next.waiting) {
                                String message = String.format("%s was expecting %s to be waiting", getName(), next.getName());
                                throw new IllegalStateException(message);
                            }
                            next.sequence = sequence - 1;
                            next.waiting = false;
                            waiting = true;
                            next.signal();
                        }
                        if (sequence <= 0) {
                            break;
                        }
                    }
                    await();
                }
            }
            sequences[id] = sequence;
        }

        private synchronized void await() {
            while (waiting) {
                try {
                    wait();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private void signal() {
            // Using notifyAll() rather than notify() since both a worker
            // and the main thread that is trying to join() is wait()'ing.
            notifyAll();
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() throws Exception {

        // Create worker threads.
        int[] sequences = new int[WORKER_COUNT];
        Worker[] workers = new Worker[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex] = new Worker(workerIndex, sequences);
        }

        // Set next worker thread pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workers[workerIndex].next = workers[(workerIndex + 1) % WORKER_COUNT];
        }

        // Start workers.
        for (Worker worker : workers) {
            worker.start();
        }

        // Ensure workers are started and waiting.
        for (Worker worker : workers) {
            // noinspection LoopConditionNotUpdatedInsideLoop, StatementWithEmptyBody
            while (worker.getState() != Thread.State.WAITING);
        }

        // Initiate the ring.
        Worker firstWorker = workers[0];
        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (firstWorker) {
            firstWorker.sequence = MESSAGE_PASSING_COUNT;
            firstWorker.waiting = false;
            firstWorker.signal();
        }

        // Wait for workers to complete.
        for (Worker worker : workers) {
            worker.join();
        }

        // Return populated sequences.
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new JavaThreadRingBenchmark().ringBenchmark();
    }

}
