package com.chaerok.backend.notification.sender;

public record PushSendResult(
        PushSendResultType type,
        String errorCode,
        String errorMessage
) {

    public PushSendResult {
        if (type == null) {
            throw new IllegalArgumentException(
                    "푸시 전송 결과 유형은 필수입니다."
            );
        }
    }

    public static PushSendResult sent() {
        return new PushSendResult(
                PushSendResultType.SENT,
                null,
                null
        );
    }

    public static PushSendResult invalidToken(
            String errorCode,
            String errorMessage
    ) {
        return new PushSendResult(
                PushSendResultType.TOKEN_INVALID,
                errorCode,
                errorMessage
        );
    }

    public static PushSendResult retryable(
            String errorCode,
            String errorMessage
    ) {
        return new PushSendResult(
                PushSendResultType.RETRYABLE_FAILURE,
                errorCode,
                errorMessage
        );
    }

    public static PushSendResult permanent(
            String errorCode,
            String errorMessage
    ) {
        return new PushSendResult(
                PushSendResultType.PERMANENT_FAILURE,
                errorCode,
                errorMessage
        );
    }
}