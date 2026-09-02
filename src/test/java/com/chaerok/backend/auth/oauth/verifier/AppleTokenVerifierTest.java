package com.chaerok.backend.auth.oauth.verifier;

import com.chaerok.backend.auth.exception.AuthErrorCode;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.user.entity.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleTokenVerifierTest {

    private AppleTokenVerifier appleTokenVerifier;

    @BeforeEach
    void setUp() {
        appleTokenVerifier = new AppleTokenVerifier(
                "com.teamchaerok.chaerok",
                "https://appleid.apple.com",
                "https://appleid.apple.com/auth/keys"
        );
    }

    @Test
    void Apple_Provider를_반환한다() {
        // when
        OAuthProvider provider = appleTokenVerifier.getProvider();

        // then
        assertThat(provider).isEqualTo(OAuthProvider.APPLE);
    }

    @Test
    void 유효하지_않은_Apple_ID_Token이면_예외가_발생한다() {
        // given
        String invalidIdToken = "invalid-token";

        // when & then
        assertThatThrownBy(
                () -> appleTokenVerifier.verify(invalidIdToken, "test-nonce")
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_APPLE_ID_TOKEN)
        );
    }

    @Test
    void Apple_로그인_nonce가_없으면_예외가_발생한다() {
        // given
        String idToken = "dummy-token";

        // when & then
        assertThatThrownBy(
                () -> appleTokenVerifier.verify(idToken, null)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(AuthErrorCode.APPLE_NONCE_REQUIRED)
        );
    }

    @Test
    void Apple_로그인_nonce가_빈_문자열이면_예외가_발생한다() {
        // given
        String idToken = "dummy-token";

        // when & then
        assertThatThrownBy(
                () -> appleTokenVerifier.verify(idToken, " ")
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(AuthErrorCode.APPLE_NONCE_REQUIRED)
        );
    }
}