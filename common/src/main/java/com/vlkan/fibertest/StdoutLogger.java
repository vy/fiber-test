package com.vlkan.fibertest;

import java.time.Instant;

public enum StdoutLogger {;

    private static final boolean ENABLED = PropertyHelper.readBooleanProperty("stdoutLogger.enabled", "false");

    public static synchronized void log(String fmt, Object... args) {
        if (ENABLED) {
            System.out.format(Instant.now() + " [" + Thread.currentThread().getName() + "] " + fmt + "%n", args);
        }
    }

}
