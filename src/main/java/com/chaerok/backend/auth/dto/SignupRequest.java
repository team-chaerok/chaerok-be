package com.chaerok.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "회원가입 토큰은 필수입니다.")
        String signupToken,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(
                max = 30,
                message = "닉네임은 30자 이하여야 합니다."
        )
        String nickname
) {
}