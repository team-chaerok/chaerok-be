package com.chaerok.backend.filmroll.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FilmRollCreateRequest(

        @NotNull(message = "클라이언트 필름 롤 ID는 필수입니다.")
        UUID clientFilmRollId,

        @NotNull(message = "지역 ID는 필수입니다.")
        Long regionId,

        @NotBlank(message = "필터 ID는 필수입니다.")
        String filterId,

        @DecimalMin(
                value = "0.0",
                message = "필터 강도는 0.0 이상이어야 합니다."
        )
        @DecimalMax(
                value = "1.0",
                message = "필터 강도는 1.0 이하여야 합니다."
        )
        double filterStrength
) {
}
