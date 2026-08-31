package com.chaerok.backend.user.service;

import com.chaerok.backend.auth.oauth.unlink.KakaoOAuthUnlinkService;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final UserService userService;
    private final KakaoOAuthUnlinkService kakaoOAuthUnlinkService;
    private final UserWithdrawalPersistenceService persistenceService;

    public void withdraw(Long userId) {
        User user = userService.findById(userId);

        if (user.getProvider() == OAuthProvider.KAKAO) {
            kakaoOAuthUnlinkService.unlink(
                    user.getProviderUserId()
            );
        }

        persistenceService.deleteUserData(user.getId());
    }
}