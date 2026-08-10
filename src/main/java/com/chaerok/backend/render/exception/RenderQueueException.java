package com.chaerok.backend.render.exception;

public class RenderQueueException extends RuntimeException {

    public RenderQueueException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
