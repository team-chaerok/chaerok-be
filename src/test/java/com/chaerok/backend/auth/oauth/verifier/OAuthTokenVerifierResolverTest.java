package com.chaerok.backend.auth.oauth.verifier;

import com.chaerok.backend.user.entity.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthTokenVerifierResolverTest {

    @Test
    @DisplayName("등록된 OAuth provider에 해당하는 verifier를 반환한다")
    void resolveReturnsRegisteredVerifier() {
        OAuthTokenVerifier kakaoVerifier =
                mock(OAuthTokenVerifier.class);

        OAuthTokenVerifier googleVerifier =
                mock(OAuthTokenVerifier.class);

        when(kakaoVerifier.getProvider())
                .thenReturn(OAuthProvider.KAKAO);

        when(googleVerifier.getProvider())
                .thenReturn(OAuthProvider.GOOGLE);

        OAuthTokenVerifierResolver resolver =
                new OAuthTokenVerifierResolver(
                        List.of(
                                kakaoVerifier,
                                googleVerifier
                        )
                );

        assertThat(resolver.resolve(OAuthProvider.KAKAO))
                .isSameAs(kakaoVerifier);

        assertThat(resolver.resolve(OAuthProvider.GOOGLE))
                .isSameAs(googleVerifier);
    }

    @Test
    @DisplayName("등록되지 않은 OAuth provider이면 예외가 발생한다")
    void resolveThrowsWhenVerifierIsNotRegistered() {
        OAuthTokenVerifier kakaoVerifier =
                mock(OAuthTokenVerifier.class);

        when(kakaoVerifier.getProvider())
                .thenReturn(OAuthProvider.KAKAO);

        OAuthTokenVerifierResolver resolver =
                new OAuthTokenVerifierResolver(
                        List.of(kakaoVerifier)
                );

        assertThatThrownBy(() ->
                resolver.resolve(OAuthProvider.GOOGLE)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAuth 검증기가 등록되지 않았습니다");
    }
}