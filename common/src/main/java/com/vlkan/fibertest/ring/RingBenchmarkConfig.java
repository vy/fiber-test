package com.vlkan.fibertest.ring;

import com.vlkan.fibertest.PropertyHelper;

public enum RingBenchmarkConfig {;

    public static final int THREAD_COUNT = PropertyHelper.readIntegerPropertyGreaterThanOrEqualTo("ring.threadCount", "1", 1);

    public static final int WORKER_COUNT = PropertyHelper.readIntegerPropertyGreaterThanOrEqualTo("ring.workerCount", "50", 2);

    public static final int MESSAGE_PASSING_COUNT = PropertyHelper.readIntegerPropertyGreaterThanOrEqualTo("ring.messagePassingCount", "1000000", 1);

}
