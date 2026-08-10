package com.chaerok.render.storage;

public class ObjectStorageOperationException extends RuntimeException {

    public ObjectStorageOperationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
