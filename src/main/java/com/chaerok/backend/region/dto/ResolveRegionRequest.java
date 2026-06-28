package com.chaerok.backend.region.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveRegionRequest(

        @NotBlank(message = "시·도명은 필수입니다.")
        String provinceName,

        @NotBlank(message = "시·군명은 필수입니다.")
        String cityCountyName

) {
}