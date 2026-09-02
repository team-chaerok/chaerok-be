package com.chaerok.backend.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        request = mock(HttpServletRequest.class);
        when(request.getRequestURI())
                .thenReturn("/api/test");
        when(request.getMethod())
                .thenReturn("GET");
    }

    @Test
    @DisplayName("BusinessException은 ErrorCode의 상태와 응답 정보를 사용한다")
    void handlesBusinessException() {
        BusinessException exception =
                new BusinessException(
                        CommonErrorCode.TYPE_MISMATCH
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(
                        exception,
                        request
                );

        assertThat(response.getStatusCode().value())
                .isEqualTo(400);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("COMMON_003");
        assertThat(response.getBody().message())
                .isEqualTo("요청값의 형식이 올바르지 않습니다.");
        assertThat(response.getBody().path())
                .isEqualTo("/api/test");
        assertThat(response.getBody().errors())
                .isEmpty();
    }

    @Test
    @DisplayName("RequestBody 검증 실패는 COMMON_001로 처리한다")
    void handlesMethodArgumentNotValid() {
        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);

        BindingResult bindingResult =
                mock(BindingResult.class);

        when(exception.getBindingResult())
                .thenReturn(bindingResult);

        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(
                        new FieldError(
                                "request",
                                "keyword",
                                "검색어는 필수입니다."
                        )
                ));

        ResponseEntity<ErrorResponse> response =
                handler.handleMethodArgumentNotValid(
                        exception,
                        request
                );

        assertThat(response.getStatusCode().value())
                .isEqualTo(400);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("COMMON_001");
        assertThat(response.getBody().message())
                .isEqualTo("요청값이 올바르지 않습니다.");
        assertThat(response.getBody().errors())
                .containsExactly(
                        new ErrorResponse.FieldErrorDetail(
                                "keyword",
                                "검색어는 필수입니다."
                        )
                );
    }

    @Test
    @DisplayName("파라미터 제약조건 검증 실패는 COMMON_001로 처리한다")
    void handlesConstraintViolation() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation =
                mock(ConstraintViolation.class);

        Path propertyPath = mock(Path.class);

        when(violation.getPropertyPath())
                .thenReturn(propertyPath);
        when(propertyPath.toString())
                .thenReturn("searchPlaces.keyword");
        when(violation.getMessage())
                .thenReturn("검색어는 필수입니다.");

        ConstraintViolationException exception =
                new ConstraintViolationException(
                        Set.of(violation)
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleConstraintViolation(
                        exception,
                        request
                );

        assertThat(response.getStatusCode().value())
                .isEqualTo(400);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("COMMON_001");
        assertThat(response.getBody().errors())
                .containsExactly(
                        new ErrorResponse.FieldErrorDetail(
                                "keyword",
                                "검색어는 필수입니다."
                        )
                );
    }

    @Test
    @DisplayName("필수 요청 파라미터 누락은 COMMON_002로 처리한다")
    void handlesMissingRequestParameter() {
        MissingServletRequestParameterException exception =
                mock(MissingServletRequestParameterException.class);

        ResponseEntity<ErrorResponse> response =
                handler.handleMissingRequestParameter(
                        exception,
                        request
                );

        assertThat(response.getStatusCode().value())
                .isEqualTo(400);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("COMMON_002");
        assertThat(response.getBody().message())
                .isEqualTo("필수 요청값이 누락되었습니다.");
    }

    @Test
    @DisplayName("요청 파라미터 타입 변환 실패는 COMMON_003으로 처리한다")
    void handlesTypeMismatch() {
        MethodArgumentTypeMismatchException exception =
                mock(MethodArgumentTypeMismatchException.class);

        ResponseEntity<ErrorResponse> response =
                handler.handleTypeMismatch(
                        exception,
                        request
                );

        assertThat(response.getStatusCode().value())
                .isEqualTo(400);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("COMMON_003");
        assertThat(response.getBody().message())
                .isEqualTo("요청값의 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("읽을 수 없는 RequestBody는 COMMON_001로 처리한다")
    void handlesMessageNotReadable() {
        HttpMessageNotReadableException exception =
                mock(HttpMessageNotReadableException.class);

        ResponseEntity<ErrorResponse> response =
                handler.handleMessageNotReadable(
                        exception,
                        request
                );

        assertThat(response.getStatusCode().value())
                .isEqualTo(400);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("COMMON_001");
        assertThat(response.getBody().message())
                .isEqualTo("요청값이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("처리되지 않은 예외는 COMMON_500으로 처리한다")
    void handlesUnexpectedException() {
        RuntimeException exception =
                new RuntimeException("unexpected error");

        ResponseEntity<ErrorResponse> response =
                handler.handleException(
                        exception,
                        request
                );

        assertThat(response.getStatusCode().value())
                .isEqualTo(500);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("COMMON_500");
        assertThat(response.getBody().message())
                .isEqualTo("서버 내부 오류가 발생했습니다.");
        assertThat(response.getBody().path())
                .isEqualTo("/api/test");
    }
}