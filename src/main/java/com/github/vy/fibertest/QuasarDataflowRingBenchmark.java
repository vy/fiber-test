package com.github.vy.fibertest;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.dataflow.Var;
import org.openjdk.jmh.annotations.Benchmark;

public class QuasarDataflowRingBenchmark extends AbstractRingBenchmark {

    protected static class InternalFiber extends Fiber<Integer> {

        protected Var<Integer> current = new Var<>();
        protected Var<Integer> next;

        public InternalFiber(final int id) {
            super(String.format("%s-%s-%d",
                    QuasarChannelRingBenchmark.class.getSimpleName(),
                    InternalFiber.class.getSimpleName(), id));
        }

        @Override
        public Integer run() throws SuspendExecution, InterruptedException {
            Integer sequence;
            do {
                sequence = current.getNext();
                next.set(sequence - 1);
            } while (sequence > 0);
            return sequence;
        }
    }

    @Override
    @Benchmark
    public Integer[] ringBenchmark() throws Exception {
        // Create fibers.
        final InternalFiber[] fibers = new InternalFiber[workerCount];
        for (int i = 0; i < workerCount; i++)
            fibers[i] = new InternalFiber(i);

        // Set next fiber pointers.
        for (int i = 0; i < workerCount; i++)
            fibers[i].next = fibers[(i+1) % workerCount].current;

        // Start fibers.
        for (final InternalFiber fiber : fibers) fiber.start();

        // Initiate the ring.
        final InternalFiber first = fibers[0];
        first.current.set(ringSize);

        // Wait for fibers to complete.
        final Integer[] sequences = new Integer[workerCount];
        for (int i = 0; i < workerCount; i++)
            sequences[i] = fibers[i].get();
        return sequences;
    }

    public static void main(String[] args) throws Exception {
        new QuasarDataflowRingBenchmark().ringBenchmark();
    }

}
