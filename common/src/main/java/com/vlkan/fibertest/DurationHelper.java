package com.vlkan.fibertest;

public enum DurationHelper {;

    public static String formatDurationSinceNanos(long startTimeNanos) {
        long durationNanos = Math.max(0, System.nanoTime() - startTimeNanos);
        return formatDurationNanos(durationNanos);
    }

    public static String formatDurationNanos(long durationNanos) {
        if (durationNanos < 0) {
            throw new IllegalArgumentException("negative duration");
        }
        String[] fields = new String[6];
        long nanos = durationNanos % 1_000_000;
        if (nanos > 0) {
            fields[0] = nanos + "ns";
        }
        long durationMillis = (durationNanos - nanos) / 1_000_000;
        if (durationMillis > 0) {
            long millis = durationMillis % 1_000;
            if (millis > 0) {
                fields[1] = millis + "ms";
            }
            long durationSecs = (durationMillis - millis) / 1_000;
            if (durationSecs > 0) {
                if (durationSecs < 60) {
                    fields[2] = durationSecs + "s";
                } else {
                    long secs = durationSecs % 60;
                    if (secs > 0) {
                        fields[2] = secs + "s";
                    }
                    long durationMins = (durationSecs - secs) / 60;
                    if (durationMins > 0) {
                        if (durationMins < 60) {
                            fields[3] = durationMins + "m";
                        } else {
                            long mins = durationMins % 60;
                            if (mins > 0) {
                                fields[3] = mins + "m";
                            }
                            long durationHours = (durationMins - mins) / 60;
                            if (durationHours > 0) {
                                if (durationHours < 24) {
                                    fields[4] = durationHours + "h";
                                } else {
                                    long hours = durationHours % 24;
                                    if (hours > 0) {
                                        fields[4] = hours + "h";
                                    }
                                    long days = (durationHours - hours) / 24;
                                    if (days > 0) {
                                        fields[5] = days + "d";
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (int fieldIndex = fields.length - 1; fieldIndex >= 0; fieldIndex--) {
            String field = fields[fieldIndex];
            if (field != null) {
                if (first) {
                    first = false;
                } else {
                    builder.append(' ');
                }
                builder.append(field);
            }
        }
        return first ? "0ns" : builder.toString();
    }

}
