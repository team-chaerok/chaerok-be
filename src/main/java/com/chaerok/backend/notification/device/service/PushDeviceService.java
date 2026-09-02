package com.chaerok.backend.notification.device.service;

import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.notification.device.entity.PushDevice;
import com.chaerok.backend.notification.device.entity.PushPlatform;
import com.chaerok.backend.notification.device.repository.PushDeviceRepository;
import com.chaerok.backend.notification.exception.NotificationErrorCode;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PushDeviceService {

    private final PushDeviceRepository pushDeviceRepository;
    private final UserService userService;

    @Transactional
    public void register(
            Long userId,
            String token,
            PushPlatform platform
    ) {
        String normalizedToken = normalizeToken(token);
        User user = userService.findById(userId);
        LocalDateTime registeredAt = LocalDateTime.now();

        pushDeviceRepository
                .findByFcmRegistrationToken(normalizedToken)
                .ifPresentOrElse(
                        device -> device.registerTo(
                                user,
                                normalizedToken,
                                platform,
                                registeredAt
                        ),
                        () -> pushDeviceRepository.save(
                                PushDevice.create(
                                        user,
                                        normalizedToken,
                                        platform,
                                        registeredAt
                                )
                        )
                );
    }

    @Transactional
    public void unregister(
            Long userId,
            String token
    ) {
        pushDeviceRepository
                .deleteByUserIdAndFcmRegistrationToken(
                        userId,
                        normalizeToken(token)
                );
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(
                    NotificationErrorCode.INVALID_FCM_TOKEN
            );
        }

        String normalized = token.trim();

        if (normalized.length() > 4096) {
            throw new BusinessException(
                    NotificationErrorCode.INVALID_FCM_TOKEN
            );
        }

        return normalized;
    }
}