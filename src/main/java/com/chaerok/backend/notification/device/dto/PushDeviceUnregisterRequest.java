package com.chaerok.backend.notification.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushDeviceUnregisterRequest(
        @NotBlank(message = "FCM 등록 토큰은 필수입니다.")
        @Size(
                max = 4096,
                message = "FCM 등록 토큰은 4096자를 초과할 수 없습니다."
        )
        String token
) {
}