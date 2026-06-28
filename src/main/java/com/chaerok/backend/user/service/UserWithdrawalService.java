package com.chaerok.backend.user.service;

import com.chaerok.backend.auth.oauth.unlink.KakaoOAuthUnlinkService;
import com.chaerok.backend.auth.service.RefreshTokenService;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final KakaoOAuthUnlinkService kakaoOAuthUnlinkService;

    @Transactional
    public void withdraw(Long userId) {
        User user = userService.findById(userId);

        if (user.getProvider() == OAuthProvider.KAKAO) {
            kakaoOAuthUnlinkService.unlink(
                    user.getProviderUserId()
            );
        }

        refreshTokenService.deleteAllByUserId(user.getId());

        userService.deleteUser(user.getId());
    }
}