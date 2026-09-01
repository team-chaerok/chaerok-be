package com.chaerok.backend.auth.jwt;

import com.chaerok.backend.auth.exception.AuthErrorCode;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        String secret =
                "chaerok-test-secret-key-must-be-longer-than-32-bytes";

        jwtTokenProvider = new JwtTokenProvider(
                secret,
                1_800_000L,
                1_209_600_000L,
                600_000L
        );
    }

    @Test
    void 액세스_토큰을_생성하고_정보를_조회한다() {
        // given
        Long userId = 1L;
        UserRole role = UserRole.USER;

        // when
        String accessToken =
                jwtTokenProvider.createAccessToken(userId, role);

        Jwt jwt = jwtTokenProvider.parseToken(accessToken);

        // then
        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.getClaimAsString("type"))
                .isEqualTo(TokenType.ACCESS.name());
        assertThat(jwt.getClaimAsString("role"))
                .isEqualTo(UserRole.USER.name());

        assertThat(jwtTokenProvider.getUserId(accessToken))
                .isEqualTo(userId);
        assertThat(jwtTokenProvider.getRole(accessToken))
                .isEqualTo(role);
        assertThat(jwtTokenProvider.isAccessToken(accessToken))
                .isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(accessToken))
                .isFalse();
    }

    @Test
    void 리프레시_토큰을_생성하고_정보를_조회한다() {
        // given
        Long userId = 1L;

        // when
        String refreshToken =
                jwtTokenProvider.createRefreshToken(userId);

        // then
        assertThat(jwtTokenProvider.getUserId(refreshToken))
                .isEqualTo(userId);
        assertThat(jwtTokenProvider.isRefreshToken(refreshToken))
                .isTrue();
        assertThat(jwtTokenProvider.isAccessToken(refreshToken))
                .isFalse();
    }

    @Test
    void 회원가입_토큰에서_사용자_정보를_조회한다() {
        // given
        String signupToken = jwtTokenProvider.createSignupToken(
                OAuthProvider.KAKAO,
                "kakao-user-1",
                "채록 사용자",
                "test@example.com"
        );

        // when
        SignupTokenInfo tokenInfo =
                jwtTokenProvider.getSignupTokenInfo(signupToken);

        // then
        assertThat(tokenInfo.provider())
                .isEqualTo(OAuthProvider.KAKAO);
        assertThat(tokenInfo.providerUserId())
                .isEqualTo("kakao-user-1");
        assertThat(tokenInfo.email())
                .isEqualTo("test@example.com");
        assertThat(jwtTokenProvider.isSignupToken(signupToken))
                .isTrue();
    }

    @Test
    void 액세스_토큰은_회원가입_토큰으로_사용할_수_없다() {
        // given
        String accessToken = jwtTokenProvider.createAccessToken(
                1L,
                UserRole.USER
        );

        // when & then
        assertThatThrownBy(
                () -> jwtTokenProvider.getSignupTokenInfo(accessToken)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_SIGNUP_TOKEN_TYPE)
        );
    }
}