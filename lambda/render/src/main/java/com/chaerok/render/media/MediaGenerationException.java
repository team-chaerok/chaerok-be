package com.chaerok.render.media;

public class MediaGenerationException extends RuntimeException {

    public MediaGenerationException(String message) {
        super(message);
    }

    public MediaGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
