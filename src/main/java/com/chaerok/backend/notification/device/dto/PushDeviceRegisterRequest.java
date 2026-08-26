package com.chaerok.backend.notification.device.dto;

import com.chaerok.backend.notification.device.entity.PushPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PushDeviceRegisterRequest(
        @NotBlank(message = "FCM 등록 토큰은 필수입니다.")
        @Size(
                max = 4096,
                message = "FCM 등록 토큰은 4096자를 초과할 수 없습니다."
        )
        String token,

        @NotNull(message = "푸시 플랫폼은 필수입니다.")
        PushPlatform platform
) {
}