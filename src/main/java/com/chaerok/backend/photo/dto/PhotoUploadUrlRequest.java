package com.chaerok.backend.photo.dto;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filter.analysis.SceneType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record PhotoUploadUrlRequest(

        @Min(value = 1, message = "사진 순서는 1 이상이어야 합니다.")
        @Max(
                value = FilmRoll.MAX_PHOTO_COUNT,
                message = "사진 순서는 24 이하여야 합니다."
        )
        int sequence,

        @NotBlank(message = "Content-Type은 필수입니다.")
        @Pattern(
                regexp = "(?i)^image/(jpeg|jpg)$",
                message = "현재는 JPEG 이미지만 업로드할 수 있습니다."
        )
        String contentType,

        @Positive(message = "파일 크기는 1바이트 이상이어야 합니다.")
        @Max(
                value = 5L * 1024 * 1024,
                message = "이미지 파일은 최대 5MB까지 업로드할 수 있습니다."
        )
        long contentLength,

        boolean hasFace,

        SceneType sceneType,

        @NotNull(message = "촬영 시각은 필수입니다.")
        LocalDateTime takenAt
) {
}
