package com.github.vy.fibertest;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.IntChannel;
import org.openjdk.jmh.annotations.Benchmark;

public class QuasarChannelRingBenchmark extends AbstractRingBenchmark {

    protected static class InternalFiber extends Fiber<Integer> {

        protected IntChannel subscriberChannel = Channels.newIntChannel(10000);
        protected IntChannel publisherChannel;

        public InternalFiber(final int id) {
            super(String.format("%s-%s-%d",
                    QuasarChannelRingBenchmark.class.getSimpleName(),
                    InternalFiber.class.getSimpleName(), id));
        }

        @Override
        @Suspendable
        public Integer run() throws SuspendExecution, InterruptedException {
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
        final InternalFiber[] fibers = new InternalFiber[workerCount];
        for (int i = 0; i < workerCount; i++)
            fibers[i] = new InternalFiber(i);

        // Set next fiber pointers.
        for (int i = 0; i < workerCount; i++)
            fibers[i].publisherChannel = fibers[(i+1) % workerCount].subscriberChannel;

        // Start fibers.
        for (final InternalFiber fiber : fibers) fiber.start();

        // Initiate the ring.
        final InternalFiber first = fibers[0];
        first.subscriberChannel.send(ringSize);

        // Wait for fibers to complete.
        final int[] sequences = new int[workerCount];
        for (int i = 0; i < workerCount; i++)
            sequences[i] = fibers[i].get();
        return sequences;
    }

    public static void main(String[] args) throws Exception {
        new QuasarChannelRingBenchmark().ringBenchmark();
    }

}
