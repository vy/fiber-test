package com.github.vy.fibertest;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import org.openjdk.jmh.annotations.Benchmark;

public class QuasarFiberRingBenchmark extends AbstractRingBenchmark {

    protected static class InternalFiber extends Fiber<Integer> {

        protected InternalFiber next;
        protected volatile boolean waiting = true;
        protected int sequence = Integer.MAX_VALUE;

        public InternalFiber(final int id) {
            super(String.format("%s-%s-%d",
                    QuasarFiberRingBenchmark.class.getSimpleName(),
                    InternalFiber.class.getSimpleName(), id));
        }

        @Override
        @Suspendable
        public Integer run() throws SuspendExecution, InterruptedException {
            while (sequence > 0) {
                while (waiting) { Strand.park(); }
                waiting = true;
                next.sequence = sequence - 1;
                next.waiting = false;
                Strand.unpark(next);
            }
            return sequence;
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() throws Exception {
        // Create fibers.
        final InternalFiber[] fibers = new InternalFiber[workerCount];
        for (int i = 0; i < workerCount; i++)
            fibers[i] = new InternalFiber(i);

        // Set next fiber pointers.
        for (int i = 0; i < workerCount; i++)
            fibers[i].next = fibers[(i+1) % workerCount];

        // Start fibers.
        for (final InternalFiber fiber : fibers) fiber.start();

        // Initiate the ring.
        final InternalFiber first = fibers[0];
        first.sequence = ringSize;
        first.waiting = false;
        Strand.unpark(first);

        // Wait for fibers to complete.
        final int[] sequences = new int[workerCount];
        for (int i = 0; i < workerCount; i++)
            sequences[i] = fibers[i].get();
        return sequences;
    }

}
