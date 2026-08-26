package com.chaerok.backend.notification.outbox.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationOutboxTest {

    @Test
    @DisplayName("전송 성공 시 SENT로 전환하고 재시도 정보를 제거한다")
    void markSent() {
        NotificationOutbox outbox = pending();
        LocalDateTime failedAt =
                LocalDateTime.of(2026, 8, 27, 2, 0);

        outbox.markRetry(
                failedAt,
                "UNAVAILABLE",
                "temporary"
        );

        LocalDateTime sentAt =
                failedAt.plusMinutes(1);
        outbox.markSent(sentAt);

        assertThat(outbox.getStatus())
                .isEqualTo(NotificationStatus.SENT);
        assertThat(outbox.getSentAt())
                .isEqualTo(sentAt);
        assertThat(outbox.getNextAttemptAt()).isNull();
        assertThat(outbox.getLastErrorCode()).isNull();
        assertThat(outbox.getLastErrorMessage()).isNull();
    }

    @Test
    @DisplayName("첫 일시 실패는 30초 뒤 재시도한다")
    void firstRetry() {
        NotificationOutbox outbox = pending();
        LocalDateTime failedAt =
                LocalDateTime.of(2026, 8, 27, 2, 0);

        outbox.markRetry(
                failedAt,
                "UNAVAILABLE",
                "temporary"
        );

        assertThat(outbox.getStatus())
                .isEqualTo(NotificationStatus.PENDING);
        assertThat(outbox.getAttemptCount()).isEqualTo(1);
        assertThat(outbox.getNextAttemptAt())
                .isEqualTo(failedAt.plusSeconds(30));
    }

    @Test
    @DisplayName("일시 실패가 최대 횟수에 도달하면 FAILED가 된다")
    void retryLimit() {
        NotificationOutbox outbox = pending();
        LocalDateTime now =
                LocalDateTime.of(2026, 8, 27, 2, 0);

        for (int attempt = 0;
             attempt < NotificationOutbox.MAX_ATTEMPTS;
             attempt++) {
            outbox.markRetry(
                    now.plusMinutes(attempt),
                    "UNAVAILABLE",
                    "temporary"
            );
        }

        assertThat(outbox.getStatus())
                .isEqualTo(NotificationStatus.FAILED);
        assertThat(outbox.getAttemptCount())
                .isEqualTo(NotificationOutbox.MAX_ATTEMPTS);
        assertThat(outbox.getNextAttemptAt()).isNull();
    }

    private NotificationOutbox pending() {
        return NotificationOutbox.pending(
                "render:test:COMPLETED",
                6L,
                14L,
                UUID.randomUUID(),
                NotificationType.RENDER_COMPLETED,
                LocalDateTime.of(2026, 8, 27, 1, 59)
        );
    }
}