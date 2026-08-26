package com.chaerok.backend.notification.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "notification_outbox",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_outbox_event_key",
                        columnNames = "event_key"
                )
        },
        indexes = {
                @Index(
                        name = "idx_notification_outbox_pending",
                        columnList = "status,next_attempt_at,created_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationOutbox {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "film_roll_id", nullable = false)
    private Long filmRollId;

    @Column(name = "render_job_id", nullable = false)
    private UUID renderJobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static NotificationOutbox pending(
            String eventKey,
            Long userId,
            Long filmRollId,
            UUID renderJobId,
            NotificationType type,
            LocalDateTime occurredAt
    ) {
        requireText(eventKey, "알림 이벤트 키");
        requirePositive(userId, "사용자 ID");
        requirePositive(filmRollId, "필름 롤 ID");

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
        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "알림 발생 시각은 필수입니다."
            );
        }

        NotificationOutbox outbox = new NotificationOutbox();
        outbox.eventKey = eventKey;
        outbox.userId = userId;
        outbox.filmRollId = filmRollId;
        outbox.renderJobId = renderJobId;
        outbox.type = type;
        outbox.status = NotificationStatus.PENDING;
        outbox.attemptCount = 0;
        outbox.occurredAt = occurredAt;
        return outbox;
    }

    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    public void markSent(LocalDateTime sentAt) {
        requireTime(sentAt, "알림 전송 완료 시각");

        this.status = NotificationStatus.SENT;
        this.sentAt = sentAt;
        this.nextAttemptAt = null;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void markRetry(
            LocalDateTime failedAt,
            String errorCode,
            String errorMessage
    ) {
        requireTime(failedAt, "알림 전송 실패 시각");

        this.attemptCount = this.attemptCount + 1;
        this.lastErrorCode = normalizeErrorCode(errorCode);
        this.lastErrorMessage = errorMessage;
        this.sentAt = null;

        if (this.attemptCount >= MAX_ATTEMPTS) {
            this.status = NotificationStatus.FAILED;
            this.nextAttemptAt = null;
            return;
        }

        this.status = NotificationStatus.PENDING;
        this.nextAttemptAt =
                failedAt.plusSeconds(retryDelaySeconds(this.attemptCount));
    }

    public void markFailed(
            LocalDateTime failedAt,
            String errorCode,
            String errorMessage
    ) {
        requireTime(failedAt, "알림 전송 실패 시각");

        this.attemptCount = this.attemptCount + 1;
        this.status = NotificationStatus.FAILED;
        this.nextAttemptAt = null;
        this.sentAt = null;
        this.lastErrorCode = normalizeErrorCode(errorCode);
        this.lastErrorMessage = errorMessage;
    }

    private static long retryDelaySeconds(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> 30L;
            case 2 -> 120L;
            case 3 -> 600L;
            default -> 1_800L;
        };
    }

    private static String normalizeErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return null;
        }

        String normalized = errorCode.trim();
        if (normalized.length() <= 100) {
            return normalized;
        }
        return normalized.substring(0, 100);
    }

    private static void requireTime(
            LocalDateTime value,
            String fieldName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 필수입니다."
            );
        }
    }

    private static void requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 필수입니다."
            );
        }
    }

    private static void requirePositive(
            Long value,
            String fieldName
    ) {
        if (value == null || value < 1L) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 1 이상이어야 합니다."
            );
        }
    }
}