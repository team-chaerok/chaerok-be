package com.chaerok.backend.auth.service;

import com.chaerok.backend.auth.entity.RefreshToken;
import com.chaerok.backend.auth.jwt.TokenHashUtil;
import com.chaerok.backend.auth.repository.RefreshTokenRepository;
import com.chaerok.backend.global.exception.InvalidTokenException;
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

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                tokenHashUtil
        );
    }

    @Test
    @DisplayName("rotation용 조회는 비관적 락 Repository 메서드를 사용한다")
    void findValidTokenForUpdateUsesLockedQuery() {
        // given
        String rawToken = "refresh-token";
        String tokenHash = "hashed-token";

        when(tokenHashUtil.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(refreshToken));

        when(refreshToken.getExpiresAt())
                .thenReturn(LocalDateTime.now().plusDays(1));

        // when
        RefreshToken result =
                refreshTokenService.findValidTokenForUpdate(rawToken);

        // then
        assertThat(result).isSameAs(refreshToken);

        verify(refreshTokenRepository)
                .findByTokenHashForUpdate(tokenHash);
    }

    @Test
    @DisplayName("rotation용 조회에서 저장되지 않은 Refresh Token이면 예외가 발생한다")
    void findValidTokenForUpdateRejectsUnknownToken() {
        // given
        String rawToken = "refresh-token";
        String tokenHash = "hashed-token";

        when(tokenHashUtil.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                refreshTokenService.findValidTokenForUpdate(rawToken)
        )
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage(
                        "저장되지 않았거나 폐기된 Refresh Token입니다."
                );
    }

    @Test
    @DisplayName("rotation용 조회에서 만료된 Refresh Token이면 예외가 발생한다")
    void findValidTokenForUpdateRejectsExpiredToken() {
        // given
        String rawToken = "refresh-token";
        String tokenHash = "hashed-token";

        when(tokenHashUtil.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(refreshToken));

        when(refreshToken.getExpiresAt())
                .thenReturn(LocalDateTime.now().minusMinutes(1));

        // when & then
        assertThatThrownBy(() ->
                refreshTokenService.findValidTokenForUpdate(rawToken)
        )
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("만료된 Refresh Token입니다.");
    }
}