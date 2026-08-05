package com.chaerok.backend.render.result;

public class InvalidRenderResultMessageException
        extends RuntimeException {

    public InvalidRenderResultMessageException(String message) {
        super(message);
    }

    public InvalidRenderResultMessageException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
