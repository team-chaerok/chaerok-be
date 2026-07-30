package com.chaerok.backend.photo.entity;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filter.analysis.SceneType;
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
        name = "photos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_photos_film_roll_sequence",
                        columnNames = {"film_roll_id", "sequence"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_photos_film_roll_status",
                        columnList = "film_roll_id,status"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_roll_id", nullable = false)
    private FilmRoll filmRoll;

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "original_object_key", nullable = false)
    private String originalObjectKey;

    @Column(name = "filtered_object_key")
    private String filteredObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PhotoStatus status;

    @Column(name = "has_face", nullable = false)
    private boolean hasFace;

    @Enumerated(EnumType.STRING)
    @Column(name = "scene_type", length = 30)
    private SceneType sceneType;

    @Column(name = "taken_at", nullable = false)
    private LocalDateTime takenAt;

    @Column(name = "upload_completed_at")
    private LocalDateTime uploadCompletedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Photo create(
            FilmRoll filmRoll,
            int sequence,
            String originalObjectKey,
            boolean hasFace,
            SceneType sceneType,
            LocalDateTime takenAt
    ) {
        if (filmRoll == null) {
            throw new IllegalArgumentException(
                    "필름 롤은 필수입니다."
            );
        }

        if (sequence < 1
                || sequence > FilmRoll.MAX_PHOTO_COUNT) {
            throw new IllegalArgumentException(
                    "사진 순서는 1 이상 24 이하여야 합니다."
            );
        }

        requireText(originalObjectKey, "원본 S3 객체 키");

        if (takenAt == null) {
            throw new IllegalArgumentException(
                    "촬영 시각은 필수입니다."
            );
        }

        Photo photo = new Photo();
        photo.filmRoll = filmRoll;
        photo.sequence = sequence;
        photo.originalObjectKey = originalObjectKey;
        photo.status = PhotoStatus.UPLOADING;
        photo.hasFace = hasFace;
        photo.sceneType = sceneType;
        photo.takenAt = takenAt;
        return photo;
    }

    public void markUploaded(LocalDateTime uploadCompletedAt) {
        requireStatus(PhotoStatus.UPLOADING);

        if (uploadCompletedAt == null) {
            throw new IllegalArgumentException(
                    "업로드 완료 시각은 필수입니다."
            );
        }

        this.status = PhotoStatus.UPLOADED;
        this.uploadCompletedAt = uploadCompletedAt;
        clearError();
    }

    public void markProcessing() {
        if (status != PhotoStatus.UPLOADED
                && status != PhotoStatus.FAILED) {
            throw new IllegalStateException(
                    "업로드가 완료되었거나 실패한 사진만 다시 처리할 수 있습니다."
            );
        }

        this.status = PhotoStatus.PROCESSING;
        clearError();
    }

    public void complete(
            String filteredObjectKey,
            LocalDateTime processedAt
    ) {
        requireStatus(PhotoStatus.PROCESSING);
        requireText(filteredObjectKey, "필터 결과 S3 객체 키");

        if (processedAt == null) {
            throw new IllegalArgumentException(
                    "사진 처리 완료 시각은 필수입니다."
            );
        }

        this.status = PhotoStatus.COMPLETED;
        this.filteredObjectKey = filteredObjectKey;
        this.processedAt = processedAt;
        clearError();
    }

    public void fail(String errorCode, String errorMessage) {
        requireText(errorCode, "오류 코드");
        requireText(errorMessage, "오류 메시지");

        this.status = PhotoStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public void expire() {
        requireStatus(PhotoStatus.COMPLETED);
        this.status = PhotoStatus.EXPIRED;
    }

    private void requireStatus(PhotoStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "사진 상태가 올바르지 않습니다. expected="
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

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 필수입니다."
            );
        }
    }
}
