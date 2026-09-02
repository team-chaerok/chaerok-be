package com.chaerok.backend.filmroll.exception;

import com.chaerok.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FilmRollErrorCode implements ErrorCode {

    FILM_ROLL_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "FILM_ROLL_NOT_FOUND",
            "필름 롤을 찾을 수 없습니다."
    ),

    PHOTO_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PHOTO_NOT_FOUND",
            "사진 정보를 찾을 수 없습니다."
    ),

    ACTIVE_FILM_ROLL_EXISTS(
            HttpStatus.CONFLICT,
            "ACTIVE_FILM_ROLL_EXISTS",
            "이미 진행 중인 필름 롤이 있습니다."
    ),

    FILM_ROLL_EXIT_REQUIRED(
            HttpStatus.CONFLICT,
            "FILM_ROLL_EXIT_REQUIRED",
            "지역 이탈이 확정된 뒤에 현상할 수 있습니다."
    ),

    DEVELOPMENT_WAIT_NOT_FINISHED(
            HttpStatus.CONFLICT,
            "DEVELOPMENT_WAIT_NOT_FINISHED",
            "지역 이탈 후 1시간이 지나야 현상할 수 있습니다."
    ),

    FILM_ROLL_NOT_CAPTURING_FOR_READY(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "촬영 중인 필름 롤만 현상 준비 상태로 전환할 수 있습니다."
    ),

    FILM_ROLL_ALREADY_COMPLETED(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "이미 현상이 완료된 필름 롤입니다."
    ),

    FILM_ROLL_EXPIRED(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "현상 결과가 만료된 필름 롤은 다시 현상할 수 없습니다."
    ),

    FILM_ROLL_HAS_NO_PHOTOS(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "사진이 없는 필름 롤은 현상할 수 없습니다."
    ),

    FILM_ROLL_INVALID_PHOTO_STATUS(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "현재 사진 상태에는 현상을 시작할 수 없습니다."
    ),

    FILM_ROLL_HAS_NO_UPLOADED_PHOTOS(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "업로드가 완료된 사진이 없어 현상할 수 없습니다."
    ),

    FILM_ROLL_PHOTO_COUNT_MISMATCH(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "필름 롤 사진 수와 업로드 완료 사진 수가 일치하지 않습니다."
    ),

    FILM_ROLL_NOT_CAPTURING_FOR_EXIT(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "촬영 중인 필름 롤만 지역 이탈을 확정할 수 있습니다."
    ),

    RESULT_EXPIRATION_MISSING(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "완료된 필름 롤의 만료 시각이 없습니다."
    ),

    COMPLETED_RENDER_JOB_NOT_FOUND(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "완료된 필름 롤의 현상 작업을 찾을 수 없습니다."
    ),

    INVALID_RESULT_FILE_SIZE(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "현상 결과 파일 크기가 올바르지 않습니다."
    ),

    COMPLETED_PHOTO_COUNT_MISMATCH(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "완료된 사진 수가 필름 롤의 사진 수와 다릅니다."
    ),

    INCOMPLETE_PHOTO_IN_RESULT(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "완료되지 않은 사진이 현상 결과에 포함되어 있습니다."
    ),

    RENDER_JOB_NOT_COMPLETED(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "최신 현상 작업이 완료 상태가 아닙니다."
    ),

    RESULT_PATH_MISMATCH(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "필름 롤과 현상 작업의 결과 경로가 일치하지 않습니다."
    ),

    ZIP_RESULT_PATH_MISSING(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "ZIP 결과 경로가 없습니다."
    ),

    REEL_RESULT_PATH_MISSING(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "릴스 결과 경로가 없습니다."
    ),

    RENDER_FAILURE_CODE_MISSING(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "현상 실패 코드가 없습니다."
    ),

    RENDER_FAILURE_MESSAGE_MISSING(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "현상 실패 메시지가 없습니다."
    ),

    FILTERED_PHOTO_RESULT_PATH_MISSING(
            HttpStatus.CONFLICT,
            "FILM_ROLL_CONFLICT",
            "필터 사진 결과 경로가 없습니다."
    ),

    INVALID_REGION_FILTER(
            HttpStatus.BAD_REQUEST,
            "FILM_ROLL_INVALID_FILTER",
            "선택한 지역에서 사용할 수 없는 필터입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}