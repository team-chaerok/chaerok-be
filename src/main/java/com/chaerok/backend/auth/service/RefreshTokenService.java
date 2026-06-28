package com.chaerok.backend.auth.service;

import com.chaerok.backend.auth.entity.RefreshToken;
import com.chaerok.backend.auth.jwt.TokenHashUtil;
import com.chaerok.backend.auth.repository.RefreshTokenRepository;
import com.chaerok.backend.global.exception.InvalidTokenException;
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
                                new InvalidTokenException(
                                        "저장되지 않았거나 폐기된 Refresh Token입니다."
                                )
                        );

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException(
                    "만료된 Refresh Token입니다."
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