package com.vlkan.fibertest;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

public class FifoQueueTest {

    @Test
    public void test_invalid_capacity() {
        Stream
                .of(-1, 0)
                .forEach(invalidCapacity -> Assertions
                        .assertThatThrownBy(() -> new FifoQueue<>(invalidCapacity))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("invalid capacity"));
    }

    @Test
    public void test_with_ArrayBlockingQueue() {
        Random random = new Random(0);
        test_with_ArrayBlockingQueue(random, 1);
        for (int trialIndex = 0; trialIndex < 1_000; trialIndex++) {
            int capacity = 1 + random.nextInt(30);
            test_with_ArrayBlockingQueue(random, capacity);
        }
    }

    private void test_with_ArrayBlockingQueue(Random random, int capacity) {
        BlockingQueue<String> javaQueue = new ArrayBlockingQueue<>(capacity);
        FifoQueue<String> fifoQueue = new FifoQueue<>(capacity);
        for (int operationIndex = 0; operationIndex < 1_000; operationIndex++) {
            Assertions.assertThat(fifoQueue.size()).isEqualTo(javaQueue.size());
            boolean pullAllowed = random.nextBoolean();
            if (pullAllowed) {
                String javaQueueResponse = javaQueue.poll();
                String fifoQueueResponse = fifoQueue.dequeue();
                Assertions.assertThat(fifoQueueResponse).isEqualTo(javaQueueResponse);
            } else {
                String item = String.valueOf(random.nextInt());
                boolean pushed = javaQueue.offer(item);
                if (pushed) {
                    fifoQueue.enqueue(item);
                } else {
                    Assertions
                            .assertThatThrownBy(() -> fifoQueue.enqueue(item))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("queue is full");
                }
            }
            Assertions.assertThat(fifoQueue.size()).isEqualTo(javaQueue.size());
        }
    }

}
