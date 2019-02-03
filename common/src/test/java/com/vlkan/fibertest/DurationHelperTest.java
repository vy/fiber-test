package com.vlkan.fibertest;

import org.junit.Test;

import static com.vlkan.fibertest.DurationHelper.formatDurationNanos;
import static org.junit.Assert.assertEquals;

public class DurationHelperTest {

    @Test
    public void test_prettyPrintDurationNanos() {
        assertEquals("1h 5ns", formatDurationNanos(createDurationNanos(0, 1, 0, 0, 0, 5)));
        assertEquals("59m 1ms 999ns", formatDurationNanos(createDurationNanos(0, 0, 59, 0, 1, 999)));
        assertEquals("1d 1m 3s 10ms", formatDurationNanos(createDurationNanos(1, 0, 1, 3, 10, 0)));
        assertEquals("0ns", formatDurationNanos(createDurationNanos(0, 0, 0, 0, 0, 0)));
    }

    private static long createDurationNanos(long days, long hours, long mins, long secs, long millis, long nanos) {
        assert nanos >= 0 && nanos < 1_000_000;
        assert millis >= 0 && millis < 1_000;
        assert secs >= 0 && secs < 60;
        assert mins >=0 && mins < 60;
        assert hours >= 0 && hours < 24;
        assert days >= 0;
        return nanos + 1_000_000 * (millis + 1_000 * (secs + 60 * (mins + 60 * (hours + 24 * days))));
    }

}
