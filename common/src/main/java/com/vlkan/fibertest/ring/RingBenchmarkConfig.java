package com.vlkan.fibertest.ring;

import com.vlkan.fibertest.PropertyHelper;

public enum RingBenchmarkConfig {;

    public static final int WORKER_COUNT = PropertyHelper.readPositiveNonZeroIntegerProperty("ring.workerCount", "50");

    public static final int MESSAGE_PASSING_COUNT = PropertyHelper.readPositiveNonZeroIntegerProperty("ring.messagePassingCount", "10000");

}
