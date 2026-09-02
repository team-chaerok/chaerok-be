package com.chaerok.backend.user.service;

import com.chaerok.backend.auth.oauth.service.AppleOAuthRevokeService;
import com.chaerok.backend.auth.oauth.unlink.KakaoOAuthUnlinkService;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserWithdrawalServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private KakaoOAuthUnlinkService kakaoOAuthUnlinkService;

    @Mock
    private AppleOAuthRevokeService appleOAuthRevokeService;

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
                persistenceService,
                appleOAuthRevokeService
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
        userWithdrawalService.withdraw(userId, null);

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

        verifyNoInteractions(appleOAuthRevokeService);
    }

    @Test
    void 구글_사용자_탈퇴_시_OAuth_연결_해제_없이_DB_삭제를_위임한다() {
        // given
        Long userId = 2L;

        when(userService.findById(userId)).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(user.getProvider()).thenReturn(OAuthProvider.GOOGLE);

        // when
        userWithdrawalService.withdraw(userId, null);

        // then
        verifyNoInteractions(kakaoOAuthUnlinkService);
        verifyNoInteractions(appleOAuthRevokeService);

        verify(persistenceService)
                .deleteUserData(userId);
    }

    @Test
    void 애플_사용자_탈퇴_시_revoke_후_DB_삭제를_위임한다() {
        // given
        Long userId = 3L;
        String authorizationCode = "apple-authorization-code";
        String providerUserId = "apple-user-id";

        when(user.getProviderUserId()).thenReturn(providerUserId);
        when(userService.findById(userId)).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(user.getProvider()).thenReturn(OAuthProvider.APPLE);

        // when
        userWithdrawalService.withdraw(userId, authorizationCode);

        // then
        InOrder inOrder = inOrder(
                userService,
                appleOAuthRevokeService,
                persistenceService
        );

        inOrder.verify(userService)
                .findById(userId);

        inOrder.verify(appleOAuthRevokeService)
                .revoke(authorizationCode, providerUserId);

        inOrder.verify(persistenceService)
                .deleteUserData(userId);

        verifyNoInteractions(kakaoOAuthUnlinkService);
    }

    @Test
    void 애플_사용자_탈퇴_시_authorizationCode가_없으면_DB를_삭제하지_않는다() {
        // given
        Long userId = 3L;

        when(userService.findById(userId)).thenReturn(user);
        when(user.getProvider()).thenReturn(OAuthProvider.APPLE);

        // when & then
        assertThatThrownBy(
                () -> userWithdrawalService.withdraw(userId, null)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage("Apple 회원탈퇴에는 authorizationCode가 필요합니다.");

        verifyNoInteractions(appleOAuthRevokeService);
        verify(persistenceService, never())
                .deleteUserData(anyLong());
    }
}