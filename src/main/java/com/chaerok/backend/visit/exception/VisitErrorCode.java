package com.chaerok.backend.visit.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VisitErrorCode implements ErrorCode {

    VISIT_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "VISIT_ALREADY_EXISTS",
            "이미 방문 인증한 장소입니다."
    ),

    FILM_ROLL_NOT_VISITABLE(
            HttpStatus.CONFLICT,
            "VISIT_NOT_ALLOWED",
            "촬영 중인 필름 롤에서만 방문을 인증할 수 있습니다."
    ),

    PLACE_REGION_MISMATCH(
            HttpStatus.CONFLICT,
            "PLACE_REGION_MISMATCH",
            "필름 롤 지역에 속하지 않은 장소는 방문 인증할 수 없습니다."
    ),

    VISIT_REQUIREMENT_NOT_MET(
            HttpStatus.CONFLICT,
            "VISIT_REQUIREMENT_NOT_MET",
            "관광지, 식당, 카페를 각각 1곳 이상 방문해야 현상할 수 있습니다."
    ),

    VISIT_PHOTO_NOT_READY(
            HttpStatus.CONFLICT,
            "VISIT_PHOTO_NOT_READY",
            "업로드가 완료된 필름 롤 사진만 방문 인증에 사용할 수 있습니다."
    ),
    VISIT_PHOTO_ALREADY_USED(
            HttpStatus.CONFLICT,
            "VISIT_PHOTO_ALREADY_USED",
            "이미 다른 방문 인증에 사용한 사진입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}