package com.chaerok.backend.render.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.render.dto.RenderRequestResponse;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.entity.RenderJobStatus;
import com.chaerok.backend.render.queue.RenderQueuePublishResult;
import com.chaerok.backend.render.repository.RenderJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderJobStateServiceTest {

    @Mock
    private RenderJobRepository renderJobRepository;

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private RenderJob renderJob;

    @Mock
    private FilmRoll filmRoll;

    @Test
    @DisplayName("SQS 전송 직후 결과가 먼저 반영됐으면 QUEUED로 되돌리지 않는다")
    void keepTerminalStateWhenResultArrivesFirst() {
        UUID renderJobId = UUID.randomUUID();
        RenderQueuePublishResult publishResult =
                new RenderQueuePublishResult("request-message");

        when(renderJobRepository.findByIdForUpdate(renderJobId))
                .thenReturn(Optional.of(renderJob));
        when(filmRollRepository.findByIdAndUserIdForUpdate(2L, 3L))
                .thenReturn(Optional.of(filmRoll));
        when(renderJob.getStatus())
                .thenReturn(RenderJobStatus.COMPLETED);
        when(filmRoll.getStatus())
                .thenReturn(FilmRollStatus.COMPLETED);
        when(renderJob.getFilmRoll()).thenReturn(filmRoll);
        when(filmRoll.getId()).thenReturn(2L);

        RenderJobStateService service = new RenderJobStateService(
                renderJobRepository,
                filmRollRepository
        );

        RenderRequestResponse response = service.markQueued(
                renderJobId,
                2L,
                3L,
                publishResult
        );

        assertThat(response.renderJobStatus()).isEqualTo("COMPLETED");
        assertThat(response.filmRollStatus()).isEqualTo("COMPLETED");
        verify(renderJob, never()).markQueued(any());
        verify(filmRoll, never()).markQueued(any());
    }

    @Test
    @DisplayName("PROCESSING 결과가 큐 상태 기록보다 먼저 도착해도 QUEUED로 되돌리지 않는다")
    void keepProcessingStateWhenStartedResultArrivesFirst() {
        UUID renderJobId = UUID.randomUUID();
        RenderQueuePublishResult publishResult =
                new RenderQueuePublishResult("request-message");

        when(renderJobRepository.findByIdForUpdate(renderJobId))
                .thenReturn(Optional.of(renderJob));
        when(filmRollRepository.findByIdAndUserIdForUpdate(2L, 3L))
                .thenReturn(Optional.of(filmRoll));
        when(renderJob.getStatus())
                .thenReturn(RenderJobStatus.PROCESSING);
        when(filmRoll.getStatus())
                .thenReturn(FilmRollStatus.PROCESSING);
        when(renderJob.getFilmRoll()).thenReturn(filmRoll);
        when(filmRoll.getId()).thenReturn(2L);

        RenderJobStateService service = new RenderJobStateService(
                renderJobRepository,
                filmRollRepository
        );

        RenderRequestResponse response = service.markQueued(
                renderJobId,
                2L,
                3L,
                publishResult
        );

        assertThat(response.renderJobStatus()).isEqualTo("PROCESSING");
        assertThat(response.filmRollStatus()).isEqualTo("PROCESSING");
        verify(renderJob, never()).markQueued(any());
        verify(filmRoll, never()).markQueued(any());
    }

    @Test
    @DisplayName("결과가 이미 반영된 작업에는 뒤늦은 큐 실패 상태를 덮어쓰지 않는다")
    void doNotOverwriteCompletedJobWithQueueFailure() {
        UUID renderJobId = UUID.randomUUID();

        when(renderJobRepository.findByIdForUpdate(renderJobId))
                .thenReturn(Optional.of(renderJob));
        when(renderJob.getStatus())
                .thenReturn(RenderJobStatus.COMPLETED);

        RenderJobStateService service = new RenderJobStateService(
                renderJobRepository,
                filmRollRepository
        );

        service.markQueueFailed(
                renderJobId,
                new RuntimeException("timeout")
        );

        verify(renderJob, never()).queueFailed(any(), any());
    }
}
