package com.vlkan.fibertest;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Random;

public class SingletonSynchronizerTest {

    @Test
    public void test() {
        Random random = new Random(0);
        SingletonSynchronizer synchronizer = new SingletonSynchronizer();
        boolean signalled = false;
        for (int trialIndex = 0; trialIndex < 1_000; trialIndex++) {
            boolean signalAllowed = random.nextBoolean();
            if (signalAllowed) {
                if (signalled) {
                    Assertions
                            .assertThatThrownBy(synchronizer::signal)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("previous signal is not consumed yet");
                } else {
                    synchronizer.signal();
                    signalled = true;
                }
            } else if (signalled) {
                synchronizer.await();
                signalled = false;
            }
        }
    }

}
