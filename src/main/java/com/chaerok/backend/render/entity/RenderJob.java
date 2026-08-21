package com.chaerok.backend.render.entity;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import jakarta.persistence.*;
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
        name = "render_jobs",
        indexes = {
                @Index(
                        name = "idx_render_jobs_film_roll_created_at",
                        columnList = "film_roll_id,created_at"
                ),
                @Index(
                        name = "idx_render_jobs_status",
                        columnList = "status"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RenderJob {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_roll_id", nullable = false)
    private FilmRoll filmRoll;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RenderJobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "request_message_id", length = 100)
    private String requestMessageId;

    @Column(name = "result_message_id", length = 100)
    private String resultMessageId;

    @Column(name = "result_bucket", length = 255)
    private String resultBucket;

    @Column(name = "zip_object_key")
    private String zipObjectKey;

    @Column(name = "zip_file_size")
    private Long zipFileSize;

    @Column(name = "reel_object_key")
    private String reelObjectKey;

    @Column(name = "reel_file_size")
    private Long reelFileSize;

    @Column(name = "manifest_object_key")
    private String manifestObjectKey;

    @Column(name = "result_occurred_at")
    private LocalDateTime resultOccurredAt;

    @Column(name = "queued_at")
    private LocalDateTime queuedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static RenderJob create(FilmRoll filmRoll) {
        if (filmRoll == null) {
            throw new IllegalArgumentException(
                    "필름 롤은 필수입니다."
            );
        }

        RenderJob renderJob = new RenderJob();
        renderJob.id = UUID.randomUUID();
        renderJob.filmRoll = filmRoll;
        renderJob.status = RenderJobStatus.CREATED;
        renderJob.attemptCount = 0;
        return renderJob;
    }

    public void markQueued(LocalDateTime queuedAt) {
        if (status != RenderJobStatus.CREATED
                && status != RenderJobStatus.QUEUE_FAILED) {
            throw new IllegalStateException(
                    "생성됐거나 큐 전송에 실패한 작업만 큐에 등록할 수 있습니다."
            );
        }

        if (queuedAt == null) {
            throw new IllegalArgumentException(
                    "큐 등록 시각은 필수입니다."
            );
        }

        this.status = RenderJobStatus.QUEUED;
        this.queuedAt = queuedAt;
        clearError();
    }

    public void markQueued(
            LocalDateTime queuedAt,
            String requestMessageId
    ) {
        requireText(requestMessageId, "요청 메시지 ID");
        markQueued(queuedAt);
        this.requestMessageId = requestMessageId;
    }

    public void markProcessing(LocalDateTime startedAt) {
        requireStatus(RenderJobStatus.QUEUED);

        if (startedAt == null) {
            throw new IllegalArgumentException(
                    "작업 시작 시각은 필수입니다."
            );
        }

        this.status = RenderJobStatus.PROCESSING;
        this.startedAt = startedAt;
        this.attemptCount++;
        clearError();
    }

    public void markProcessingFromResult(
            int attempt,
            String requestMessageId,
            LocalDateTime startedAt
    ) {
        if (status != RenderJobStatus.CREATED
                && status != RenderJobStatus.QUEUED
                && status != RenderJobStatus.PROCESSING
                && status != RenderJobStatus.QUEUE_FAILED
                && status != RenderJobStatus.COMPLETED
                && status != RenderJobStatus.FAILED) {
            throw new IllegalStateException(
                    "현재 렌더링 작업 상태에는 처리 시작 결과를 적용할 수 없습니다. status="
                            + status
            );
        }

        requirePositiveAttempt(attempt);
        requireText(requestMessageId, "요청 메시지 ID");

        if (startedAt == null) {
            throw new IllegalArgumentException(
                    "작업 시작 시각은 필수입니다."
            );
        }

        this.attemptCount = Math.max(this.attemptCount, attempt);
        this.requestMessageId = requestMessageId;

        if (this.startedAt == null
                || startedAt.isBefore(this.startedAt)) {
            this.startedAt = startedAt;
        }

        if (status != RenderJobStatus.COMPLETED
                && status != RenderJobStatus.FAILED) {
            this.status = RenderJobStatus.PROCESSING;
            clearError();
        }
    }

    public void complete(LocalDateTime completedAt) {
        requireStatus(RenderJobStatus.PROCESSING);

        if (completedAt == null) {
            throw new IllegalArgumentException(
                    "작업 완료 시각은 필수입니다."
            );
        }

        this.status = RenderJobStatus.COMPLETED;
        this.completedAt = completedAt;
        clearError();
    }

    public void completeFromResult(
            int attempt,
            String requestMessageId,
            String resultMessageId,
            String resultBucket,
            String zipObjectKey,
            Long zipFileSize,
            String reelObjectKey,
            Long reelFileSize,
            String manifestObjectKey,
            LocalDateTime occurredAt
    ) {
        requireResultApplicableStatus();
        requirePositiveAttempt(attempt);
        requireText(requestMessageId, "요청 메시지 ID");
        requireText(resultMessageId, "결과 메시지 ID");
        requireText(resultBucket, "결과 S3 버킷");
        requireText(zipObjectKey, "ZIP S3 객체 키");
        requireNonNegative(zipFileSize, "ZIP 파일 크기");
        requireText(reelObjectKey, "릴스 S3 객체 키");
        requireNonNegative(reelFileSize, "릴스 파일 크기");
        requireText(manifestObjectKey, "manifest S3 객체 키");

        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "결과 발생 시각은 필수입니다."
            );
        }

        this.status = RenderJobStatus.COMPLETED;
        this.attemptCount = Math.max(this.attemptCount, attempt);
        this.requestMessageId = requestMessageId;
        this.resultMessageId = resultMessageId;
        this.resultBucket = resultBucket;
        this.zipObjectKey = zipObjectKey;
        this.zipFileSize = zipFileSize;
        this.reelObjectKey = reelObjectKey;
        this.reelFileSize = reelFileSize;
        this.manifestObjectKey = manifestObjectKey;
        this.resultOccurredAt = occurredAt;
        this.completedAt = occurredAt;

        if (this.startedAt == null) {
            this.startedAt = occurredAt;
        }

        clearError();
    }

    public void failFromResult(
            int attempt,
            String requestMessageId,
            String resultMessageId,
            String resultBucket,
            String errorCode,
            String errorMessage,
            LocalDateTime occurredAt
    ) {
        requireResultApplicableStatus();
        requirePositiveAttempt(attempt);
        requireText(requestMessageId, "요청 메시지 ID");
        requireText(resultMessageId, "결과 메시지 ID");
        requireText(resultBucket, "결과 S3 버킷");
        requireText(errorCode, "오류 코드");
        requireText(errorMessage, "오류 메시지");

        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "결과 발생 시각은 필수입니다."
            );
        }

        this.status = RenderJobStatus.FAILED;
        this.attemptCount = Math.max(this.attemptCount, attempt);
        this.requestMessageId = requestMessageId;
        this.resultMessageId = resultMessageId;
        this.resultBucket = resultBucket;
        this.resultOccurredAt = occurredAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;

        if (this.startedAt == null) {
            this.startedAt = occurredAt;
        }
    }

    public void queueFailed(String errorCode, String errorMessage) {
        if (status != RenderJobStatus.CREATED
                && status != RenderJobStatus.QUEUED) {
            throw new IllegalStateException(
                    "큐 등록 전후 상태에서만 큐 전송 실패 처리할 수 있습니다."
            );
        }

        setFailure(
                RenderJobStatus.QUEUE_FAILED,
                errorCode,
                errorMessage
        );
    }

    public void fail(String errorCode, String errorMessage) {
        setFailure(
                RenderJobStatus.FAILED,
                errorCode,
                errorMessage
        );
    }

    private void setFailure(
            RenderJobStatus failureStatus,
            String errorCode,
            String errorMessage
    ) {
        requireText(errorCode, "오류 코드");
        requireText(errorMessage, "오류 메시지");

        this.status = failureStatus;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    private void requireResultApplicableStatus() {
        if (status != RenderJobStatus.CREATED
                && status != RenderJobStatus.QUEUED
                && status != RenderJobStatus.PROCESSING
                && status != RenderJobStatus.QUEUE_FAILED) {
            throw new IllegalStateException(
                    "현재 렌더링 작업 상태에는 결과를 적용할 수 없습니다. status="
                            + status
            );
        }
    }

    private void requireStatus(RenderJobStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "렌더링 작업 상태가 올바르지 않습니다. expected="
                            + expected
                            + ", actual="
                            + status
            );
        }
    }

    private void clearError() {
        this.errorCode = null;
        this.errorMessage = null;
    }

    private static void requirePositiveAttempt(int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException(
                    "렌더링 시도 횟수는 1 이상이어야 합니다."
            );
        }
    }

    private static void requireNonNegative(
            Long value,
            String fieldName
    ) {
        if (value == null || value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 0 이상이어야 합니다."
            );
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 필수입니다."
            );
        }
    }
}
