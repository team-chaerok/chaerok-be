package com.chaerok.backend.place.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {

    PLACE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PLACE_001",
            "장소를 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}