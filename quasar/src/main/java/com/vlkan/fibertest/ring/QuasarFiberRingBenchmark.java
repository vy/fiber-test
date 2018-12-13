package com.vlkan.fibertest.ring;

import co.paralleluniverse.common.monitoring.MonitorType;
import co.paralleluniverse.fibers.*;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.strands.Strand;
import org.openjdk.jmh.annotations.Benchmark;

import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.THREAD_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Quasar {@link Fiber}s.
 */
public class QuasarFiberRingBenchmark implements RingBenchmark {

    private static class InternalFiber extends Fiber<Integer> {

        private InternalFiber next;

        private volatile boolean waiting = true;

        private int sequence;

        private InternalFiber(int id, FiberScheduler scheduler) {
            super("QuasarFiber-" + id, scheduler);
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
        FiberScheduler scheduler = new FiberForkJoinScheduler("FiberRingBenchmark", THREAD_COUNT, null, MonitorType.NONE, false);
        InternalFiber[] fibers = new InternalFiber[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            fibers[workerIndex] = new InternalFiber(workerIndex, scheduler);
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
        InternalFiber firstFiber = fibers[0];
        firstFiber.sequence = MESSAGE_PASSING_COUNT;
        firstFiber.waiting = false;
        Strand.unpark(firstFiber);

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
        new QuasarFiberRingBenchmark().ringBenchmark();
    }

}
