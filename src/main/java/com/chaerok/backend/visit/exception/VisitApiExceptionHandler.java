package com.chaerok.backend.visit.exception;

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
public class VisitApiExceptionHandler {

    @ExceptionHandler(VisitAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleVisitAlreadyExists(
            VisitAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return conflict(
                "VISIT_ALREADY_EXISTS",
                exception,
                request
        );
    }

    @ExceptionHandler(FilmRollNotVisitableException.class)
    public ResponseEntity<ErrorResponse> handleFilmRollNotVisitable(
            FilmRollNotVisitableException exception,
            HttpServletRequest request
    ) {
        return conflict(
                "VISIT_NOT_ALLOWED",
                exception,
                request
        );
    }

    @ExceptionHandler(PlaceRegionMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePlaceRegionMismatch(
            PlaceRegionMismatchException exception,
            HttpServletRequest request
    ) {
        return conflict(
                "PLACE_REGION_MISMATCH",
                exception,
                request
        );
    }

    @ExceptionHandler(VisitPhotoNotReadyException.class)
    public ResponseEntity<ErrorResponse> handleVisitPhotoNotReady(
            VisitPhotoNotReadyException exception,
            HttpServletRequest request
    ) {
        return conflict(
                "VISIT_PHOTO_NOT_READY",
                exception,
                request
        );
    }

    @ExceptionHandler(VisitPhotoAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleVisitPhotoAlreadyUsed(
            VisitPhotoAlreadyUsedException exception,
            HttpServletRequest request
    ) {
        return conflict(
                "VISIT_PHOTO_ALREADY_USED",
                exception,
                request
        );
    }

    @ExceptionHandler(VisitRequirementNotMetException.class)
    public ResponseEntity<ErrorResponse> handleVisitRequirementNotMet(
            VisitRequirementNotMetException exception,
            HttpServletRequest request
    ) {
        return conflict(
                "VISIT_REQUIREMENT_NOT_MET",
                exception,
                request
        );
    }

    private ResponseEntity<ErrorResponse> conflict(
            String code,
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        code,
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }
}