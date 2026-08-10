package com.chaerok.render.retry;

import java.util.Map;

public record RenderRetryConfig(int maxReceiveCount) {

    public static final String ENV_MAX_RECEIVE_COUNT =
            "RENDER_REQUEST_MAX_RECEIVE_COUNT";
    public static final int DEFAULT_MAX_RECEIVE_COUNT = 3;

    public RenderRetryConfig {
        if (maxReceiveCount < 1 || maxReceiveCount > 1000) {
            throw new IllegalStateException(
                    ENV_MAX_RECEIVE_COUNT
                            + " must be between 1 and 1000."
            );
        }
    }

    public static RenderRetryConfig fromEnvironment(
            Map<String, String> environment
    ) {
        if (environment == null) {
            throw new IllegalStateException(
                    "Lambda environment is required."
            );
        }

        String raw = environment.get(ENV_MAX_RECEIVE_COUNT);
        if (raw == null || raw.isBlank()) {
            return new RenderRetryConfig(
                    DEFAULT_MAX_RECEIVE_COUNT
            );
        }

        try {
            return new RenderRetryConfig(
                    Integer.parseInt(raw.trim())
            );
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    ENV_MAX_RECEIVE_COUNT
                            + " must be an integer.",
                    exception
            );
        }
    }

    public boolean isFinalAttempt(int attempt) {
        return Math.max(attempt, 1) >= maxReceiveCount;
    }
}
