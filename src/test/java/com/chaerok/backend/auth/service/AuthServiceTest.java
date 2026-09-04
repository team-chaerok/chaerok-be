package com.chaerok.backend.auth.service;

import com.chaerok.backend.auth.constant.TermsVersion;
import com.chaerok.backend.auth.dto.OAuthLoginRequest;
import com.chaerok.backend.auth.dto.OAuthLoginResponse;
import com.chaerok.backend.auth.dto.RefreshTokenRequest;
import com.chaerok.backend.auth.dto.SignupRequest;
import com.chaerok.backend.auth.dto.TokenResponse;
import com.chaerok.backend.auth.entity.RefreshToken;
import com.chaerok.backend.auth.exception.AuthErrorCode;
import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.auth.jwt.SignupTokenInfo;
import com.chaerok.backend.auth.oauth.dto.OAuthUserInfo;
import com.chaerok.backend.auth.oauth.verifier.OAuthTokenVerifier;
import com.chaerok.backend.auth.oauth.verifier.OAuthTokenVerifierResolver;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.entity.UserRole;
import com.chaerok.backend.user.exception.UserErrorCode;
import com.chaerok.backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private OAuthTokenVerifierResolver verifierResolver;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private OAuthTokenVerifier verifier;

    @Mock
    private OAuthLoginRequest loginRequest;

    @Mock
    private SignupRequest signupRequest;

    @Mock
    private RefreshToken savedToken;

    @Mock
    private User user;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                verifierResolver,
                userService,
                jwtTokenProvider,
                refreshTokenService
        );
    }

    @Test
    @DisplayName("기존 OAuth 사용자는 Access Token과 Refresh Token을 발급한다")
    void loginExistingUser() {
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        LocalDateTime expiresAt =
                LocalDateTime.of(2026, 9, 10, 12, 0);

        OAuthUserInfo oauthUserInfo = new OAuthUserInfo(
                OAuthProvider.KAKAO,
                "provider-user-id",
                "채록",
                "chaerok@example.com"
        );

        when(loginRequest.provider())
                .thenReturn(OAuthProvider.KAKAO);
        when(loginRequest.idToken())
                .thenReturn("id-token");
        when(loginRequest.nonce())
                .thenReturn(null);

        when(verifierResolver.resolve(OAuthProvider.KAKAO))
                .thenReturn(verifier);
        when(verifier.verify("id-token", null))
                .thenReturn(oauthUserInfo);

        when(userService.findByOAuthProvider(
                OAuthProvider.KAKAO,
                "provider-user-id"
        )).thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);
        when(user.getRole()).thenReturn(UserRole.USER);

        when(jwtTokenProvider.createAccessToken(
                1L,
                UserRole.USER
        )).thenReturn(accessToken);

        when(jwtTokenProvider.createRefreshToken(1L))
                .thenReturn(refreshToken);

        when(jwtTokenProvider.getExpiration(refreshToken))
                .thenReturn(expiresAt);

        OAuthLoginResponse response =
                authService.login(loginRequest);

        assertThat(response.registered()).isTrue();
        assertThat(response.signupToken()).isNull();
        assertThat(response.tokens()).isNotNull();
        assertThat(response.tokens().accessToken())
                .isEqualTo(accessToken);
        assertThat(response.tokens().refreshToken())
                .isEqualTo(refreshToken);

        verify(refreshTokenService).save(
                user,
                refreshToken,
                expiresAt
        );
    }

    @Test
    @DisplayName("신규 OAuth 사용자는 회원가입 토큰을 반환한다")
    void loginNewUser() {
        OAuthUserInfo oauthUserInfo = new OAuthUserInfo(
                OAuthProvider.KAKAO,
                "provider-user-id",
                "카카오닉네임",
                "chaerok@example.com"
        );

        when(loginRequest.provider())
                .thenReturn(OAuthProvider.KAKAO);
        when(loginRequest.idToken())
                .thenReturn("id-token");
        when(loginRequest.nonce())
                .thenReturn(null);

        when(verifierResolver.resolve(OAuthProvider.KAKAO))
                .thenReturn(verifier);
        when(verifier.verify("id-token", null))
                .thenReturn(oauthUserInfo);

        when(userService.findByOAuthProvider(
                OAuthProvider.KAKAO,
                "provider-user-id"
        )).thenReturn(Optional.empty());

        when(jwtTokenProvider.createSignupToken(
                OAuthProvider.KAKAO,
                "provider-user-id",
                "카카오닉네임",
                "chaerok@example.com"
        )).thenReturn("signup-token");

        OAuthLoginResponse response =
                authService.login(loginRequest);

        assertThat(response.registered()).isFalse();
        assertThat(response.tokens()).isNull();
        assertThat(response.signupToken())
                .isEqualTo("signup-token");

        verify(refreshTokenService, never()).save(
                user,
                "signup-token",
                null
        );
    }

    @Test
    @DisplayName("OAuth 닉네임이 없으면 기본 닉네임으로 회원가입 토큰을 생성한다")
    void loginNewUserUsesDefaultNickname() {
        OAuthUserInfo oauthUserInfo = new OAuthUserInfo(
                OAuthProvider.KAKAO,
                "provider-user-id",
                null,
                "chaerok@example.com"
        );

        when(loginRequest.provider())
                .thenReturn(OAuthProvider.KAKAO);
        when(loginRequest.idToken())
                .thenReturn("id-token");
        when(loginRequest.nonce())
                .thenReturn(null);

        when(verifierResolver.resolve(OAuthProvider.KAKAO))
                .thenReturn(verifier);
        when(verifier.verify("id-token", null))
                .thenReturn(oauthUserInfo);

        when(userService.findByOAuthProvider(
                OAuthProvider.KAKAO,
                "provider-user-id"
        )).thenReturn(Optional.empty());

        when(jwtTokenProvider.createSignupToken(
                OAuthProvider.KAKAO,
                "provider-user-id",
                "채록 사용자",
                "chaerok@example.com"
        )).thenReturn("signup-token");

        OAuthLoginResponse response =
                authService.login(loginRequest);

        assertThat(response.registered()).isFalse();
        assertThat(response.signupToken())
                .isEqualTo("signup-token");

        verify(jwtTokenProvider).createSignupToken(
                OAuthProvider.KAKAO,
                "provider-user-id",
                "채록 사용자",
                "chaerok@example.com"
        );
    }

    @Test
    @DisplayName("회원가입 토큰 정보로 사용자를 생성하고 Access Token과 Refresh Token을 발급한다")
    void signup() {
        SignupTokenInfo tokenInfo = new SignupTokenInfo(
                OAuthProvider.KAKAO,
                "provider-user-id",
                "chaerok@example.com"
        );

        LocalDateTime expiresAt =
                LocalDateTime.of(2026, 9, 10, 12, 0);

        when(signupRequest.signupToken())
                .thenReturn("signup-token");
        when(signupRequest.nickname())
                .thenReturn("채록");

        when(jwtTokenProvider.getSignupTokenInfo("signup-token"))
                .thenReturn(tokenInfo);

        when(userService.findByOAuthProvider(
                OAuthProvider.KAKAO,
                "provider-user-id"
        )).thenReturn(Optional.empty());

        when(userService.createUser(
                OAuthProvider.KAKAO,
                "provider-user-id",
                "채록",
                "chaerok@example.com",
                TermsVersion.SERVICE_TERMS,
                TermsVersion.PRIVACY_POLICY
        )).thenReturn(user);

        when(user.getId()).thenReturn(1L);
        when(user.getRole()).thenReturn(UserRole.USER);

        when(jwtTokenProvider.createAccessToken(
                1L,
                UserRole.USER
        )).thenReturn("access-token");

        when(jwtTokenProvider.createRefreshToken(1L))
                .thenReturn("refresh-token");

        when(jwtTokenProvider.getExpiration("refresh-token"))
                .thenReturn(expiresAt);

        TokenResponse response =
                authService.signup(signupRequest);

        assertThat(response.accessToken())
                .isEqualTo("access-token");
        assertThat(response.refreshToken())
                .isEqualTo("refresh-token");

        verify(userService).createUser(
                OAuthProvider.KAKAO,
                "provider-user-id",
                "채록",
                "chaerok@example.com",
                TermsVersion.SERVICE_TERMS,
                TermsVersion.PRIVACY_POLICY
        );

        verify(refreshTokenService).save(
                user,
                "refresh-token",
                expiresAt
        );
    }

    @Test
    @DisplayName("이미 가입된 OAuth 사용자는 다시 회원가입할 수 없다")
    void signupRejectsAlreadyRegisteredUser() {
        SignupTokenInfo tokenInfo = new SignupTokenInfo(
                OAuthProvider.KAKAO,
                "provider-user-id",
                "chaerok@example.com"
        );

        when(signupRequest.signupToken())
                .thenReturn("signup-token");

        when(jwtTokenProvider.getSignupTokenInfo("signup-token"))
                .thenReturn(tokenInfo);

        when(userService.findByOAuthProvider(
                OAuthProvider.KAKAO,
                "provider-user-id"
        )).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.signup(signupRequest)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserErrorCode.ALREADY_REGISTERED)
        );

        verify(userService, never()).createUser(
                OAuthProvider.KAKAO,
                "provider-user-id",
                null,
                "chaerok@example.com",
                TermsVersion.SERVICE_TERMS,
                TermsVersion.PRIVACY_POLICY
        );

        verify(refreshTokenService, never())
                .save(
                        user,
                        "refresh-token",
                        null
                );
    }

    @Test
    @DisplayName("refresh는 rotation 전용 락 조회 후 기존 토큰을 폐기하고 새 토큰을 발급한다")
    void refreshUsesLockedRefreshTokenLookup() {
        String oldRefreshToken = "old-refresh-token";
        String newRefreshToken = "new-refresh-token";
        LocalDateTime expiresAt =
                LocalDateTime.of(2026, 9, 10, 12, 0);

        RefreshTokenRequest request =
                new RefreshTokenRequest(oldRefreshToken);

        when(jwtTokenProvider.getRefreshTokenUserId(oldRefreshToken))
                .thenReturn(1L);

        when(refreshTokenService.findValidTokenForUpdate(oldRefreshToken))
                .thenReturn(savedToken);

        when(savedToken.getUser())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(1L);

        when(user.getRole())
                .thenReturn(UserRole.USER);

        when(jwtTokenProvider.createAccessToken(
                1L,
                UserRole.USER
        )).thenReturn("new-access-token");

        when(jwtTokenProvider.createRefreshToken(1L))
                .thenReturn(newRefreshToken);

        when(jwtTokenProvider.getExpiration(newRefreshToken))
                .thenReturn(expiresAt);

        TokenResponse response =
                authService.refresh(request);

        assertThat(response.accessToken())
                .isEqualTo("new-access-token");
        assertThat(response.refreshToken())
                .isEqualTo(newRefreshToken);

        verify(refreshTokenService)
                .findValidTokenForUpdate(oldRefreshToken);

        verify(refreshTokenService, never())
                .findValidToken(oldRefreshToken);

        verify(refreshTokenService)
                .delete(oldRefreshToken);

        verify(refreshTokenService)
                .save(
                        user,
                        newRefreshToken,
                        expiresAt
                );
    }

    @Test
    @DisplayName("Refresh Token의 사용자와 저장된 토큰 사용자가 다르면 rotation을 거부한다")
    void refreshRejectsUserMismatch() {
        String oldRefreshToken = "old-refresh-token";

        RefreshTokenRequest request =
                new RefreshTokenRequest(oldRefreshToken);

        when(jwtTokenProvider.getRefreshTokenUserId(oldRefreshToken))
                .thenReturn(1L);

        when(refreshTokenService.findValidTokenForUpdate(oldRefreshToken))
                .thenReturn(savedToken);

        when(savedToken.getUser())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(2L);

        assertThatThrownBy(() ->
                authService.refresh(request)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                AuthErrorCode.REFRESH_TOKEN_USER_MISMATCH
                        )
        );

        verify(refreshTokenService, never())
                .delete(oldRefreshToken);

        verify(jwtTokenProvider, never())
                .createAccessToken(
                        2L,
                        UserRole.USER
                );

        verify(jwtTokenProvider, never())
                .createRefreshToken(2L);
    }

    @Test
    @DisplayName("logout은 기존 일반 Refresh Token 조회 후 토큰을 삭제한다")
    void logoutUsesNormalRefreshTokenLookup() {
        String refreshToken = "refresh-token";

        RefreshTokenRequest request =
                new RefreshTokenRequest(refreshToken);

        when(jwtTokenProvider.getRefreshTokenUserId(refreshToken))
                .thenReturn(1L);

        when(refreshTokenService.findValidToken(refreshToken))
                .thenReturn(savedToken);

        when(savedToken.getUser())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(1L);

        authService.logout(request);

        verify(refreshTokenService)
                .findValidToken(refreshToken);

        verify(refreshTokenService, never())
                .findValidTokenForUpdate(anyString());

        verify(refreshTokenService)
                .delete(refreshToken);
    }

    @Test
    @DisplayName("로그아웃 토큰의 사용자와 저장된 토큰 사용자가 다르면 삭제하지 않는다")
    void logoutRejectsUserMismatch() {
        String refreshToken = "refresh-token";

        RefreshTokenRequest request =
                new RefreshTokenRequest(refreshToken);

        when(jwtTokenProvider.getRefreshTokenUserId(refreshToken))
                .thenReturn(1L);

        when(refreshTokenService.findValidToken(refreshToken))
                .thenReturn(savedToken);

        when(savedToken.getUser())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(2L);

        assertThatThrownBy(() ->
                authService.logout(request)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                AuthErrorCode.REFRESH_TOKEN_USER_MISMATCH
                        )
        );

        verify(refreshTokenService, never())
                .delete(refreshToken);
    }
}