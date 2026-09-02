package com.chaerok.backend.global.exception;

import com.chaerok.backend.global.aws.ObjectStorageException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ErrorResponse.FieldErrorDetail> errors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                new ErrorResponse.FieldErrorDetail(
                                        error.getField(),
                                        error.getDefaultMessage()
                                )
                        )
                        .toList();

        ErrorCode errorCode =
                CommonErrorCode.INVALID_REQUEST;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        request.getRequestURI(),
                        errors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ErrorResponse.FieldErrorDetail> errors =
                exception.getConstraintViolations()
                        .stream()
                        .map(violation ->
                                new ErrorResponse.FieldErrorDetail(
                                        extractFieldName(
                                                violation
                                                        .getPropertyPath()
                                                        .toString()
                                        ),
                                        violation.getMessage()
                                )
                        )
                        .toList();

        ErrorCode errorCode =
                CommonErrorCode.INVALID_REQUEST;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        request.getRequestURI(),
                        errors
                ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        List<ErrorResponse.FieldErrorDetail> errors =
                exception.getAllValidationResults()
                        .stream()
                        .flatMap(result ->
                                result.getResolvableErrors()
                                        .stream()
                                        .map(error ->
                                                new ErrorResponse.FieldErrorDetail(
                                                        result.getMethodParameter()
                                                                .getParameterName(),
                                                        error.getDefaultMessage()
                                                )
                                        )
                        )
                        .toList();

        ErrorCode errorCode =
                CommonErrorCode.INVALID_REQUEST;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        request.getRequestURI(),
                        errors
                ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(
                        CommonErrorCode
                                .MISSING_REQUEST_PARAMETER
                                .getStatus()
                )
                .body(ErrorResponse.from(
                        CommonErrorCode.MISSING_REQUEST_PARAMETER,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(
                        CommonErrorCode.TYPE_MISMATCH.getStatus()
                )
                .body(ErrorResponse.from(
                        CommonErrorCode.TYPE_MISMATCH,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(
                        CommonErrorCode.INVALID_REQUEST.getStatus()
                )
                .body(ErrorResponse.from(
                        CommonErrorCode.INVALID_REQUEST,
                        request.getRequestURI()
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
                .status(
                        CommonErrorCode
                                .INTERNAL_SERVER_ERROR
                                .getStatus()
                )
                .body(ErrorResponse.from(
                        CommonErrorCode.INTERNAL_SERVER_ERROR,
                        request.getRequestURI()
                ));
    }

    private String extractFieldName(String propertyPath) {
        int lastDotIndex = propertyPath.lastIndexOf('.');

        return lastDotIndex >= 0
                ? propertyPath.substring(lastDotIndex + 1)
                : propertyPath;
    }
}