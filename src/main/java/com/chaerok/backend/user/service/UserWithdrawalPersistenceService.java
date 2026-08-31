package com.chaerok.backend.user.service;

import com.chaerok.backend.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserWithdrawalPersistenceService {

    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    @Transactional
    public void deleteUserData(Long userId) {
        refreshTokenService.deleteAllByUserId(userId);
        userService.deleteUser(userId);
    }
}