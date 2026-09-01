package com.chaerok.backend.user.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    ALREADY_REGISTERED(
            HttpStatus.CONFLICT,
            "USER_001",
            "이미 가입된 사용자입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}