package com.chaerok.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "COMMON_001",
            "요청값이 올바르지 않습니다."
    ),

    MISSING_REQUEST_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "COMMON_002",
            "필수 요청값이 누락되었습니다."
    ),

    TYPE_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "COMMON_003",
            "요청값의 형식이 올바르지 않습니다."
    ),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMON_500",
            "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}