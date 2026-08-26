package com.chaerok.backend.notification.message;

import com.chaerok.backend.notification.outbox.entity.NotificationOutbox;
import com.chaerok.backend.notification.outbox.entity.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPayloadFactoryTest {

    private final NotificationPayloadFactory factory =
            new NotificationPayloadFactory();

    @Test
    @DisplayName("현상 완료 알림에는 필름롤 이동용 data를 포함한다")
    void completedPayload() {
        UUID renderJobId = UUID.randomUUID();
        NotificationOutbox outbox =
                NotificationOutbox.pending(
                        "render:" + renderJobId + ":COMPLETED",
                        6L,
                        14L,
                        renderJobId,
                        NotificationType.RENDER_COMPLETED,
                        LocalDateTime.now()
                );

        NotificationPayload payload =
                factory.from(outbox);

        assertThat(payload.title())
                .isEqualTo("필름 현상이 완료됐어요");
        assertThat(payload.collapseKey())
                .isEqualTo("chaerok-development");
        assertThat(payload.notificationTag())
                .isEqualTo(
                        "render:"
                                + renderJobId
                                + ":COMPLETED"
                );
        assertThat(payload.data())
                .containsEntry("filmRollId", "14")
                .containsEntry(
                        "renderJobId",
                        renderJobId.toString()
                )
                .containsEntry(
                        "notificationType",
                        "RENDER_COMPLETED"
                )
                .containsEntry(
                        "screen",
                        "filmRollResult"
                );
    }

    @Test
    @DisplayName("현상 시작과 실패는 서로 다른 사용자 문구를 만든다")
    void startedAndFailedPayload() {
        NotificationPayload started =
                factory.from(
                        outbox(NotificationType.RENDER_STARTED)
                );
        NotificationPayload failed =
                factory.from(
                        outbox(NotificationType.RENDER_FAILED)
                );

        assertThat(started.title())
                .contains("시작");
        assertThat(failed.title())
                .contains("문제");
    }

    private NotificationOutbox outbox(
            NotificationType type
    ) {
        UUID renderJobId = UUID.randomUUID();
        return NotificationOutbox.pending(
                "render:"
                        + renderJobId
                        + ":"
                        + type.name(),
                6L,
                14L,
                renderJobId,
                type,
                LocalDateTime.now()
        );
    }
}