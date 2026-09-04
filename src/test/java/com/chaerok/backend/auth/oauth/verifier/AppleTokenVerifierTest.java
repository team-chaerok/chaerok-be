package com.chaerok.backend.auth.oauth.verifier;

import com.chaerok.backend.auth.exception.AuthErrorCode;
import com.chaerok.backend.auth.oauth.dto.OAuthUserInfo;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.user.entity.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class AppleTokenVerifierTest {

    private AppleTokenVerifier appleTokenVerifier;

    @BeforeEach
    void setUp() {
        appleTokenVerifier = spy(new AppleTokenVerifier(
                "com.teamchaerok.chaerok",
                "https://appleid.apple.com",
                "https://appleid.apple.com/auth/keys"
        ));
    }

    @Test
    void Apple_Provider를_반환한다() {
        // when
        OAuthProvider provider = appleTokenVerifier.getProvider();

        // then
        assertThat(provider).isEqualTo(OAuthProvider.APPLE);
    }

    @Test
    void 유효한_Apple_ID_Token이면_사용자_정보를_반환한다() {
        // given
        String idToken = "apple-id-token";
        String nonce = "test-nonce";

        Jwt jwt = new Jwt(
                idToken,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "apple-user-id",
                        "email", "user@example.com",
                        "nonce", nonce
                )
        );

        doReturn(jwt)
                .when(appleTokenVerifier)
                .decodeAndValidate(idToken);

        // when
        OAuthUserInfo result =
                appleTokenVerifier.verify(idToken, nonce);

        // then
        assertThat(result.provider())
                .isEqualTo(OAuthProvider.APPLE);
        assertThat(result.providerUserId())
                .isEqualTo("apple-user-id");
        assertThat(result.nickname())
                .isNull();
        assertThat(result.email())
                .isEqualTo("user@example.com");
    }

    @Test
    void 유효하지_않은_Apple_ID_Token이면_예외가_발생한다() {
        // given
        String invalidIdToken = "invalid-token";

        // when & then
        assertThatThrownBy(
                () -> appleTokenVerifier.verify(
                        invalidIdToken,
                        "test-nonce"
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                AuthErrorCode.INVALID_APPLE_ID_TOKEN
                        )
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
                        .isEqualTo(
                                AuthErrorCode.APPLE_NONCE_REQUIRED
                        )
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
                        .isEqualTo(
                                AuthErrorCode.APPLE_NONCE_REQUIRED
                        )
        );
    }

    @Test
    void Apple_ID_Token의_nonce가_요청_nonce와_다르면_예외가_발생한다() {
        // given
        String idToken = "apple-id-token";

        Jwt jwt = new Jwt(
                idToken,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "apple-user-id",
                        "email", "user@example.com",
                        "nonce", "token-nonce"
                )
        );

        doReturn(jwt)
                .when(appleTokenVerifier)
                .decodeAndValidate(idToken);

        // when & then
        assertThatThrownBy(
                () -> appleTokenVerifier.verify(
                        idToken,
                        "request-nonce"
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                AuthErrorCode.APPLE_NONCE_MISMATCH
                        )
        );
    }

    @Test
    void Apple_ID_Token에_nonce가_없으면_예외가_발생한다() {
        // given
        String idToken = "apple-id-token";

        Jwt jwt = new Jwt(
                idToken,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "apple-user-id",
                        "email", "user@example.com"
                )
        );

        doReturn(jwt)
                .when(appleTokenVerifier)
                .decodeAndValidate(idToken);

        // when & then
        assertThatThrownBy(
                () -> appleTokenVerifier.verify(
                        idToken,
                        "request-nonce"
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                AuthErrorCode.APPLE_NONCE_MISMATCH
                        )
        );
    }
}