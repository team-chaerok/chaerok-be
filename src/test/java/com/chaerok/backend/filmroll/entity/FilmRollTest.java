package com.chaerok.backend.filmroll.entity;

import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FilmRollTest {

    private final User user = mock(User.class);
    private final Region region = mock(Region.class);

    @Test
    @DisplayName("필름 롤을 만들면 촬영 중 상태와 초기값을 가진다")
    void createFilmRoll() {
        FilmRoll filmRoll = newFilmRoll();

        assertThat(filmRoll.getUser()).isSameAs(user);
        assertThat(filmRoll.getRegion()).isSameAs(region);
        assertThat(filmRoll.getFilterId()).isEqualTo("gongju");
        assertThat(filmRoll.getFilterStrength()).isEqualTo(0.8);
        assertThat(filmRoll.getFilterVersion()).isEqualTo(1);
        assertThat(filmRoll.getStatus()).isEqualTo(FilmRollStatus.CAPTURING);
        assertThat(filmRoll.getTotalPhotoCount()).isZero();
        assertThat(filmRoll.getProcessedPhotoCount()).isZero();
    }

    @Test
    @DisplayName("필름 롤에는 사진을 최대 24장까지만 추가할 수 있다")
    void cannotAddMoreThanTwentyFourPhotos() {
        FilmRoll filmRoll = newFilmRoll();

        for (int i = 0; i < FilmRoll.MAX_PHOTO_COUNT; i++) {
            filmRoll.increasePhotoCount();
        }

        assertThat(filmRoll.getTotalPhotoCount())
                .isEqualTo(FilmRoll.MAX_PHOTO_COUNT);

        assertThatThrownBy(filmRoll::increasePhotoCount)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최대 24장");
    }

    @Test
    @DisplayName("사진이 없는 필름 롤은 현상 준비 상태로 바꿀 수 없다")
    void emptyFilmRollCannotBeReady() {
        FilmRoll filmRoll = newFilmRoll();

        assertThatThrownBy(filmRoll::markReady)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("사진이 없는 필름 롤");
    }

    @Test
    @DisplayName("필름 롤은 촬영부터 현상 완료까지 정상적으로 상태가 전환된다")
    void completeFilmRollLifecycle() {
        FilmRoll filmRoll = newFilmRoll();
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 29, 20, 0);
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 29, 20, 10);

        filmRoll.increasePhotoCount();
        filmRoll.markReady();
        filmRoll.markQueued(requestedAt);
        filmRoll.markProcessing();
        filmRoll.updateProcessedPhotoCount(1);
        filmRoll.complete(
                "users/1/rolls/1/export/chaerok.zip",
                "users/1/rolls/1/export/chaerok.mp4",
                completedAt
        );

        assertThat(filmRoll.getStatus()).isEqualTo(FilmRollStatus.COMPLETED);
        assertThat(filmRoll.getRequestedAt()).isEqualTo(requestedAt);
        assertThat(filmRoll.getProcessedPhotoCount()).isEqualTo(1);
        assertThat(filmRoll.getZipObjectKey())
                .isEqualTo("users/1/rolls/1/export/chaerok.zip");
        assertThat(filmRoll.getReelObjectKey())
                .isEqualTo("users/1/rolls/1/export/chaerok.mp4");
        assertThat(filmRoll.getCompletedAt()).isEqualTo(completedAt);
        assertThat(filmRoll.getExpiresAt())
                .isEqualTo(completedAt.plusHours(FilmRoll.RESULT_RETENTION_HOURS));
    }

    @Test
    @DisplayName("처리 완료 사진 수는 전체 사진 수를 넘을 수 없다")
    void processedPhotoCountCannotExceedTotalCount() {
        FilmRoll filmRoll = newFilmRoll();
        filmRoll.increasePhotoCount();
        filmRoll.markReady();
        filmRoll.markQueued(LocalDateTime.now());
        filmRoll.markProcessing();

        assertThatThrownBy(() -> filmRoll.updateProcessedPhotoCount(2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("처리 완료 사진 수");
    }

    @Test
    @DisplayName("실패한 필름 롤은 오류를 지우고 현상 준비 상태로 재시도할 수 있다")
    void prepareRetryAfterFailure() {
        FilmRoll filmRoll = newFilmRoll();
        filmRoll.increasePhotoCount();
        filmRoll.markReady();
        filmRoll.markQueued(LocalDateTime.now());
        filmRoll.markProcessing();
        filmRoll.fail("FILTER_FAILED", "필터 처리 실패");

        filmRoll.prepareRetry();

        assertThat(filmRoll.getStatus()).isEqualTo(FilmRollStatus.READY);
        assertThat(filmRoll.getProcessedPhotoCount()).isZero();
        assertThat(filmRoll.getErrorCode()).isNull();
        assertThat(filmRoll.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("완료된 필름 롤은 만료 상태로 전환할 수 있다")
    void expireCompletedFilmRoll() {
        FilmRoll filmRoll = newFilmRoll();
        filmRoll.increasePhotoCount();
        filmRoll.markReady();
        filmRoll.markQueued(LocalDateTime.now());
        filmRoll.markProcessing();
        filmRoll.complete("result.zip", "result.mp4", LocalDateTime.now());

        filmRoll.expire();

        assertThat(filmRoll.getStatus()).isEqualTo(FilmRollStatus.EXPIRED);
    }

    @Test
    @DisplayName("잘못된 상태에서는 필름 롤 처리를 시작할 수 없다")
    void cannotStartProcessingFromCapturing() {
        FilmRoll filmRoll = newFilmRoll();

        assertThatThrownBy(filmRoll::markProcessing)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected=QUEUED")
                .hasMessageContaining("actual=CAPTURING");
    }

    private FilmRoll newFilmRoll() {
        return FilmRoll.create(
                user,
                region,
                " gongju ",
                0.8,
                1
        );
    }

    @Test
    @DisplayName("큐 상태 기록보다 결과가 먼저 도착해도 필름 롤 완료 상태를 반영한다")
    void completeFromResultBeforeQueuedStateIsRecorded() {
        FilmRoll filmRoll = newFilmRoll();
        filmRoll.increasePhotoCount();
        filmRoll.markReady();

        LocalDateTime completedAt =
                LocalDateTime.of(2026, 8, 5, 3, 42, 31);

        filmRoll.completeFromResult(
                "result.zip",
                "result.mp4",
                completedAt
        );

        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.COMPLETED);
        assertThat(filmRoll.getProcessedPhotoCount()).isEqualTo(1);
        assertThat(filmRoll.getCompletedAt()).isEqualTo(completedAt);
    }
}
