package com.vlkan.fibertest;

public enum PropertyHelper {;

    public static int readIntegerPropertyGreaterThanOrEqualTo(String name, String defaultValue, int minValue) {
        String value = System.getProperty(name, defaultValue);
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException error) {
            String message = String.format(
                    "illegal integer property (name=%s, defaultValue=%s, minValue=%d, value=%s)",
                    name, defaultValue, minValue, value);
            throw new IllegalArgumentException(message);
        }
        if (number < minValue) {
            String message = String.format(
                    "illegal integer property (name=%s, defaultValue=%s, minValue=%d, value=%s)",
                    name, defaultValue, minValue, value);
            throw new IllegalArgumentException(message);
        }
        return number;
    }

    public static boolean readBooleanProperty(String name, String defaultValue) {
        String value = System.getProperty(name, defaultValue);
        return Boolean.parseBoolean(value);
    }

}
