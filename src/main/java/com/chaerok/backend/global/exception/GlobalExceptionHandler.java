package com.chaerok.backend.global.exception;

import com.chaerok.backend.global.aws.ObjectStorageException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.from(
                        errorCode,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ErrorResponse.FieldErrorDetail> errors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> new ErrorResponse.FieldErrorDetail(
                                error.getField(),
                                error.getDefaultMessage()
                        ))
                        .toList();

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        "INVALID_REQUEST",
                        "요청값이 올바르지 않습니다.",
                        request.getRequestURI(),
                        errors
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        "BAD_REQUEST",
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception. method={}, path={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage(),
                exception
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        "INTERNAL_SERVER_ERROR",
                        "서버 내부 오류가 발생했습니다.",
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