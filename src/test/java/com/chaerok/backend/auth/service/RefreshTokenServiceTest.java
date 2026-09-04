package com.chaerok.backend.auth.service;

import com.chaerok.backend.auth.entity.RefreshToken;
import com.chaerok.backend.auth.exception.AuthErrorCode;
import com.chaerok.backend.auth.jwt.TokenHashUtil;
import com.chaerok.backend.auth.repository.RefreshTokenRepository;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenHashUtil tokenHashUtil;

    @Mock
    private RefreshToken refreshToken;

    @Mock
    private User user;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                tokenHashUtil
        );
    }

    @Test
    @DisplayName("Refresh Token 저장 시 원본 토큰을 해시하여 저장한다")
    void saveHashesRawToken() {
        String rawToken = "refresh-token";
        String tokenHash = "hashed-token";
        LocalDateTime expiresAt =
                LocalDateTime.of(2026, 9, 10, 12, 0);

        when(tokenHashUtil.hash(rawToken))
                .thenReturn(tokenHash);

        refreshTokenService.save(
                user,
                rawToken,
                expiresAt
        );

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository)
                .save(captor.capture());

        RefreshToken saved = captor.getValue();

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getTokenHash()).isEqualTo(tokenHash);
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("저장된 Refresh Token을 정상 조회한다")
    void findValidToken() {
        String rawToken = "refresh-token";
        String tokenHash = "hashed-token";

        when(tokenHashUtil.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(refreshToken));

        when(refreshToken.getExpiresAt())
                .thenReturn(LocalDateTime.now().plusDays(1));

        RefreshToken result =
                refreshTokenService.findValidToken(rawToken);

        assertThat(result).isSameAs(refreshToken);

        verify(refreshTokenRepository)
                .findByTokenHash(tokenHash);
    }

    @Test
    @DisplayName("저장되지 않은 Refresh Token 조회 시 예외가 발생한다")
    void findValidTokenRejectsUnknownToken() {
        String rawToken = "refresh-token";
        String tokenHash = "hashed-token";

        when(tokenHashUtil.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                refreshTokenService.findValidToken(rawToken)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND)
        );
    }

    @Test
    @DisplayName("만료된 Refresh Token 조회 시 예외가 발생한다")
    void findValidTokenRejectsExpiredToken() {
        String rawToken = "refresh-token";
        String tokenHash = "hashed-token";

        when(tokenHashUtil.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(refreshToken));

        when(refreshToken.getExpiresAt())
                .thenReturn(LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() ->
                refreshTokenService.findValidToken(rawToken)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(AuthErrorCode.EXPIRED_REFRESH_TOKEN)
        );
    }

    @Test
    @DisplayName("rotation용 조회는 비관적 락 Repository 메서드를 사용한다")
    void findValidTokenForUpdateUsesLockedQuery() {
        String rawToken = "refresh-token";
        String tokenHash = "hashed-token";

        when(tokenHashUtil.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(refreshToken));

        when(refreshToken.getExpiresAt())
                .thenReturn(LocalDateTime.now().plusDays(1));

        RefreshToken result =
                refreshTokenService.findValidTokenForUpdate(rawToken);

        assertThat(result).isSameAs(refreshToken);

        verify(refreshTokenRepository)
                .findByTokenHashForUpdate(tokenHash);
    }

    @Test
    @DisplayName("rotation용 조회에서 저장되지 않은 Refresh Token이면 예외가 발생한다")
    void findValidTokenForUpdateRejectsUnknownToken() {
        String rawToken = "refresh-token";
        String tokenHash = "hashed-token";

        when(tokenHashUtil.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                refreshTokenService.findValidTokenForUpdate(rawToken)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND)
        );
    }

    @Test
    @DisplayName("rotation용 조회에서 만료된 Refresh Token이면 예외가 발생한다")
    void findValidTokenForUpdateRejectsExpiredToken() {
        String rawToken = "refresh-token";
        String tokenHash = "hashed-token";

        when(tokenHashUtil.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(refreshToken));

        when(refreshToken.getExpiresAt())
                .thenReturn(LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() ->
                refreshTokenService.findValidTokenForUpdate(rawToken)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(AuthErrorCode.EXPIRED_REFRESH_TOKEN)
        );
    }

    @Test
    @DisplayName("Refresh Token 삭제 시 원본 토큰을 해시하여 삭제한다")
    void deleteHashesRawToken() {
        String rawToken = "refresh-token";
        String tokenHash = "hashed-token";

        when(tokenHashUtil.hash(rawToken))
                .thenReturn(tokenHash);

        refreshTokenService.delete(rawToken);

        verify(refreshTokenRepository)
                .deleteByTokenHash(tokenHash);
    }

    @Test
    @DisplayName("사용자 ID 기준으로 모든 Refresh Token을 삭제한다")
    void deleteAllByUserId() {
        refreshTokenService.deleteAllByUserId(1L);

        verify(refreshTokenRepository)
                .deleteAllByUserId(1L);
    }
}