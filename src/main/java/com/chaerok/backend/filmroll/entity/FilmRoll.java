package com.chaerok.backend.filmroll.entity;

import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "film_rolls",
        indexes = {
                @Index(
                        name = "idx_film_rolls_user_status",
                        columnList = "user_id,status"
                ),
                @Index(
                        name = "idx_film_rolls_region_status",
                        columnList = "region_id,status"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FilmRoll {

    public static final int MAX_PHOTO_COUNT = 24;
    public static final int RESULT_RETENTION_HOURS = 48;
    public static final int DEVELOPMENT_DELAY_HOURS = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "filter_id", nullable = false, length = 100)
    private String filterId;

    @Column(name = "filter_strength", nullable = false)
    private double filterStrength;

    @Column(name = "filter_version", nullable = false)
    private int filterVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FilmRollStatus status;

    @Column(name = "total_photo_count", nullable = false)
    private int totalPhotoCount;

    @Column(name = "processed_photo_count", nullable = false)
    private int processedPhotoCount;

    @Column(name = "zip_object_key")
    private String zipObjectKey;

    @Column(name = "reel_object_key")
    private String reelObjectKey;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "exited_at")
    private LocalDateTime exitedAt;

    @Column(name = "develop_available_at")
    private LocalDateTime developAvailableAt;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static FilmRoll create(
            User user,
            Region region,
            String filterId,
            double filterStrength,
            int filterVersion
    ) {
        validateCreateArguments(
                user,
                region,
                filterId,
                filterStrength,
                filterVersion
        );

        FilmRoll filmRoll = new FilmRoll();
        filmRoll.user = user;
        filmRoll.region = region;
        filmRoll.filterId = filterId.trim();
        filmRoll.filterStrength = filterStrength;
        filmRoll.filterVersion = filterVersion;
        filmRoll.status = FilmRollStatus.CAPTURING;
        filmRoll.totalPhotoCount = 0;
        filmRoll.processedPhotoCount = 0;
        return filmRoll;
    }

    public void increasePhotoCount() {
        requireStatus(FilmRollStatus.CAPTURING);

        if (totalPhotoCount >= MAX_PHOTO_COUNT) {
            throw new IllegalStateException(
                    "필름 롤에는 최대 24장까지만 저장할 수 있습니다."
            );
        }

        totalPhotoCount++;
    }

    public void decreasePhotoCount() {
        requireStatus(FilmRollStatus.CAPTURING);

        if (totalPhotoCount <= 0) {
            throw new IllegalStateException(
                    "필름 롤의 사진 수는 0보다 작아질 수 없습니다."
            );
        }

        totalPhotoCount--;
    }


    public void confirmExit(LocalDateTime exitedAt) {
        requireStatus(FilmRollStatus.CAPTURING);

        if (exitedAt == null) {
            throw new IllegalArgumentException(
                    "지역 이탈 확정 시각은 필수입니다."
            );
        }

        if (this.exitedAt != null) {
            throw new IllegalStateException(
                    "이미 지역 이탈이 확정된 필름 롤입니다."
            );
        }

        this.exitedAt = exitedAt;
        this.developAvailableAt =
                exitedAt.plusHours(DEVELOPMENT_DELAY_HOURS);
    }

    public boolean isExitConfirmed() {
        return exitedAt != null;
    }

    public boolean isDevelopmentAvailable(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException(
                    "현상 가능 여부 확인 시각은 필수입니다."
            );
        }

        return isExitConfirmed()
                && developAvailableAt != null
                && status != FilmRollStatus.EXPIRED
                && !now.isBefore(developAvailableAt);
    }

    public void expireAfterExit() {
        requireStatus(FilmRollStatus.CAPTURING);

        if (!isExitConfirmed()) {
            throw new IllegalStateException(
                    "지역 이탈이 확정된 필름 롤만 종료할 수 있습니다."
            );
        }

        status = FilmRollStatus.EXPIRED;
        developAvailableAt = null;
    }

    public void markReady() {
        requireStatus(FilmRollStatus.CAPTURING);

        if (totalPhotoCount == 0) {
            throw new IllegalStateException(
                    "사진이 없는 필름 롤은 현상할 수 없습니다."
            );
        }

        status = FilmRollStatus.READY;
    }

    public void markQueued(LocalDateTime requestedAt) {
        requireStatus(FilmRollStatus.READY);

        if (requestedAt == null) {
            throw new IllegalArgumentException(
                    "현상 요청 시각은 필수입니다."
            );
        }

        status = FilmRollStatus.QUEUED;
        this.requestedAt = requestedAt;
        clearError();
    }

    public void markProcessing() {
        requireStatus(FilmRollStatus.QUEUED);
        status = FilmRollStatus.PROCESSING;
    }

    public void markProcessingFromResult() {
        if (status != FilmRollStatus.READY
                && status != FilmRollStatus.QUEUED
                && status != FilmRollStatus.PROCESSING) {
            throw new IllegalStateException(
                    "현재 필름 롤 상태에는 처리 시작 결과를 적용할 수 없습니다. status="
                            + status
            );
        }

        status = FilmRollStatus.PROCESSING;
        clearError();
    }

    public void updateProcessedPhotoCount(int processedPhotoCount) {
        requireStatus(FilmRollStatus.PROCESSING);

        if (processedPhotoCount < 0
                || processedPhotoCount > totalPhotoCount) {
            throw new IllegalArgumentException(
                    "처리 완료 사진 수가 올바르지 않습니다."
            );
        }

        this.processedPhotoCount = processedPhotoCount;
    }

    public void complete(
            String zipObjectKey,
            String reelObjectKey,
            LocalDateTime completedAt
    ) {
        requireStatus(FilmRollStatus.PROCESSING);
        requireText(zipObjectKey, "ZIP S3 객체 키");
        requireText(reelObjectKey, "릴스 S3 객체 키");

        if (completedAt == null) {
            throw new IllegalArgumentException(
                    "현상 완료 시각은 필수입니다."
            );
        }

        this.status = FilmRollStatus.COMPLETED;
        this.processedPhotoCount = totalPhotoCount;
        this.zipObjectKey = zipObjectKey;
        this.reelObjectKey = reelObjectKey;
        this.completedAt = completedAt;
        this.expiresAt = completedAt.plusHours(RESULT_RETENTION_HOURS);
        clearError();
    }

    public void completeFromResult(
            String zipObjectKey,
            String reelObjectKey,
            LocalDateTime completedAt
    ) {
        if (status != FilmRollStatus.READY
                && status != FilmRollStatus.QUEUED
                && status != FilmRollStatus.PROCESSING) {
            throw new IllegalStateException(
                    "현재 필름 롤 상태에는 완료 결과를 적용할 수 없습니다. status="
                            + status
            );
        }

        requireText(zipObjectKey, "ZIP S3 객체 키");
        requireText(reelObjectKey, "릴스 S3 객체 키");

        if (completedAt == null) {
            throw new IllegalArgumentException(
                    "현상 완료 시각은 필수입니다."
            );
        }

        this.status = FilmRollStatus.COMPLETED;
        this.processedPhotoCount = totalPhotoCount;
        this.zipObjectKey = zipObjectKey;
        this.reelObjectKey = reelObjectKey;
        this.completedAt = completedAt;
        this.expiresAt = completedAt.plusHours(RESULT_RETENTION_HOURS);
        clearError();
    }

    public void failFromResult(
            String errorCode,
            String errorMessage
    ) {
        if (status != FilmRollStatus.READY
                && status != FilmRollStatus.QUEUED
                && status != FilmRollStatus.PROCESSING) {
            throw new IllegalStateException(
                    "현재 필름 롤 상태에는 실패 결과를 적용할 수 없습니다. status="
                            + status
            );
        }

        fail(errorCode, errorMessage);
    }

    public void fail(String errorCode, String errorMessage) {
        requireText(errorCode, "오류 코드");
        requireText(errorMessage, "오류 메시지");

        this.status = FilmRollStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public void prepareRetry() {
        requireStatus(FilmRollStatus.FAILED);

        this.status = FilmRollStatus.READY;
        this.processedPhotoCount = 0;
        this.zipObjectKey = null;
        this.reelObjectKey = null;
        this.completedAt = null;
        this.expiresAt = null;
        clearError();
    }

    public void expire() {
        requireStatus(FilmRollStatus.COMPLETED);
        this.status = FilmRollStatus.EXPIRED;
    }

    private void requireStatus(FilmRollStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "필름 롤 상태가 올바르지 않습니다. expected="
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

    private static void validateCreateArguments(
            User user,
            Region region,
            String filterId,
            double filterStrength,
            int filterVersion
    ) {
        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다.");
        }

        if (region == null) {
            throw new IllegalArgumentException("지역은 필수입니다.");
        }

        requireText(filterId, "필터 ID");

        if (!Double.isFinite(filterStrength)
                || filterStrength < 0.0
                || filterStrength > 1.0) {
            throw new IllegalArgumentException(
                    "필터 강도는 0.0 이상 1.0 이하여야 합니다."
            );
        }

        if (filterVersion < 1) {
            throw new IllegalArgumentException(
                    "필터 버전은 1 이상이어야 합니다."
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
