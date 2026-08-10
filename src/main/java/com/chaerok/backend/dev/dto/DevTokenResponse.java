package com.chaerok.backend.dev.dto;

import com.chaerok.backend.user.entity.User;

public record DevTokenResponse(
        Long userId,
        String nickname,
        String role,
        String tokenType,
        String accessToken
) {
    public static DevTokenResponse of(User user, String accessToken) {
        return new DevTokenResponse(
                user.getId(),
                user.getNickname(),
                user.getRole().name(),
                "Bearer",
                accessToken
        );
    }
}
