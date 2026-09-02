package com.chaerok.backend.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(
            ErrorCode errorCode,
            String logMessage
    ) {
        super(logMessage);
        this.errorCode = errorCode;
    }

    public BusinessException(
            ErrorCode errorCode,
            String logMessage,
            Throwable cause
    ) {
        super(logMessage, cause);
        this.errorCode = errorCode;
    }
}