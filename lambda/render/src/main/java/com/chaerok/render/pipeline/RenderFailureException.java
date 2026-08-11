package com.chaerok.render.pipeline;

public final class RenderFailureException
        extends RenderPipelineException {

    private final String errorCode;
    private final boolean retryable;

    public RenderFailureException(
            String errorCode,
            boolean retryable,
            String message
    ) {
        super(message);
        this.errorCode = requireErrorCode(errorCode);
        this.retryable = retryable;
    }

    public RenderFailureException(
            String errorCode,
            boolean retryable,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorCode = requireErrorCode(errorCode);
        this.retryable = retryable;
    }

    public String errorCode() {
        return errorCode;
    }

    public boolean retryable() {
        return retryable;
    }

    private static String requireErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException(
                    "errorCode is required."
            );
        }
        return errorCode;
    }
}
