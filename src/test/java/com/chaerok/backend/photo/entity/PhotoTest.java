package com.chaerok.backend.photo.entity;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PhotoTest {

    private final FilmRoll filmRoll = mock(FilmRoll.class);

    @Test
    @DisplayName("사진을 만들면 업로드 중 상태와 입력값을 가진다")
    void createPhoto() {
        LocalDateTime takenAt = LocalDateTime.of(2026, 7, 29, 18, 0);

        Photo photo = Photo.create(
                filmRoll,
                1,
                "users/1/rolls/1/original/001.jpg",
                takenAt
        );

        assertThat(photo.getFilmRoll()).isSameAs(filmRoll);
        assertThat(photo.getSequence()).isEqualTo(1);
        assertThat(photo.getOriginalObjectKey())
                .isEqualTo("users/1/rolls/1/original/001.jpg");
        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.UPLOADING);
        assertThat(photo.getTakenAt()).isEqualTo(takenAt);
        assertThat(photo.getFilteredObjectKey()).isNull();
    }

    @Test
    @DisplayName("사진 순서는 1부터 24까지만 허용한다")
    void sequenceMustBeBetweenOneAndTwentyFour() {
        assertThatThrownBy(() -> createPhoto(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 이상 24 이하");

        assertThatThrownBy(() -> createPhoto(25))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 이상 24 이하");
    }

    @Test
    @DisplayName("사진은 업로드부터 필터 처리 완료까지 정상적으로 상태가 전환된다")
    void completePhotoLifecycle() {
        Photo photo = createPhoto(1);
        LocalDateTime uploadedAt = LocalDateTime.of(2026, 7, 29, 18, 1);
        LocalDateTime processedAt = LocalDateTime.of(2026, 7, 29, 18, 2);

        photo.markUploaded(uploadedAt);
        photo.markProcessing();
        photo.complete(
                "users/1/rolls/1/filtered/001.jpg",
                processedAt
        );

        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.COMPLETED);
        assertThat(photo.getUploadCompletedAt()).isEqualTo(uploadedAt);
        assertThat(photo.getFilteredObjectKey())
                .isEqualTo("users/1/rolls/1/filtered/001.jpg");
        assertThat(photo.getProcessedAt()).isEqualTo(processedAt);
    }

    @Test
    @DisplayName("업로드 완료 사진은 현상 직전에 순서를 다시 부여할 수 있다")
    void resequenceUploadedPhotoForDevelopment() {
        Photo photo = createPhoto(3);
        photo.markUploaded(LocalDateTime.now());

        photo.resequenceForDevelopment(2);

        assertThat(photo.getSequence()).isEqualTo(2);
    }

    @Test
    @DisplayName("업로드 중 사진은 현상용 순서 재배치를 할 수 없다")
    void uploadingPhotoCannotBeResequencedForDevelopment() {
        Photo photo = createPhoto(2);

        assertThatThrownBy(() ->
                photo.resequenceForDevelopment(1)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("사진 상태");
    }

    @Test
    @DisplayName("업로드가 끝나지 않은 사진은 필터 처리를 시작할 수 없다")
    void uploadingPhotoCannotStartProcessing() {
        Photo photo = createPhoto(1);

        assertThatThrownBy(photo::markProcessing)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("업로드가 완료되었거나 실패한 사진");
    }

    @Test
    @DisplayName("실패한 사진은 다시 처리할 수 있고 기존 오류가 지워진다")
    void retryFailedPhoto() {
        Photo photo = createPhoto(1);
        photo.markUploaded(LocalDateTime.now());
        photo.markProcessing();
        photo.fail("IMAGE_READ_FAILED", "이미지를 읽을 수 없음");

        photo.markProcessing();

        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.PROCESSING);
        assertThat(photo.getErrorCode()).isNull();
        assertThat(photo.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("처리 시작 결과는 업로드 완료 사진을 PROCESSING으로 바꾸고 중복 적용할 수 있다")
    void markProcessingFromResultIsIdempotent() {
        Photo photo = createPhoto(1);
        photo.markUploaded(LocalDateTime.now());

        photo.markProcessingFromResult();
        photo.markProcessingFromResult();

        assertThat(photo.getStatus())
                .isEqualTo(PhotoStatus.PROCESSING);
    }

    @Test
    @DisplayName("렌더링 최종 실패 후 사진은 원본 업로드 완료 상태로 복구한다")
    void resetAfterRenderFailureForRetry() {
        Photo photo = createPhoto(1);
        photo.markUploaded(LocalDateTime.now());
        photo.markProcessingFromResult();

        photo.resetAfterRenderFailure();

        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.UPLOADED);
        assertThat(photo.getFilteredObjectKey()).isNull();
        assertThat(photo.getProcessedAt()).isNull();
    }

    @Test
    @DisplayName("완료된 사진은 만료 상태로 전환할 수 있다")
    void expireCompletedPhoto() {
        Photo photo = createPhoto(1);
        photo.markUploaded(LocalDateTime.now());
        photo.markProcessing();
        photo.complete("filtered/001.jpg", LocalDateTime.now());

        photo.expire();

        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.EXPIRED);
    }

    private Photo createPhoto(int sequence) {
        return Photo.create(
                filmRoll,
                sequence,
                "users/1/rolls/1/original/%03d.jpg".formatted(sequence),
                LocalDateTime.of(2026, 7, 29, 18, 0)
        );
    }

    @Test
    @DisplayName("업로드 완료 상태에서 결과 메시지로 바로 사진 처리를 완료한다")
    void completeFromResultAfterUpload() {
        Photo photo = createPhoto(1);
        photo.markUploaded(LocalDateTime.now());

        LocalDateTime processedAt =
                LocalDateTime.of(2026, 8, 5, 3, 42, 31);

        photo.completeFromResult(
                "filtered/001.jpg",
                processedAt
        );

        assertThat(photo.getStatus())
                .isEqualTo(PhotoStatus.COMPLETED);
        assertThat(photo.getFilteredObjectKey())
                .isEqualTo("filtered/001.jpg");
        assertThat(photo.getProcessedAt()).isEqualTo(processedAt);
    }
}
