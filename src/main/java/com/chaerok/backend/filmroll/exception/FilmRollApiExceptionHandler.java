package com.chaerok.backend.filmroll.exception;

import com.chaerok.backend.global.aws.ObjectStorageException;
import com.chaerok.backend.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class FilmRollApiExceptionHandler {

    @ExceptionHandler(FilmRollNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFilmRollNotFound(
            FilmRollNotFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        "FILM_ROLL_NOT_FOUND",
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(PhotoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePhotoNotFound(
            PhotoNotFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        "PHOTO_NOT_FOUND",
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }


    @ExceptionHandler(ActiveFilmRollExistsException.class)
    public ResponseEntity<ErrorResponse> handleActiveFilmRollExists(
            ActiveFilmRollExistsException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "ACTIVE_FILM_ROLL_EXISTS",
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(FilmRollConflictException.class)
    public ResponseEntity<ErrorResponse> handleFilmRollConflict(
            FilmRollConflictException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "FILM_ROLL_CONFLICT",
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(PhotoUploadValidationException.class)
    public ResponseEntity<ErrorResponse> handlePhotoUploadValidation(
            PhotoUploadValidationException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        "INVALID_PHOTO_UPLOAD",
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ObjectStorageException.class)
    public ResponseEntity<ErrorResponse> handleObjectStorage(
            ObjectStorageException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(
                        "OBJECT_STORAGE_UNAVAILABLE",
                        "파일 저장소 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.",
                        request.getRequestURI()
                ));
    }
}
