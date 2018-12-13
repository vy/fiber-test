package com.vlkan.fibertest.ring;

import co.paralleluniverse.common.monitoring.MonitorType;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.IntChannel;
import org.openjdk.jmh.annotations.Benchmark;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.THREAD_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Quasar {@link Fiber}s with {@link co.paralleluniverse.strands.channels.Channel}s.
 */
public class QuasarChannelRingBenchmark implements RingBenchmark {

    private static class InternalFiber extends Fiber<Integer> {

        private IntChannel subscriberChannel = Channels.newIntChannel(10_000);

        private IntChannel publisherChannel;

        private InternalFiber(int id, FiberScheduler scheduler) {
            super("QuasarFiber-" + id, scheduler);
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
        FiberScheduler scheduler = new FiberForkJoinScheduler("ChannelRingBenchmark", THREAD_COUNT, null, MonitorType.NONE, false);
        InternalFiber[] fibers = new InternalFiber[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            fibers[workerIndex] = new InternalFiber(workerIndex, scheduler);
        }

        // Set next fiber pointers.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            fibers[workerIndex].publisherChannel = fibers[(workerIndex + 1) % WORKER_COUNT].subscriberChannel;
        }

        // Start fibers.
        for (InternalFiber fiber : fibers) {
            fiber.start();
        }

        // Initiate the ring.
        InternalFiber first = fibers[0];
        first.subscriberChannel.send(MESSAGE_PASSING_COUNT);

        // Wait for fibers to complete.
        int[] sequences = new int[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            sequences[workerIndex] = fibers[workerIndex].get();
        }
        scheduler.shutdown();

        // Return populated sequences.
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new QuasarChannelRingBenchmark().ringBenchmark();
    }

}
