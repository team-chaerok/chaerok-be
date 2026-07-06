package com.chaerok.backend.place.service;

import java.math.BigDecimal;

public class TourApiValueMapper {

    private TourApiValueMapper() {
    }

    public static String valueOrFallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    public static BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static BigDecimal toBigDecimalOrFallback(String value, BigDecimal fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}