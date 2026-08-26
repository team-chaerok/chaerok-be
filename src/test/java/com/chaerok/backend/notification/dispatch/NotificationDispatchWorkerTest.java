package com.chaerok.backend.notification.dispatch;

import com.chaerok.backend.notification.device.entity.PushDevice;
import com.chaerok.backend.notification.device.entity.PushPlatform;
import com.chaerok.backend.notification.device.repository.PushDeviceRepository;
import com.chaerok.backend.notification.message.NotificationPayload;
import com.chaerok.backend.notification.message.NotificationPayloadFactory;
import com.chaerok.backend.notification.outbox.entity.NotificationOutbox;
import com.chaerok.backend.notification.outbox.entity.NotificationStatus;
import com.chaerok.backend.notification.outbox.entity.NotificationType;
import com.chaerok.backend.notification.outbox.repository.NotificationOutboxRepository;
import com.chaerok.backend.notification.sender.PushSendResult;
import com.chaerok.backend.notification.sender.PushSender;
import com.chaerok.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchWorkerTest {

    @Mock
    private NotificationOutboxRepository outboxRepository;

    @Mock
    private PushDeviceRepository pushDeviceRepository;

    @Mock
    private PushSender pushSender;

    @Mock
    private User user;

    private NotificationDispatchWorker worker;

    @BeforeEach
    void setUp() {
        worker = new NotificationDispatchWorker(
                outboxRepository,
                pushDeviceRepository,
                new NotificationPayloadFactory(),
                pushSender
        );
    }

    @Test
    @DisplayName("등록 기기로 전송에 성공하면 Outbox를 SENT로 전환한다")
    void sent() {
        NotificationOutbox outbox = outbox();
        PushDevice device = device();
        LocalDateTime now =
                LocalDateTime.of(2026, 8, 27, 2, 30);

        when(outboxRepository.findById(1L))
                .thenReturn(Optional.of(outbox));
        when(pushDeviceRepository.findAllByUserId(6L))
                .thenReturn(List.of(device));
        when(pushSender.send(
                eq("token-1"),
                any(NotificationPayload.class)
        )).thenReturn(PushSendResult.sent());

        worker.dispatchOne(1L, now);

        assertThat(outbox.getStatus())
                .isEqualTo(NotificationStatus.SENT);
        assertThat(outbox.getSentAt())
                .isEqualTo(now);
        verify(pushDeviceRepository, never())
                .delete(any(PushDevice.class));
    }

    @Test
    @DisplayName("UNREGISTERED 등 무효 토큰은 삭제하고 Outbox 처리를 종료한다")
    void invalidToken() {
        NotificationOutbox outbox = outbox();
        PushDevice device = device();

        when(outboxRepository.findById(1L))
                .thenReturn(Optional.of(outbox));
        when(pushDeviceRepository.findAllByUserId(6L))
                .thenReturn(List.of(device));
        when(pushSender.send(
                eq("token-1"),
                any(NotificationPayload.class)
        )).thenReturn(
                PushSendResult.invalidToken(
                        "UNREGISTERED",
                        "invalid token"
                )
        );

        worker.dispatchOne(
                1L,
                LocalDateTime.now()
        );

        verify(pushDeviceRepository).delete(device);
        assertThat(outbox.getStatus())
                .isEqualTo(NotificationStatus.SENT);
    }

    @Test
    @DisplayName("FCM 일시 장애는 Outbox를 PENDING으로 유지하고 재시도를 예약한다")
    void retryableFailure() {
        NotificationOutbox outbox = outbox();
        PushDevice device = device();
        LocalDateTime now =
                LocalDateTime.of(2026, 8, 27, 2, 30);

        when(outboxRepository.findById(1L))
                .thenReturn(Optional.of(outbox));
        when(pushDeviceRepository.findAllByUserId(6L))
                .thenReturn(List.of(device));
        when(pushSender.send(
                eq("token-1"),
                any(NotificationPayload.class)
        )).thenReturn(
                PushSendResult.retryable(
                        "UNAVAILABLE",
                        "temporary"
                )
        );

        worker.dispatchOne(1L, now);

        assertThat(outbox.getStatus())
                .isEqualTo(NotificationStatus.PENDING);
        assertThat(outbox.getAttemptCount()).isEqualTo(1);
        assertThat(outbox.getNextAttemptAt())
                .isEqualTo(now.plusSeconds(30));
    }

    @Test
    @DisplayName("등록 기기가 없으면 재시도하지 않고 Outbox를 종료한다")
    void noDevice() {
        NotificationOutbox outbox = outbox();

        when(outboxRepository.findById(1L))
                .thenReturn(Optional.of(outbox));
        when(pushDeviceRepository.findAllByUserId(6L))
                .thenReturn(List.of());

        worker.dispatchOne(
                1L,
                LocalDateTime.now()
        );

        assertThat(outbox.getStatus())
                .isEqualTo(NotificationStatus.SENT);
        verify(pushSender, never())
                .send(
                        any(),
                        any(NotificationPayload.class)
                );
    }

    private NotificationOutbox outbox() {
        UUID renderJobId = UUID.randomUUID();
        return NotificationOutbox.pending(
                "render:"
                        + renderJobId
                        + ":COMPLETED",
                6L,
                14L,
                renderJobId,
                NotificationType.RENDER_COMPLETED,
                LocalDateTime.now()
        );
    }

    private PushDevice device() {
        return PushDevice.create(
                user,
                "token-1",
                PushPlatform.ANDROID,
                LocalDateTime.now()
        );
    }
}