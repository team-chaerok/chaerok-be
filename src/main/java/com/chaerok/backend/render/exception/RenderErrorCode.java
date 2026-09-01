package com.chaerok.backend.render.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RenderErrorCode implements ErrorCode {

    FILM_ROLL_NOT_READY(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "READY 상태의 필름 롤만 현상을 요청할 수 있습니다."
    ),

    ACTIVE_RENDER_JOB_EXISTS(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "이미 진행 중인 현상 작업이 있습니다."
    ),

    RENDER_PHOTO_NOT_FOUND(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "현상할 사진이 없습니다."
    ),

    RENDER_PHOTO_COUNT_MISMATCH(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "필름 롤 사진 수와 저장된 사진 수가 일치하지 않습니다."
    ),

    INCOMPLETE_PHOTO_UPLOAD(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "업로드가 완료되지 않은 사진이 있습니다."
    ),

    INVALID_PHOTO_SEQUENCE(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "사진 순서는 1부터 빠짐없이 연속되어야 합니다."
    ),

    FILM_ROLL_NOT_READY_FOR_QUEUE(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "READY 상태에서만 현상 대기 상태로 전환할 수 있습니다."
    ),

    RENDER_QUEUE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "RENDER_QUEUE_UNAVAILABLE",
            "현상 요청을 대기열에 등록하지 못했습니다. 잠시 후 다시 시도해 주세요."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}