package com.chaerok.backend.auth.dto;

import com.chaerok.backend.user.entity.OAuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthLoginRequest(

        @NotNull(message = "OAuth 제공자는 필수입니다.")
        OAuthProvider provider,

        @NotBlank(message = "ID Token은 필수입니다.")
        String idToken
) {
}