package com.github.vy.fibertest;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.IntChannel;
import org.openjdk.jmh.annotations.Benchmark;

public class QuasarChannelRingBenchmark extends AbstractRingBenchmark {

    private static class InternalFiber extends Fiber<Integer> {

        private IntChannel subscriberChannel = Channels.newIntChannel(10000);

        private IntChannel publisherChannel;

        private InternalFiber(int id) {
            super(String.format("%s-%s-%d",
                    QuasarChannelRingBenchmark.class.getSimpleName(),
                    InternalFiber.class.getSimpleName(), id));
        }

        @Override
        protected Integer run() throws SuspendExecution, InterruptedException {
            Integer sequence;
            do {
                sequence = subscriberChannel.receive();
                publisherChannel.send(sequence - 1);
            } while (sequence > 0);
            subscriberChannel.close();
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
            fibers[workerIndex].publisherChannel = fibers[(workerIndex + 1) % workerCount].subscriberChannel;
        }

        // Start fibers.
        for (InternalFiber fiber : fibers) {
            fiber.start();
        }

        // Initiate the ring.
        InternalFiber first = fibers[0];
        first.subscriberChannel.send(ringSize);

        // Wait for fibers to complete.
        int[] sequences = new int[workerCount];
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            sequences[workerIndex] = fibers[workerIndex].get();
        }
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new QuasarChannelRingBenchmark().ringBenchmark();
    }

}
