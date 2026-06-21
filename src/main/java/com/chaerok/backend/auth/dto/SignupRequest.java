package com.chaerok.backend.auth.dto;

import jakarta.validation.constraints.AssertTrue;
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
        String nickname,

        @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
        boolean termsAgreed,

        @AssertTrue(message = "개인정보 수집 및 이용에 동의해야 합니다.")
        boolean privacyAgreed
) {
}