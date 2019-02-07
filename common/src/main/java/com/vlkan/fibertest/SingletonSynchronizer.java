package com.vlkan.fibertest;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SingletonSynchronizer {

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private boolean signalled = false;

    public SingletonSynchronizer() {}

    public void signal() {
        lock.lock();
        try {
            if (signalled) {
                throw new IllegalStateException("previous signal is not consumed yet");
            }
            signalled = true;
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void await() {
        lock.lock();
        try {
            while (!signalled) {
                condition.await();
            }
            signalled = false;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

}
