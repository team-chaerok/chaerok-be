package com.chaerok.backend.auth.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_SIGNUP_TOKEN_TYPE(
            HttpStatus.UNAUTHORIZED,
            "AUTH_001",
            "회원가입용 토큰이 아닙니다."
    ),

    INVALID_OR_EXPIRED_SIGNUP_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AUTH_002",
            "유효하지 않거나 만료된 회원가입 토큰입니다."
    ),

    INVALID_REFRESH_TOKEN_TYPE(
            HttpStatus.UNAUTHORIZED,
            "AUTH_003",
            "Refresh Token이 아닙니다."
    ),

    INVALID_OR_EXPIRED_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AUTH_004",
            "유효하지 않거나 만료된 Refresh Token입니다."
    ),

    TOKEN_EXPIRATION_MISSING(
            HttpStatus.UNAUTHORIZED,
            "AUTH_005",
            "토큰 만료 시간이 존재하지 않습니다."
    ),

    INVALID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AUTH_006",
            "유효하지 않은 토큰입니다."
    ),

    APPLE_NONCE_REQUIRED(
            HttpStatus.UNAUTHORIZED,
            "AUTH_007",
            "Apple 로그인 nonce가 필요합니다."
    ),

    APPLE_NONCE_MISMATCH(
            HttpStatus.UNAUTHORIZED,
            "AUTH_008",
            "Apple ID Token의 nonce가 일치하지 않습니다."
    ),

    INVALID_APPLE_ID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AUTH_009",
            "유효하지 않은 Apple ID Token입니다."
    ),

    INVALID_GOOGLE_ID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AUTH_010",
            "유효하지 않은 Google ID Token입니다."
    ),

    INVALID_KAKAO_ID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AUTH_011",
            "유효하지 않은 카카오 ID Token입니다."
    ),

    REFRESH_TOKEN_USER_MISMATCH(
            HttpStatus.UNAUTHORIZED,
            "AUTH_012",
            "Refresh Token의 사용자 정보가 일치하지 않습니다."
    ),

    REFRESH_TOKEN_NOT_FOUND(
            HttpStatus.UNAUTHORIZED,
            "AUTH_013",
            "저장되지 않았거나 폐기된 Refresh Token입니다."
    ),

    EXPIRED_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AUTH_014",
            "만료된 Refresh Token입니다."
    ),

    APPLE_WITHDRAWAL_USER_MISMATCH(
            HttpStatus.UNAUTHORIZED,
            "AUTH_015",
            "Apple 회원탈퇴 인증 사용자가 일치하지 않습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}