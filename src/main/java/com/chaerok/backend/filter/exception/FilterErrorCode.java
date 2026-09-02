package com.chaerok.backend.filter.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FilterErrorCode implements ErrorCode {

    IMAGE_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "FILTER_001",
            "이미지 파일은 필수입니다."
    ),

    IMAGE_FILE_TOO_LARGE(
            HttpStatus.BAD_REQUEST,
            "FILTER_002",
            "이미지 파일은 최대 20MB까지 업로드할 수 있습니다."
    ),

    INVALID_IMAGE_CONTENT_TYPE(
            HttpStatus.BAD_REQUEST,
            "FILTER_003",
            "JPG, PNG, WebP 이미지만 업로드할 수 있습니다."
    ),

    INVALID_IMAGE_FORMAT(
            HttpStatus.BAD_REQUEST,
            "FILTER_004",
            "이미지를 읽을 수 없습니다. 지원하지 않는 이미지 형식일 수 있습니다."
    ),

    FILTER_ID_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "FILTER_005",
            "filterId는 필수입니다."
    ),

    INVALID_FILTER_STRENGTH(
            HttpStatus.BAD_REQUEST,
            "FILTER_006",
            "필터 강도는 0.0 이상 1.0 이하의 유효한 숫자여야 합니다."
    ),

    INVALID_IMAGE_SIZE(
            HttpStatus.BAD_REQUEST,
            "FILTER_007",
            "이미지 해상도가 올바르지 않습니다."
    ),

    IMAGE_SIZE_LIMIT_EXCEEDED(
            HttpStatus.BAD_REQUEST,
            "FILTER_008",
            "이미지 해상도는 최대 6000x6000까지 허용됩니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}