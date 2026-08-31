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
        assertThatThrownBy(() -> appleTokenVerifier.verify(invalidIdToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("유효하지 않은 Apple ID Token입니다.");
    }
}