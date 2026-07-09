package com.chaerok.backend.global.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        String path,
        LocalDateTime timestamp,
        List<FieldErrorDetail> errors
) {

    public static ErrorResponse of(
            String code,
            String message,
            String path
    ) {
        return new ErrorResponse(
                code,
                message,
                path,
                LocalDateTime.now(),
                List.of()
        );
    }

    public static ErrorResponse of(
            String code,
            String message,
            String path,
            List<FieldErrorDetail> errors
    ) {
        return new ErrorResponse(
                code,
                message,
                path,
                LocalDateTime.now(),
                errors
        );
    }

    public record FieldErrorDetail(
            String field,
            String message
    ) {
    }
}