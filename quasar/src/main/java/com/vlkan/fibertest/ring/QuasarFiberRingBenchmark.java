package com.vlkan.fibertest.ring;

import co.paralleluniverse.common.monitoring.MonitorType;
import co.paralleluniverse.fibers.*;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import org.openjdk.jmh.annotations.Benchmark;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import static com.vlkan.fibertest.StdoutLogger.log;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.MESSAGE_PASSING_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.THREAD_COUNT;
import static com.vlkan.fibertest.ring.RingBenchmarkConfig.WORKER_COUNT;

/**
 * Ring benchmark using Quasar {@link Fiber}s.
 */
public class QuasarFiberRingBenchmark implements RingBenchmark {

    private static class InternalFiber extends Fiber<Integer> {

        private final Lock lock = new ReentrantLock();

        private final Condition notWaiting = lock.newCondition();

        private final int id;

        private InternalFiber next;

        private volatile boolean waiting = true;

        private int sequence;

        private InternalFiber(int id, FiberScheduler scheduler) {
            super("QuasarFiber-" + id, scheduler);
            this.id = id;
        }

        @Override
        protected Integer run() throws SuspendExecution {
            for (;;) {
                log("[%2d] locking", id);
                lock.lock();
                try {
                    if (!waiting) {
                        log("[%2d] locking next", id);
                        next.lock.lock();
                        try {
                            log("[%2d] signaling next (sequence=%d)", id, sequence);
                            if (!next.waiting) {
                                String message = String.format("%s was expecting %s to be waiting", id, next.id);
                                throw new IllegalStateException(message);
                            }
                            next.sequence = sequence - 1;
                            next.waiting = false;
                            waiting = true;
                            next.notWaiting.signal();
                        } finally {
                            log("[%2d] unlocking next", id);
                            next.lock.unlock();
                        }
                        if (sequence <= 0) {
                            return sequence;
                        }
                    }
                    await();
                } finally {
                    log("[%2d] unlocking", id);
                    lock.unlock();
                }
            }
        }

        private void await() throws SuspendExecution {
            while (waiting) {
                try {
                    log("[%2d] awaiting", id);
                    notWaiting.await();
                    log("[%2d] woke up", id);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

    }

    @Override
    @Benchmark
    public int[] ringBenchmark() throws Exception {

        log("creating fibers (THREAD_COUNT=%d, WORKER_COUNT=%d)", THREAD_COUNT, WORKER_COUNT);
        FiberScheduler scheduler = new FiberForkJoinScheduler("FiberRingBenchmark", THREAD_COUNT, null, MonitorType.NONE, false);
        InternalFiber[] fibers = new InternalFiber[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            fibers[workerIndex] = new InternalFiber(workerIndex, scheduler);
        }

        log("setting next fiber pointers");
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            fibers[workerIndex].next = fibers[(workerIndex + 1) % WORKER_COUNT];
        }

        log("starting fibers");
        for (InternalFiber fiber : fibers) {
            fiber.start();
        }

        log("ensuring fibers are started and waiting");
        for (InternalFiber fiber : fibers) {
            while (fiber.getState() != Strand.State.WAITING);
        }

        log("initiating the ring (MESSAGE_PASSING_COUNT=%d)", MESSAGE_PASSING_COUNT);
        InternalFiber firstFiber = fibers[0];
        firstFiber.lock.lock();
        try {
            firstFiber.sequence = MESSAGE_PASSING_COUNT;
            firstFiber.waiting = false;
            firstFiber.notWaiting.signal();
        } finally {
            firstFiber.lock.unlock();
        }

        log("waiting for fibers to complete");
        int[] sequences = new int[WORKER_COUNT];
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            sequences[workerIndex] = fibers[workerIndex].get();
        }
        scheduler.shutdown();

        log("returning populated sequences");
        return sequences;

    }

    public static void main(String[] args) throws Exception {
        new QuasarFiberRingBenchmark().ringBenchmark();
    }

}
