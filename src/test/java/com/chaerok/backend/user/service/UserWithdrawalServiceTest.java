package com.chaerok.backend.user.service;

import com.chaerok.backend.auth.oauth.unlink.KakaoOAuthUnlinkService;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserWithdrawalServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private KakaoOAuthUnlinkService kakaoOAuthUnlinkService;

    @Mock
    private UserWithdrawalPersistenceService persistenceService;

    @Mock
    private User user;

    private UserWithdrawalService userWithdrawalService;

    @BeforeEach
    void setUp() {
        userWithdrawalService = new UserWithdrawalService(
                userService,
                kakaoOAuthUnlinkService,
                persistenceService
        );
    }

    @Test
    void 카카오_사용자_탈퇴_시_연결을_해제한_뒤_DB_삭제를_위임한다() {
        // given
        Long userId = 1L;
        String providerUserId = "123456789";

        when(userService.findById(userId)).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(user.getProvider()).thenReturn(OAuthProvider.KAKAO);
        when(user.getProviderUserId()).thenReturn(providerUserId);

        // when
        userWithdrawalService.withdraw(userId);

        // then
        InOrder inOrder = inOrder(
                userService,
                kakaoOAuthUnlinkService,
                persistenceService
        );

        inOrder.verify(userService)
                .findById(userId);

        inOrder.verify(kakaoOAuthUnlinkService)
                .unlink(providerUserId);

        inOrder.verify(persistenceService)
                .deleteUserData(userId);
    }

    @Test
    void 구글_사용자_탈퇴_시_카카오_연결_해제_없이_DB_삭제를_위임한다() {
        // given
        Long userId = 2L;

        when(userService.findById(userId)).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(user.getProvider()).thenReturn(OAuthProvider.GOOGLE);

        // when
        userWithdrawalService.withdraw(userId);

        // then
        verify(kakaoOAuthUnlinkService, never())
                .unlink(anyString());

        verify(persistenceService)
                .deleteUserData(userId);
    }
}