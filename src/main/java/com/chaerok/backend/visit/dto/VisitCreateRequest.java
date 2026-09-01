package com.chaerok.backend.visit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VisitCreateRequest(
        @NotNull(message = "장소 ID는 필수입니다.")
        @Positive(message = "장소 ID는 양수여야 합니다.")
        Long placeId,

        @NotNull(message = "방문 인증 사진 ID는 필수입니다.")
        @Positive(message = "방문 인증 사진 ID는 양수여야 합니다.")
        Long photoId
) {
}