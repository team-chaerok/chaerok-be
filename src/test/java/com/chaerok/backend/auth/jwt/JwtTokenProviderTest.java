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
        assertThat(jwt.getClaimAsString("iss"))
                .isEqualTo("chaerok");
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(jwt.getExpiresAt())
                .isAfter(jwt.getIssuedAt());

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
        assertThat(jwtTokenProvider.isSignupToken(accessToken))
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
        assertThat(jwtTokenProvider.getRefreshTokenUserId(refreshToken))
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
    void 액세스_토큰은_리프레시_토큰으로_사용할_수_없다() {
        // given
        String accessToken = jwtTokenProvider.createAccessToken(
                1L,
                UserRole.USER
        );

        // when & then
        assertThatThrownBy(
                () -> jwtTokenProvider.getRefreshTokenUserId(accessToken)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                AuthErrorCode.INVALID_REFRESH_TOKEN_TYPE
                        )
        );
    }

    @Test
    void 유효하지_않은_리프레시_토큰이면_예외가_발생한다() {
        // given
        String invalidToken = "invalid-refresh-token";

        // when & then
        assertThatThrownBy(
                () -> jwtTokenProvider.getRefreshTokenUserId(invalidToken)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                AuthErrorCode.INVALID_OR_EXPIRED_REFRESH_TOKEN
                        )
        );
    }

    @Test
    void 유효하지_않은_회원가입_토큰이면_예외가_발생한다() {
        // given
        String invalidToken = "invalid-signup-token";

        // when & then
        assertThatThrownBy(
                () -> jwtTokenProvider.getSignupTokenInfo(invalidToken)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                AuthErrorCode.INVALID_OR_EXPIRED_SIGNUP_TOKEN
                        )
        );
    }

    @Test
    void 토큰의_만료_시간을_조회한다() {
        // given
        String accessToken = jwtTokenProvider.createAccessToken(
                1L,
                UserRole.USER
        );

        Jwt jwt = jwtTokenProvider.parseToken(accessToken);

        // when
        var expiration =
                jwtTokenProvider.getExpiration(accessToken);

        // then
        assertThat(expiration).isEqualTo(
                java.time.LocalDateTime.ofInstant(
                        jwt.getExpiresAt(),
                        java.time.ZoneId.systemDefault()
                )
        );
    }

    @Test
    void 유효하지_않은_토큰의_만료_시간을_조회하면_예외가_발생한다() {
        // given
        String invalidToken = "invalid-token";

        // when & then
        assertThatThrownBy(
                () -> jwtTokenProvider.getExpiration(invalidToken)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_TOKEN)
        );
    }

    @Test
    void 이메일이_없는_회원가입_토큰에서_사용자_정보를_조회한다() {
        // given
        String signupToken = jwtTokenProvider.createSignupToken(
                OAuthProvider.APPLE,
                "apple-user-1",
                "채록 사용자",
                null
        );

        // when
        SignupTokenInfo tokenInfo =
                jwtTokenProvider.getSignupTokenInfo(signupToken);

        // then
        assertThat(tokenInfo.provider())
                .isEqualTo(OAuthProvider.APPLE);
        assertThat(tokenInfo.providerUserId())
                .isEqualTo("apple-user-1");
        assertThat(tokenInfo.email()).isNull();
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
                        .isEqualTo(
                                AuthErrorCode.INVALID_SIGNUP_TOKEN_TYPE
                        )
        );
    }
}