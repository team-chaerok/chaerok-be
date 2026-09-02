package com.chaerok.backend.course.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    COURSE_PLACE_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "COURSE_001",
            "코스 장소는 1개 이상 선택해야 합니다."
    ),

    COURSE_PLACE_LIMIT_EXCEEDED(
            HttpStatus.CONFLICT,
            "COURSE_002",
            "코스 장소는 최대 3개까지 선택할 수 있습니다."
    ),

    EXTERNAL_PLACE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COURSE_003",
            "외부 장소 정보를 찾을 수 없습니다."
    ),

    PLACE_REGION_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "COURSE_004",
            "선택한 장소가 코스 지역과 일치하지 않습니다."
    ),

    EXTERNAL_PLACE_ID_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "COURSE_005",
            "외부 후보 장소 저장 시 externalPlaceId는 필수입니다."
    ),

    INVALID_EXTERNAL_PLACE_DATA(
            HttpStatus.BAD_REQUEST,
            "COURSE_006",
            "외부 장소 정보가 올바르지 않습니다."
    ),

    UNSUPPORTED_PLACE_CATEGORY(
            HttpStatus.BAD_REQUEST,
            "COURSE_007",
            "지원하지 않는 장소 유형입니다."
    ),

    PLACE_CATEGORY_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "COURSE_008",
            "장소 유형과 세부 유형이 일치하지 않습니다."
    ),

    DUPLICATE_PLACE(
            HttpStatus.CONFLICT,
            "COURSE_009",
            "이미 ACTIVE 코스에 저장된 장소입니다."
    ),

    DUPLICATE_PLACE_CATEGORY(
            HttpStatus.CONFLICT,
            "COURSE_010",
            "이미 ACTIVE 코스에 저장된 장소 유형입니다."
    ),

    DUPLICATE_REQUEST_PLACE_CATEGORY(
            HttpStatus.BAD_REQUEST,
            "COURSE_011",
            "한 번의 요청에 동일한 장소 유형을 중복 저장할 수 없습니다."
    ),

    INVALID_ANCHOR_REGION(
            HttpStatus.BAD_REQUEST,
            "COURSE_012",
            "해당 지역의 Anchor 장소가 아닙니다."
    ),

    NON_REPRESENTATIVE_ANCHOR(
            HttpStatus.BAD_REQUEST,
            "COURSE_013",
            "대표 장소만 Anchor로 사용할 수 있습니다."
    ),

    ANCHOR_COORDINATE_MISSING(
            HttpStatus.CONFLICT,
            "COURSE_014",
            "Anchor 장소의 좌표 정보가 없습니다."
    ),

    ACTIVE_COURSE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COURSE_015",
            "ACTIVE 코스가 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}