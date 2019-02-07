package com.vlkan.fibertest;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static com.vlkan.fibertest.StdoutLogger.log;

/**
 * A not-thread-safe garbage-free array-backed bounded FIFO queue.
 */
@NotThreadSafe
public class FifoQueue<E> {

    private final int capacity;

    private final E[] items;

    private int dequeueIndex = 0;

    private int size = 0;

    public FifoQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("invalid capacity: " + capacity);
        }
        this.capacity = capacity;
        // noinspection unchecked
        this.items = (E[]) new Object[capacity];
    }

    public int capacity() {
        return capacity;
    }

    public void enqueue(E item) {
        log("enqueue (dequeueIndex=%d, size=%d, item=%s)", () -> new Object[] { dequeueIndex, size, item });
        boolean full = isFull();
        if (full) {
            throw new IllegalStateException("queue is full");
        }
        int enqueueIndex = (dequeueIndex + size) % capacity;
        items[enqueueIndex] = item;
        size++;
    }

    public boolean isFull() {
        return size == capacity;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    @Nullable
    public E dequeue() {
        log("dequeue (dequeueIndex=%d, size=%d)", () -> new Object[] { dequeueIndex, size });
        boolean empty = isEmpty();
        if (empty) {
            return null;
        }
        size--;
        E item = items[dequeueIndex];
        dequeueIndex = (dequeueIndex + 1) % capacity;
        return item;
    }

}
