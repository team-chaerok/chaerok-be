package com.chaerok.backend.heritage.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HeritageErrorCode implements ErrorCode {

    NOT_TOUR_API_PLACE(
            HttpStatus.BAD_REQUEST,
            "HERITAGE_001",
            "TourAPI 장소가 아닙니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}