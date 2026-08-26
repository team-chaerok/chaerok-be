package com.chaerok.backend.notification.device.service;

import com.chaerok.backend.notification.device.entity.PushDevice;
import com.chaerok.backend.notification.device.entity.PushPlatform;
import com.chaerok.backend.notification.device.repository.PushDeviceRepository;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushDeviceServiceTest {

    @Mock
    private PushDeviceRepository repository;

    @Mock
    private UserService userService;

    @Mock
    private User user;

    private PushDeviceService service;

    @BeforeEach
    void setUp() {
        service = new PushDeviceService(
                repository,
                userService
        );
    }

    @Test
    @DisplayName("새 FCM 토큰을 현재 사용자에게 등록한다")
    void registerNewToken() {
        when(userService.findById(6L)).thenReturn(user);
        when(repository.findByFcmRegistrationToken("token-1"))
                .thenReturn(Optional.empty());

        service.register(
                6L,
                " token-1 ",
                PushPlatform.ANDROID
        );

        verify(repository).save(any(PushDevice.class));
    }

    @Test
    @DisplayName("기존 FCM 토큰 재등록은 새 행을 만들지 않는다")
    void registerExistingToken() {
        PushDevice existing = PushDevice.create(
                user,
                "token-1",
                PushPlatform.ANDROID,
                java.time.LocalDateTime.now()
        );

        when(userService.findById(6L)).thenReturn(user);
        when(repository.findByFcmRegistrationToken("token-1"))
                .thenReturn(Optional.of(existing));

        service.register(
                6L,
                "token-1",
                PushPlatform.ANDROID
        );

        verify(repository, never()).save(any(PushDevice.class));
    }

    @Test
    @DisplayName("현재 사용자와 토큰 연결을 해제한다")
    void unregisterToken() {
        service.unregister(6L, " token-1 ");

        verify(repository)
                .deleteByUserIdAndFcmRegistrationToken(
                        6L,
                        "token-1"
                );
    }
}