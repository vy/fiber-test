package com.vlkan.fibertest;

import java.util.LinkedList;
import kilim.Continuation;
import kilim.Fiber;
import kilim.Pausable;
import kilim.tools.Kilim;
import org.openjdk.jmh.annotations.Benchmark;

/**
 * Ring benchmark using Kilim continuations.
 */
public class KilimContinuationRingBenchmark extends AbstractRingBenchmark {

    static LinkedList<Continuation> queue = new LinkedList();

    static void scheduler() {
        for (Continuation cc; ((cc = queue.pollFirst()) != null); )
            cc.run();
    }
    static void resume(InternalFiber task) {
        if (! task.done)
            queue.add(task);
    }
    
    public static class InternalFiber extends Continuation {
        private final int sid;
        private final int[] sequences;
        private InternalFiber next;
        private int sequence;
        private boolean done;
        

        private InternalFiber(int id, int[] sequences) {
            this.sid = id;
            this.sequences = sequences;
        }

        @Override
        public void execute() throws Pausable {
            while (true) {
                next.sequence = sequence - 1;
                resume(next);
                if (sequence <= 0)
                    break;
                Fiber.yield();
            }
            done = true;
            sequences[sid] = sequence;
        }
    }

    @Override
    @Benchmark
    public int[] ringBenchmark() {

        // Create fibers.
        int[] sequences = new int[workerCount];
        InternalFiber[] fibers = new InternalFiber[workerCount];
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            fibers[workerIndex] = new InternalFiber(workerIndex, sequences);
        }

        // Set next fiber pointers.
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            fibers[workerIndex].next = fibers[(workerIndex + 1) % workerCount];
        }

        // Initiate the ring.
        InternalFiber firstFiber = fibers[0];
        firstFiber.sequence = ringSize;
        resume(firstFiber);
        
        scheduler();

        return sequences;

    }
    
    public static void main(String[] args) {
        if (Kilim.trampoline(true,args)) return;
        int [] seqs = new KilimContinuationRingBenchmark().ringBenchmark();
    }


}
