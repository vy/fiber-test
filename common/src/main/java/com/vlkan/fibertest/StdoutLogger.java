package com.vlkan.fibertest;

import java.time.Instant;
import java.util.function.Supplier;

public enum StdoutLogger {;

    private static final boolean ENABLED = PropertyHelper.readBooleanProperty("stdoutLogger.enabled", "false");

    public static void log(String fmt) {
        if (ENABLED) {
            System.out.format(Instant.now() + " [" + Thread.currentThread().getName() + "] " + fmt + "%n");
        }
    }

    public static void log(String fmt, Object arg) {
        if (ENABLED) {
            System.out.format(Instant.now() + " [" + Thread.currentThread().getName() + "] " + fmt + "%n", arg);
        }
    }

    public static void log(String fmt, Supplier<Object[]> argsSupplier) {
        if (ENABLED) {
            Object[] args = argsSupplier.get();
            System.out.format(Instant.now() + " [" + Thread.currentThread().getName() + "] " + fmt + "%n", args);
        }
    }

}
