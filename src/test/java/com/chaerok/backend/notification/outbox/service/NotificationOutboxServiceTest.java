package com.chaerok.backend.notification.outbox.service;

import com.chaerok.backend.notification.outbox.entity.NotificationOutbox;
import com.chaerok.backend.notification.outbox.entity.NotificationStatus;
import com.chaerok.backend.notification.outbox.entity.NotificationType;
import com.chaerok.backend.notification.outbox.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxServiceTest {

    @Mock
    private NotificationOutboxRepository repository;

    private NotificationOutboxService service;

    @BeforeEach
    void setUp() {
        service = new NotificationOutboxService(repository);
    }

    @Test
    @DisplayName("렌더 시작 알림을 PENDING outbox로 저장한다")
    void enqueueStarted() {
        UUID renderJobId = UUID.randomUUID();
        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 8, 27, 1, 30);

        service.enqueueRenderStarted(
                6L,
                14L,
                renderJobId,
                occurredAt
        );

        ArgumentCaptor<NotificationOutbox> captor =
                ArgumentCaptor.forClass(NotificationOutbox.class);

        verify(repository).save(captor.capture());

        NotificationOutbox saved = captor.getValue();
        assertThat(saved.getEventKey())
                .isEqualTo("render:" + renderJobId + ":STARTED");
        assertThat(saved.getType())
                .isEqualTo(NotificationType.RENDER_STARTED);
        assertThat(saved.getStatus())
                .isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getAttemptCount()).isZero();
    }

    @Test
    @DisplayName("렌더 완료 알림의 동일 event key는 중복 저장하지 않는다")
    void deduplicateCompleted() {
        UUID renderJobId = UUID.randomUUID();
        String eventKey =
                "render:" + renderJobId + ":COMPLETED";

        when(repository.existsByEventKey(eventKey))
                .thenReturn(true);

        service.enqueueRenderCompleted(
                6L,
                14L,
                renderJobId,
                LocalDateTime.now()
        );

        verify(repository, never())
                .save(any(NotificationOutbox.class));
    }

    @Test
    @DisplayName("렌더 실패 알림을 FAILED 유형이 아닌 PENDING 전송 작업으로 저장한다")
    void enqueueFailed() {
        UUID renderJobId = UUID.randomUUID();

        service.enqueueRenderFailed(
                6L,
                14L,
                renderJobId,
                LocalDateTime.now()
        );

        ArgumentCaptor<NotificationOutbox> captor =
                ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getType())
                .isEqualTo(NotificationType.RENDER_FAILED);
        assertThat(captor.getValue().getStatus())
                .isEqualTo(NotificationStatus.PENDING);
    }
}