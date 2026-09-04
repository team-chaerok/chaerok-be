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
class KakaoTokenVerifierTest {

    @Mock
    private JwtDecoder jwtDecoder;

    private KakaoTokenVerifier kakaoTokenVerifier;

    @BeforeEach
    void setUp() {
        kakaoTokenVerifier =
                new KakaoTokenVerifier(jwtDecoder);
    }

    @Test
    @DisplayName("Kakao provider를 반환한다")
    void getProviderReturnsKakao() {
        assertThat(kakaoTokenVerifier.getProvider())
                .isEqualTo(OAuthProvider.KAKAO);
    }

    @Test
    @DisplayName("유효한 Kakao ID Token이면 사용자 정보를 반환한다")
    void verifyReturnsOAuthUserInfo() {
        String idToken = "kakao-id-token";

        Jwt jwt = new Jwt(
                idToken,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "kakao-user-id",
                        "nickname", "채록 사용자",
                        "email", "chaerok@example.com"
                )
        );

        when(jwtDecoder.decode(idToken))
                .thenReturn(jwt);

        OAuthUserInfo result =
                kakaoTokenVerifier.verify(idToken, null);

        assertThat(result.provider())
                .isEqualTo(OAuthProvider.KAKAO);
        assertThat(result.providerUserId())
                .isEqualTo("kakao-user-id");
        assertThat(result.nickname())
                .isEqualTo("채록 사용자");
        assertThat(result.email())
                .isEqualTo("chaerok@example.com");
    }

    @Test
    @DisplayName("Kakao ID Token 검증에 실패하면 INVALID_KAKAO_ID_TOKEN 예외가 발생한다")
    void verifyRejectsInvalidKakaoIdToken() {
        String idToken = "invalid-kakao-id-token";

        when(jwtDecoder.decode(idToken))
                .thenThrow(new JwtException("invalid token"));

        assertThatThrownBy(() ->
                kakaoTokenVerifier.verify(idToken, null)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                AuthErrorCode.INVALID_KAKAO_ID_TOKEN
                        )
        );
    }
}