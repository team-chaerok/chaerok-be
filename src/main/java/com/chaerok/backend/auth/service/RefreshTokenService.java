package com.chaerok.backend.auth.service;

import com.chaerok.backend.auth.entity.RefreshToken;
import com.chaerok.backend.auth.exception.AuthErrorCode;
import com.chaerok.backend.auth.jwt.TokenHashUtil;
import com.chaerok.backend.auth.repository.RefreshTokenRepository;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashUtil tokenHashUtil;

    @Transactional
    public void save(
            User user,
            String rawToken,
            LocalDateTime expiresAt
    ) {
        String tokenHash = tokenHashUtil.hash(rawToken);

        RefreshToken refreshToken = RefreshToken.create(
                user,
                tokenHash,
                expiresAt
        );

        refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken findValidToken(String rawToken) {
        String tokenHash = tokenHashUtil.hash(rawToken);

        RefreshToken refreshToken =
                refreshTokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new BusinessException(
                                        AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
                                )
                        );

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    AuthErrorCode.EXPIRED_REFRESH_TOKEN
            );
        }

        return refreshToken;
    }

    public RefreshToken findValidTokenForUpdate(String rawToken) {
        String tokenHash = tokenHashUtil.hash(rawToken);

        RefreshToken refreshToken =
                refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(() ->
                                new BusinessException(
                                        AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
                                )
                        );

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    AuthErrorCode.EXPIRED_REFRESH_TOKEN
            );
        }

        return refreshToken;
    }

    @Transactional
    public void delete(String rawToken) {
        String tokenHash = tokenHashUtil.hash(rawToken);

        refreshTokenRepository.deleteByTokenHash(tokenHash);
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }
}