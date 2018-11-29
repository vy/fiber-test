package com.vlkan.fibertest;

public enum PropertyHelper {;

    public static int readPositiveNonZeroIntegerProperty(String name, String defaultValue) {
        String value = System.getProperty(name, defaultValue);
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("illegal number: " + value, error);
        }
        if (number < 1) {
            throw new IllegalArgumentException("illegal positive non-zero number: " + number);
        }
        return number;
    }

}
