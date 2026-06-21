package com.chaerok.backend.user.dto;

import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.entity.UserRole;

public record UserResponse(
        Long id,
        OAuthProvider provider,
        String nickname,
        String email,
        UserRole role
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getProvider(),
                user.getNickname(),
                user.getEmail(),
                user.getRole()
        );
    }
}