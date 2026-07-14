package com.chaerok.backend.course.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CoursePlaceSaveRequest(
        Long placeId,
        String externalPlaceId,
        String source,

        @NotBlank(message = "장소명은 필수입니다.")
        String title,

        @NotBlank(message = "장소 유형은 필수입니다.")
        String categoryGroup,

        String categoryDetail,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String placeUrl
) {
}