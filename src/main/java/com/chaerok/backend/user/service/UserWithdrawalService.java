package com.chaerok.backend.user.service;

import com.chaerok.backend.auth.oauth.service.AppleOAuthRevokeService;
import com.chaerok.backend.auth.oauth.unlink.KakaoOAuthUnlinkService;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final UserService userService;
    private final KakaoOAuthUnlinkService kakaoOAuthUnlinkService;
    private final UserWithdrawalPersistenceService persistenceService;
    private final AppleOAuthRevokeService appleOAuthRevokeService;

    public void withdraw(Long userId, String authorizationCode) {
        User user = userService.findById(userId);

        if (user.getProvider() == OAuthProvider.KAKAO) {
            kakaoOAuthUnlinkService.unlink(
                    user.getProviderUserId()
            );
        }

        if (user.getProvider() == OAuthProvider.APPLE) {
            if (authorizationCode == null || authorizationCode.isBlank()) {
                throw new BusinessException(
                        UserErrorCode.APPLE_AUTHORIZATION_CODE_REQUIRED
                );
            }

            appleOAuthRevokeService.revoke(
                    authorizationCode,
                    user.getProviderUserId()
            );
        }

        persistenceService.deleteUserData(user.getId());
    }
}