package com.chaerok.backend.notification.device.service;

import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.notification.device.entity.PushDevice;
import com.chaerok.backend.notification.device.entity.PushPlatform;
import com.chaerok.backend.notification.device.repository.PushDeviceRepository;
import com.chaerok.backend.notification.exception.NotificationErrorCode;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @DisplayName("새 FCM 토큰을 정규화하여 현재 사용자에게 등록한다")
    void registerNewToken() {
        // given
        when(userService.findById(6L)).thenReturn(user);
        when(repository.findByFcmRegistrationToken("token-1"))
                .thenReturn(Optional.empty());

        // when
        service.register(
                6L,
                " token-1 ",
                PushPlatform.ANDROID
        );

        // then
        ArgumentCaptor<PushDevice> captor =
                ArgumentCaptor.forClass(PushDevice.class);

        verify(repository).save(captor.capture());

        PushDevice savedDevice = captor.getValue();

        assertThat(savedDevice.getUser()).isSameAs(user);
        assertThat(savedDevice.getFcmRegistrationToken())
                .isEqualTo("token-1");
        assertThat(savedDevice.getPlatform())
                .isEqualTo(PushPlatform.ANDROID);
        assertThat(savedDevice.getLastRegisteredAt()).isNotNull();
    }

    @Test
    @DisplayName("기존 FCM 토큰 재등록은 기존 기기의 사용자와 등록 시각을 갱신한다")
    void registerExistingToken() {
        // given
        User previousUser = org.mockito.Mockito.mock(User.class);
        java.time.LocalDateTime previousRegisteredAt =
                java.time.LocalDateTime.now().minusDays(1);

        PushDevice existing = PushDevice.create(
                previousUser,
                "token-1",
                PushPlatform.ANDROID,
                previousRegisteredAt
        );

        when(userService.findById(6L)).thenReturn(user);
        when(repository.findByFcmRegistrationToken("token-1"))
                .thenReturn(Optional.of(existing));

        // when
        service.register(
                6L,
                " token-1 ",
                PushPlatform.ANDROID
        );

        // then
        verify(repository, never()).save(any(PushDevice.class));

        assertThat(existing.getUser()).isSameAs(user);
        assertThat(existing.getFcmRegistrationToken())
                .isEqualTo("token-1");
        assertThat(existing.getPlatform())
                .isEqualTo(PushPlatform.ANDROID);
        assertThat(existing.getLastRegisteredAt())
                .isAfter(previousRegisteredAt);
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

    @Test
    @DisplayName("FCM 토큰이 null이면 등록에 실패한다")
    void rejectNullToken() {
        // when & then
        assertThatThrownBy(
                () -> service.register(
                        6L,
                        null,
                        PushPlatform.ANDROID
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                NotificationErrorCode.INVALID_FCM_TOKEN
                        )
        );

        verifyNoInteractions(userService, repository);
    }

    @Test
    @DisplayName("FCM 토큰이 공백이면 등록에 실패한다")
    void rejectBlankToken() {
        // when & then
        assertThatThrownBy(
                () -> service.register(
                        6L,
                        "   ",
                        PushPlatform.ANDROID
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                NotificationErrorCode.INVALID_FCM_TOKEN
                        )
        );

        verifyNoInteractions(userService, repository);
    }

    @Test
    @DisplayName("FCM 토큰이 4096자를 초과하면 등록에 실패한다")
    void rejectTokenLongerThan4096Characters() {
        // given
        String token = "a".repeat(4097);

        // when & then
        assertThatThrownBy(
                () -> service.register(
                        6L,
                        token,
                        PushPlatform.ANDROID
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                NotificationErrorCode.INVALID_FCM_TOKEN
                        )
        );

        verifyNoInteractions(userService, repository);
    }

    @Test
    @DisplayName("해제할 FCM 토큰이 공백이면 삭제하지 않는다")
    void rejectBlankTokenWhenUnregistering() {
        // when & then
        assertThatThrownBy(
                () -> service.unregister(6L, " ")
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                NotificationErrorCode.INVALID_FCM_TOKEN
                        )
        );

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("FCM 토큰이 4096자이면 등록할 수 있다")
    void acceptTokenWith4096Characters() {
        // given
        String token = "a".repeat(4096);

        when(userService.findById(6L)).thenReturn(user);
        when(repository.findByFcmRegistrationToken(token))
                .thenReturn(Optional.empty());

        // when
        service.register(
                6L,
                token,
                PushPlatform.ANDROID
        );

        // then
        ArgumentCaptor<PushDevice> captor =
                ArgumentCaptor.forClass(PushDevice.class);

        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getFcmRegistrationToken())
                .hasSize(4096);
    }
}