package com.chaerok.backend.notification.sender;

public enum PushSendResultType {
    SENT,
    TOKEN_INVALID,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE
}