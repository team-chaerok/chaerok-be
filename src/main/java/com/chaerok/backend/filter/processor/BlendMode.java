package com.chaerok.backend.filter.processor;

public enum BlendMode {
    NORMAL,
    SCREEN,
    OVERLAY,
    SOFT_LIGHT,
    MULTIPLY;

    public static BlendMode from(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }

        return switch (value.trim().toLowerCase()) {
            case "screen" -> SCREEN;
            case "overlay" -> OVERLAY;
            case "soft_light" -> SOFT_LIGHT;
            case "multiply" -> MULTIPLY;
            default -> NORMAL;
        };
    }
}