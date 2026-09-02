package com.chaerok.backend.region.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RegionErrorCode implements ErrorCode {

    REGION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "REGION_001",
            "지역을 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}