package com.chaerok.backend.auth.jwt;

import com.chaerok.backend.user.entity.OAuthProvider;

public record SignupTokenInfo(
        OAuthProvider provider,
        String providerUserId,
        String email
) {
}