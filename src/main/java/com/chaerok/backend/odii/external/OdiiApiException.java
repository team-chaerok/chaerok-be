package com.chaerok.backend.odii.external;

public class OdiiApiException extends RuntimeException {

    public OdiiApiException(String message) {
        super(message);
    }

    public OdiiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
