package com.vlkan.fibertest.ring;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import org.openjdk.jmh.annotations.Benchmark;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

public class QuasarFiberRingBenchmark implements RingBenchmark {

    private static class InternalFiber extends Fiber<Integer> {

        private InternalFiber next;

        private volatile boolean waiting = true;

        private int sequence;

        private InternalFiber(int id) {
            super(String.format("%s-%s-%d",
                    QuasarFiberRingBenchmark.class.getSimpleName(),
                    InternalFiber.class.getSimpleName(), id));
        }

        @Override
        protected Integer run() throws SuspendExecution {
            do {
                while (waiting) {
                    Strand.park();
                }
                waiting = true;
                next.sequence = sequence - 1;
                next.waiting = false;
                Strand.unpark(next);
            } while (sequence > 0);
            return sequence;
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() throws Exception {

        // Create fibers.
        InternalFiber[] fibers = new InternalFiber[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            fibers[workerIndex] = new InternalFiber(workerIndex);
        }

        // Set next fiber pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            fibers[workerIndex].next = fibers[(workerIndex + 1) % WORKER_COUNT];
        }

        // Start fibers.
        for (InternalFiber fiber : fibers) {
            fiber.start();
        }

        // Initiate the ring.
        InternalFiber first = fibers[0];
        first.sequence = MESSAGE_PASSING_COUNT;
        first.waiting = false;
        Strand.unpark(first);

        // Wait for fibers to complete.
        int[] sequences = new int[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            sequences[workerIndex] = fibers[workerIndex].get();
        }
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new QuasarFiberRingBenchmark().ringBenchmark();
    }

}
