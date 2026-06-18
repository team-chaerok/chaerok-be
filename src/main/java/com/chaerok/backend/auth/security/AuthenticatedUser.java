package com.chaerok.backend.auth.security;

import com.chaerok.backend.user.entity.UserRole;

public record AuthenticatedUser(
        Long userId,
        UserRole role
) {
}