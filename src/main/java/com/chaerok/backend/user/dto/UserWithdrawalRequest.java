package com.chaerok.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserWithdrawalRequest(

        @Schema(
                description = "Apple 로그인 사용자의 회원탈퇴 시 재인증으로 발급받은 authorization code"
        )
        String authorizationCode
) {
}