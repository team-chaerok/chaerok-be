package com.chaerok.backend.render.exception;

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
public class RenderApiExceptionHandler {

    @ExceptionHandler(RenderQueueException.class)
    public ResponseEntity<ErrorResponse> handleRenderQueue(
            RenderQueueException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(
                        "RENDER_QUEUE_UNAVAILABLE",
                        "현상 요청을 대기열에 등록하지 못했습니다. 잠시 후 다시 시도해 주세요.",
                        request.getRequestURI()
                ));
    }
}
