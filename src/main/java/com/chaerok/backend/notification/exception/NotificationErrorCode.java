package com.chaerok.backend.notification.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    INVALID_FCM_TOKEN(
            HttpStatus.BAD_REQUEST,
            "NOTIFICATION_001",
            "FCM 등록 토큰이 올바르지 않습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}