package com.chaerok.backend.auth.oauth.dto;

import com.chaerok.backend.user.entity.OAuthProvider;

public record OAuthUserInfo(
        OAuthProvider provider,
        String providerUserId,
        String nickname,
        String email
) {
}