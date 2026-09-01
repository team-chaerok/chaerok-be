package com.chaerok.backend.auth.oauth.verifier;

import com.chaerok.backend.global.exception.InvalidTokenException;
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
        )
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("유효하지 않은 Apple ID Token입니다.");
    }

    @Test
    void Apple_로그인_nonce가_없으면_예외가_발생한다() {
        // given
        String idToken = "dummy-token";

        // when & then
        assertThatThrownBy(() -> appleTokenVerifier.verify(idToken, null))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Apple 로그인 nonce가 필요합니다.");
    }

    @Test
    void Apple_로그인_nonce가_빈_문자열이면_예외가_발생한다() {
        // given
        String idToken = "dummy-token";

        // when & then
        assertThatThrownBy(() -> appleTokenVerifier.verify(idToken, " "))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Apple 로그인 nonce가 필요합니다.");
    }
}