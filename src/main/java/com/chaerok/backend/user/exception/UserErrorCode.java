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
    ),

    APPLE_AUTHORIZATION_CODE_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "USER_002",
            "Apple 회원탈퇴에는 authorizationCode가 필요합니다."
    ),

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER_003",
            "사용자를 찾을 수 없습니다."
    ),

    REVIEW_MODE_CONFIGURATION_INVALID(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "USER_004",
            "심사용 모드 설정을 불러올 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}