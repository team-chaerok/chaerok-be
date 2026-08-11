package com.chaerok.backend.photo.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoUploadUrlRequestTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    @DisplayName("10MB JPEG 업로드 요청은 허용한다")
    void allowsTenMegabyteJpeg() {
        PhotoUploadUrlRequest request = new PhotoUploadUrlRequest(
                1,
                "image/jpeg",
                10L * 1024 * 1024,
                LocalDateTime.of(2026, 8, 11, 11, 0)
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("10MB를 초과한 업로드 요청은 거부한다")
    void rejectsUploadLargerThanTenMegabytes() {
        PhotoUploadUrlRequest request = new PhotoUploadUrlRequest(
                1,
                "image/jpeg",
                10L * 1024 * 1024 + 1,
                LocalDateTime.of(2026, 8, 11, 11, 0)
        );

        Set<ConstraintViolation<PhotoUploadUrlRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("이미지 파일은 최대 10MB까지 업로드할 수 있습니다.");
    }
}
