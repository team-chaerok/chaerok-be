package com.chaerok.backend.auth.dto;

public record OAuthLoginResponse(
        boolean registered,
        TokenResponse tokens,
        String signupToken
) {

    public static OAuthLoginResponse existingUser(
            String accessToken,
            String refreshToken
    ) {
        return new OAuthLoginResponse(
                true,
                new TokenResponse(accessToken, refreshToken),
                null
        );
    }

    public static OAuthLoginResponse newUser(String signupToken) {
        return new OAuthLoginResponse(
                false,
                null,
                signupToken
        );
    }
}