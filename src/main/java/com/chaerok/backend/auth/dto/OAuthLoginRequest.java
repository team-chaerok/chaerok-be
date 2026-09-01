package com.chaerok.backend.auth.dto;

import com.chaerok.backend.user.entity.OAuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthLoginRequest(

        @NotNull(message = "OAuth 제공자는 필수입니다.")
        OAuthProvider provider,

        @NotBlank(message = "ID Token은 필수입니다.")
        String idToken,

        @Schema(
                description = "Apple 로그인 요청에 사용한 SHA-256 해시 nonce. Kakao/Google에서는 사용하지 않습니다."
        )
        String nonce
) {
}