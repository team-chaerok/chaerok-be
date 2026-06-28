package com.chaerok.backend.user.service;

import com.chaerok.backend.auth.oauth.unlink.KakaoOAuthUnlinkService;
import com.chaerok.backend.auth.service.RefreshTokenService;
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
    private RefreshTokenService refreshTokenService;

    @Mock
    private KakaoOAuthUnlinkService kakaoOAuthUnlinkService;

    @Mock
    private User user;

    private UserWithdrawalService userWithdrawalService;

    @BeforeEach
    void setUp() {
        userWithdrawalService = new UserWithdrawalService(
                userService,
                refreshTokenService,
                kakaoOAuthUnlinkService
        );
    }

    @Test
    void 카카오_사용자_탈퇴_시_연결을_해제하고_토큰과_사용자를_삭제한다() {
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
                refreshTokenService
        );

        inOrder.verify(userService).findById(userId);
        inOrder.verify(kakaoOAuthUnlinkService)
                .unlink(providerUserId);
        inOrder.verify(refreshTokenService)
                .deleteAllByUserId(userId);
        inOrder.verify(userService)
                .deleteUser(userId);
    }

    @Test
    void 구글_사용자_탈퇴_시_카카오_연결_해제_없이_토큰과_사용자를_삭제한다() {
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

        verify(refreshTokenService)
                .deleteAllByUserId(userId);

        verify(userService)
                .deleteUser(userId);
    }
}