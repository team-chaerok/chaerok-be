package com.chaerok.backend.render.entity;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RenderJobTest {

    private final FilmRoll filmRoll = mock(FilmRoll.class);

    @Test
    @DisplayName("렌더링 작업을 만들면 생성 상태와 고유 ID를 가진다")
    void createRenderJob() {
        RenderJob renderJob = RenderJob.create(filmRoll);

        assertThat(renderJob.getId()).isNotNull();
        assertThat(renderJob.getFilmRoll()).isSameAs(filmRoll);
        assertThat(renderJob.getStatus()).isEqualTo(RenderJobStatus.CREATED);
        assertThat(renderJob.getAttemptCount()).isZero();
    }

    @Test
    @DisplayName("렌더링 작업은 큐 등록부터 완료까지 정상적으로 상태가 전환된다")
    void completeRenderJobLifecycle() {
        RenderJob renderJob = RenderJob.create(filmRoll);
        LocalDateTime queuedAt = LocalDateTime.of(2026, 7, 29, 19, 0);
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 29, 19, 1);
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 29, 19, 5);

        renderJob.markQueued(queuedAt);
        renderJob.markProcessing(startedAt);
        renderJob.complete(completedAt);

        assertThat(renderJob.getStatus()).isEqualTo(RenderJobStatus.COMPLETED);
        assertThat(renderJob.getQueuedAt()).isEqualTo(queuedAt);
        assertThat(renderJob.getStartedAt()).isEqualTo(startedAt);
        assertThat(renderJob.getCompletedAt()).isEqualTo(completedAt);
        assertThat(renderJob.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("큐 전송 실패 작업은 오류를 지우고 다시 큐에 등록할 수 있다")
    void retryQueueFailedJob() {
        RenderJob renderJob = RenderJob.create(filmRoll);
        renderJob.queueFailed("SQS_SEND_FAILED", "SQS 전송 실패");

        renderJob.markQueued(LocalDateTime.now());

        assertThat(renderJob.getStatus()).isEqualTo(RenderJobStatus.QUEUED);
        assertThat(renderJob.getErrorCode()).isNull();
        assertThat(renderJob.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("작업 시작 횟수는 실제 처리 시작 시 증가한다")
    void attemptCountIncreasesWhenProcessingStarts() {
        RenderJob renderJob = RenderJob.create(filmRoll);
        renderJob.markQueued(LocalDateTime.now());

        renderJob.markProcessing(LocalDateTime.now());

        assertThat(renderJob.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("큐에 등록되지 않은 작업은 처리를 시작할 수 없다")
    void createdJobCannotStartProcessing() {
        RenderJob renderJob = RenderJob.create(filmRoll);

        assertThatThrownBy(() -> renderJob.markProcessing(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected=QUEUED")
                .hasMessageContaining("actual=CREATED");
    }

    @Test
    @DisplayName("처리 중 작업을 실패 처리하면 실패 정보가 저장된다")
    void failProcessingJob() {
        RenderJob renderJob = RenderJob.create(filmRoll);
        renderJob.markQueued(LocalDateTime.now());
        renderJob.markProcessing(LocalDateTime.now());

        renderJob.fail("RENDER_FAILED", "렌더링 실패");

        assertThat(renderJob.getStatus()).isEqualTo(RenderJobStatus.FAILED);
        assertThat(renderJob.getErrorCode()).isEqualTo("RENDER_FAILED");
        assertThat(renderJob.getErrorMessage()).isEqualTo("렌더링 실패");
    }
}
