package com.chaerok.backend.auth.oauth.verifier;

import com.chaerok.backend.auth.exception.AuthErrorCode;
import com.chaerok.backend.auth.oauth.dto.OAuthUserInfo;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.user.entity.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleTokenVerifierTest {

    @Mock
    private JwtDecoder jwtDecoder;

    private GoogleTokenVerifier googleTokenVerifier;

    @BeforeEach
    void setUp() {
        googleTokenVerifier =
                new GoogleTokenVerifier(jwtDecoder);
    }

    @Test
    @DisplayName("Google provider를 반환한다")
    void getProviderReturnsGoogle() {
        assertThat(googleTokenVerifier.getProvider())
                .isEqualTo(OAuthProvider.GOOGLE);
    }

    @Test
    @DisplayName("유효한 Google ID Token이면 사용자 정보를 반환한다")
    void verifyReturnsOAuthUserInfo() {
        String idToken = "google-id-token";

        Jwt jwt = new Jwt(
                idToken,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "google-user-id",
                        "name", "채록 사용자",
                        "email", "chaerok@example.com"
                )
        );

        when(jwtDecoder.decode(idToken))
                .thenReturn(jwt);

        OAuthUserInfo result =
                googleTokenVerifier.verify(idToken, null);

        assertThat(result.provider())
                .isEqualTo(OAuthProvider.GOOGLE);
        assertThat(result.providerUserId())
                .isEqualTo("google-user-id");
        assertThat(result.nickname())
                .isEqualTo("채록 사용자");
        assertThat(result.email())
                .isEqualTo("chaerok@example.com");
    }

    @Test
    @DisplayName("Google ID Token 검증에 실패하면 INVALID_GOOGLE_ID_TOKEN 예외가 발생한다")
    void verifyRejectsInvalidGoogleIdToken() {
        String idToken = "invalid-google-id-token";

        when(jwtDecoder.decode(idToken))
                .thenThrow(new JwtException("invalid token"));

        assertThatThrownBy(() ->
                googleTokenVerifier.verify(idToken, null)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                AuthErrorCode.INVALID_GOOGLE_ID_TOKEN
                        )
        );
    }
}