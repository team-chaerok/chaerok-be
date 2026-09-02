package com.chaerok.backend.photo.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PhotoErrorCode implements ErrorCode {

    PHOTO_ADD_AFTER_EXIT_NOT_ALLOWED(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "지역 이탈 확정 후에는 새 사진을 추가할 수 없습니다."
    ),

    PHOTO_NOT_UPLOADING(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "업로드 대기 중인 사진만 업로드 완료 처리할 수 있습니다."
    ),

    PHOTO_LIMIT_EXCEEDED(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "필름 롤에는 최대 24장까지만 업로드할 수 있습니다."
    ),

    PHOTO_SEQUENCE_ALREADY_IN_USE(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "이미 사용 중인 사진 순서입니다."
    ),

    PHOTO_UPLOAD_NOT_ALLOWED(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "촬영 중인 필름 롤에서만 사진을 업로드할 수 있습니다."
    ),

    UPLOADED_PHOTO_NOT_FOUND(
            HttpStatus.BAD_REQUEST,
            "INVALID_PHOTO_UPLOAD",
            "S3에서 업로드된 사진을 찾을 수 없습니다."
    ),

    EMPTY_PHOTO_FILE(
            HttpStatus.BAD_REQUEST,
            "INVALID_PHOTO_UPLOAD",
            "업로드된 사진 파일이 비어 있습니다."
    ),

    PHOTO_FILE_TOO_LARGE(
            HttpStatus.BAD_REQUEST,
            "INVALID_PHOTO_UPLOAD",
            "업로드된 사진이 허용된 최대 크기를 초과했습니다."
    ),

    INVALID_PHOTO_CONTENT_TYPE(
            HttpStatus.BAD_REQUEST,
            "INVALID_PHOTO_UPLOAD",
            "업로드된 파일은 JPEG 이미지가 아닙니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}