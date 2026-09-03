package com.chaerok.backend.notification.device.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.notification.device.dto.PushDeviceRegisterRequest;
import com.chaerok.backend.notification.device.dto.PushDeviceUnregisterRequest;
import com.chaerok.backend.notification.device.entity.PushPlatform;
import com.chaerok.backend.notification.device.service.PushDeviceService;
import com.chaerok.backend.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PushDeviceControllerTest {

    @Mock
    private PushDeviceService pushDeviceService;

    private PushDeviceController controller;
    private AuthenticatedUser authenticatedUser;

    @BeforeEach
    void setUp() {
        controller = new PushDeviceController(pushDeviceService);
        authenticatedUser =
                new AuthenticatedUser(6L, UserRole.USER);
    }

    @Test
    @DisplayName("현재 사용자의 FCM 토큰을 등록하고 204를 반환한다")
    void register() {
        ResponseEntity<Void> response = controller.register(
                authenticatedUser,
                new PushDeviceRegisterRequest(
                        "token-1",
                        PushPlatform.ANDROID
                )
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(pushDeviceService).register(
                6L,
                "token-1",
                PushPlatform.ANDROID
        );
    }

    @Test
    @DisplayName("현재 사용자의 iOS FCM 토큰을 등록하고 204를 반환한다")
    void registerIos() {
        ResponseEntity<Void> response = controller.register(
                authenticatedUser,
                new PushDeviceRegisterRequest(
                        "ios-token-1",
                        PushPlatform.IOS
                )
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(pushDeviceService).register(
                6L,
                "ios-token-1",
                PushPlatform.IOS
        );
    }

    @Test
    @DisplayName("현재 사용자의 FCM 토큰을 해제하고 204를 반환한다")
    void unregister() {
        ResponseEntity<Void> response = controller.unregister(
                authenticatedUser,
                new PushDeviceUnregisterRequest("token-1")
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(pushDeviceService).unregister(
                6L,
                "token-1"
        );
    }
}
