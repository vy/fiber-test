package com.vlkan.fibertest.ring;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.dataflow.Var;
import org.openjdk.jmh.annotations.Benchmark;

public class QuasarDataflowRingBenchmark extends AbstractRingBenchmark {

    private static class InternalFiber extends Fiber<Integer> {

        private Var<Integer> current = new Var<>();

        private Var<Integer> next;

        private InternalFiber(int id) {
            super(String.format("%s-%s-%d",
                    QuasarChannelRingBenchmark.class.getSimpleName(),
                    InternalFiber.class.getSimpleName(), id));
        }

        @Override
        protected Integer run() throws SuspendExecution, InterruptedException {
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
    public int[] ringBenchmark() throws Exception {

        // Create fibers.
        InternalFiber[] fibers = new InternalFiber[workerCount];
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            fibers[workerIndex] = new InternalFiber(workerIndex);
        }

        // Set next fiber pointers.
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            fibers[workerIndex].next = fibers[(workerIndex + 1) % workerCount].current;
        }

        // Start fibers.
        for (InternalFiber fiber : fibers) {
            fiber.start();
        }

        // Initiate the ring.
        InternalFiber first = fibers[0];
        first.current.set(ringSize);

        // Wait for fibers to complete.
        int[] sequences = new int[workerCount];
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            sequences[workerIndex] = fibers[workerIndex].get();
        }
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new QuasarDataflowRingBenchmark().ringBenchmark();
    }

}
