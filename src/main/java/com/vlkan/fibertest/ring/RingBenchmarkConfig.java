package com.vlkan.fibertest.ring;

public enum RingBenchmarkConfig {;

    public static final int WORKER_COUNT = Integer.parseInt(System.getProperty("ring.workerCount", "50"));

    public static final int MESSAGE_PASSING_COUNT = Integer.parseInt(System.getProperty("ring.messagePassingCount", "10000"));

}
