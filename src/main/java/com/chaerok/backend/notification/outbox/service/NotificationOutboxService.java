package com.chaerok.backend.notification.outbox.service;

import com.chaerok.backend.notification.outbox.entity.NotificationOutbox;
import com.chaerok.backend.notification.outbox.entity.NotificationType;
import com.chaerok.backend.notification.outbox.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private final NotificationOutboxRepository notificationOutboxRepository;

    @Transactional
    public void enqueueRenderStarted(
            Long userId,
            Long filmRollId,
            UUID renderJobId,
            LocalDateTime occurredAt
    ) {
        enqueue(
                userId,
                filmRollId,
                renderJobId,
                NotificationType.RENDER_STARTED,
                occurredAt
        );
    }

    @Transactional
    public void enqueueRenderCompleted(
            Long userId,
            Long filmRollId,
            UUID renderJobId,
            LocalDateTime occurredAt
    ) {
        enqueue(
                userId,
                filmRollId,
                renderJobId,
                NotificationType.RENDER_COMPLETED,
                occurredAt
        );
    }

    @Transactional
    public void enqueueRenderFailed(
            Long userId,
            Long filmRollId,
            UUID renderJobId,
            LocalDateTime occurredAt
    ) {
        enqueue(
                userId,
                filmRollId,
                renderJobId,
                NotificationType.RENDER_FAILED,
                occurredAt
        );
    }

    private void enqueue(
            Long userId,
            Long filmRollId,
            UUID renderJobId,
            NotificationType type,
            LocalDateTime occurredAt
    ) {
        String eventKey = eventKey(renderJobId, type);

        if (notificationOutboxRepository.existsByEventKey(eventKey)) {
            return;
        }

        notificationOutboxRepository.save(
                NotificationOutbox.pending(
                        eventKey,
                        userId,
                        filmRollId,
                        renderJobId,
                        type,
                        occurredAt
                )
        );
    }

    static String eventKey(
            UUID renderJobId,
            NotificationType type
    ) {
        if (renderJobId == null) {
            throw new IllegalArgumentException(
                    "렌더링 작업 ID는 필수입니다."
            );
        }
        if (type == null) {
            throw new IllegalArgumentException(
                    "알림 유형은 필수입니다."
            );
        }

        String event = switch (type) {
            case RENDER_STARTED -> "STARTED";
            case RENDER_COMPLETED -> "COMPLETED";
            case RENDER_FAILED -> "FAILED";
        };

        return "render:"
                + renderJobId
                + ":"
                + event;
    }
}