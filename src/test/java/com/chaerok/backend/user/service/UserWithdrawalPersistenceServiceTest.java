package com.chaerok.backend.user.service;

import com.chaerok.backend.auth.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class UserWithdrawalPersistenceServiceTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserService userService;

    private UserWithdrawalPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new UserWithdrawalPersistenceService(
                refreshTokenService,
                userService
        );
    }

    @Test
    void 회원탈퇴_DB_처리_시_리프레시_토큰과_사용자를_순서대로_삭제한다() {
        // given
        Long userId = 1L;

        // when
        persistenceService.deleteUserData(userId);

        // then
        InOrder inOrder = inOrder(
                refreshTokenService,
                userService
        );

        inOrder.verify(refreshTokenService)
                .deleteAllByUserId(userId);

        inOrder.verify(userService)
                .deleteUser(userId);
    }
}