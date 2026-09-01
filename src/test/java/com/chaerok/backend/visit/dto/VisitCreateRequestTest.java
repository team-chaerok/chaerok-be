package com.chaerok.backend.visit.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VisitCreateRequestTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    @DisplayName("placeId와 photoId가 모두 있으면 방문 인증 요청이 유효하다")
    void acceptsPlaceAndPhoto() {
        VisitCreateRequest request =
                new VisitCreateRequest(200L, 400L);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("photoId가 없으면 방문 인증 요청을 거부한다")
    void rejectsMissingPhotoId() {
        VisitCreateRequest request =
                new VisitCreateRequest(200L, null);

        Set<ConstraintViolation<VisitCreateRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("방문 인증 사진 ID는 필수입니다.");
    }
}