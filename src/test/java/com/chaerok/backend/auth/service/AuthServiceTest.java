package com.chaerok.backend.auth.service;

import com.chaerok.backend.auth.dto.RefreshTokenRequest;
import com.chaerok.backend.auth.entity.RefreshToken;
import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.auth.oauth.verifier.OAuthTokenVerifierResolver;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.entity.UserRole;
import com.chaerok.backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

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
    @DisplayName("refresh는 rotation 전용 락 조회를 사용한다")
    void refreshUsesLockedRefreshTokenLookup() {
        // given
        String oldRefreshToken = "old-refresh-token";
        String newRefreshToken = "new-refresh-token";

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
                .thenReturn(LocalDateTime.now().plusDays(7));

        // when
        authService.refresh(request);

        // then
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
                        jwtTokenProvider.getExpiration(newRefreshToken)
                );
    }

    @Test
    @DisplayName("logout은 기존 일반 Refresh Token 조회를 사용한다")
    void logoutUsesNormalRefreshTokenLookup() {
        // given
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

        // when
        authService.logout(request);

        // then
        verify(refreshTokenService)
                .findValidToken(refreshToken);

        verify(refreshTokenService, never())
                .findValidTokenForUpdate(anyString());

        verify(refreshTokenService)
                .delete(refreshToken);
    }
}