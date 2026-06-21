package com.chaerok.backend.global.exception;

public record ErrorResponse(
        String code,
        String message
) {
}