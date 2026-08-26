package com.chaerok.backend.render.result;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.aws.AwsProperties;
import com.chaerok.backend.notification.outbox.service.NotificationOutboxService;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.entity.RenderJobStatus;
import com.chaerok.backend.render.repository.RenderJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderResultProcessorTest {

    @Mock
    private RenderJobRepository renderJobRepository;

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private NotificationOutboxService notificationOutboxService;

    @Mock
    private RenderJob renderJob;

    @Mock
    private RenderJob latestRenderJob;

    @Mock
    private FilmRoll filmRoll;

    @Mock
    private Region region;

    @Mock
    private Photo photo;

    private RenderResultProcessor processor;
    private UUID renderJobId;

    @BeforeEach
    void setUp() {
        AwsProperties awsProperties = new AwsProperties();
        awsProperties.getS3().setBucket("bucket");

        processor = new RenderResultProcessor(
                renderJobRepository,
                filmRollRepository,
                photoRepository,
                notificationOutboxService,
                awsProperties
        );

        renderJobId = UUID.randomUUID();

        when(renderJobRepository.findByIdForUpdate(renderJobId))
                .thenReturn(Optional.of(renderJob));
        when(renderJob.getFilmRoll()).thenReturn(filmRoll);
        when(filmRoll.getId()).thenReturn(2L);
        when(filmRollRepository.findByIdAndUserIdForUpdate(2L, 3L))
                .thenReturn(Optional.of(filmRoll));
        when(filmRoll.getRegion()).thenReturn(region);
        when(region.getId()).thenReturn(1L);
    }

    @Test
    @DisplayName("완료 결과를 사진, 필름 롤, 렌더링 작업에 한 트랜잭션으로 적용한다")
    void applyCompletedResult() {
        RenderResultMessage message = completedMessage();
        LocalDateTime occurredAt = LocalDateTime.ofInstant(
                message.occurredAt(),
                ZoneId.of("Asia/Seoul")
        );

        when(renderJob.getStatus()).thenReturn(RenderJobStatus.CREATED);
        when(filmRoll.getStatus()).thenReturn(FilmRollStatus.READY);
        when(filmRoll.getTotalPhotoCount()).thenReturn(1);
        when(photoRepository.findAllByFilmRollIdOrderBySequenceAscForUpdate(2L))
                .thenReturn(List.of(photo));
        when(photo.getId()).thenReturn(10L);
        when(photo.getSequence()).thenReturn(1);
        when(photo.getStatus()).thenReturn(PhotoStatus.UPLOADED);

        RenderResultProcessingOutcome outcome = processor.process(
                message,
                "result-message-1"
        );

        assertThat(outcome).isEqualTo(RenderResultProcessingOutcome.APPLIED);

        verify(photo).completeFromResult(
                "filtered/001.jpg",
                occurredAt
        );
        verify(filmRoll).completeFromResult(
                "result.zip",
                "result.mp4",
                occurredAt
        );
        verify(renderJob).completeFromResult(
                1,
                "request-1",
                "result-message-1",
                "bucket",
                "result.zip",
                200L,
                "result.mp4",
                300L,
                "manifest.json",
                occurredAt
        );
        verify(notificationOutboxService).enqueueRenderCompleted(
                3L,
                2L,
                renderJobId,
                occurredAt
        );
    }

    @Test
    @DisplayName("최종 실패 결과는 PROCESSING 사진을 업로드 완료 상태로 복구하고 재시도 가능하게 만든다")
    void applyFailedResultResetsProcessingPhotos() {
        RenderResultMessage message = failedMessage();
        LocalDateTime occurredAt = LocalDateTime.ofInstant(
                message.occurredAt(),
                ZoneId.of("Asia/Seoul")
        );

        when(renderJob.getStatus()).thenReturn(RenderJobStatus.PROCESSING);
        when(filmRoll.getStatus()).thenReturn(FilmRollStatus.PROCESSING);
        when(filmRoll.getTotalPhotoCount()).thenReturn(1);
        when(photoRepository.findAllByFilmRollIdOrderBySequenceAscForUpdate(2L))
                .thenReturn(List.of(photo));
        when(photo.getStatus()).thenReturn(PhotoStatus.PROCESSING);

        RenderResultProcessingOutcome outcome = processor.process(
                message,
                "result-message-failed"
        );

        assertThat(outcome).isEqualTo(RenderResultProcessingOutcome.APPLIED);

        verify(photo).resetAfterRenderFailure();
        verify(filmRoll).failFromResult(
                "MEDIA_GENERATION_FAILED",
                "FFmpeg failed"
        );
        verify(renderJob).failFromResult(
                3,
                "request-1",
                "result-message-failed",
                "bucket",
                "MEDIA_GENERATION_FAILED",
                "FFmpeg failed",
                occurredAt
        );
        verify(notificationOutboxService).enqueueRenderFailed(
                3L,
                2L,
                renderJobId,
                occurredAt
        );
    }

    @Test
    @DisplayName("처리 시작 결과는 RenderJob, FilmRoll, Photo를 PROCESSING으로 함께 전환한다")
    void applyStartedResult() {
        RenderResultMessage message = startedMessage(2);
        LocalDateTime occurredAt = LocalDateTime.ofInstant(
                message.occurredAt(),
                ZoneId.of("Asia/Seoul")
        );

        when(renderJob.getStatus()).thenReturn(RenderJobStatus.QUEUED);
        when(filmRoll.getStatus()).thenReturn(FilmRollStatus.QUEUED);
        when(filmRoll.getTotalPhotoCount()).thenReturn(1);
        when(photoRepository.findAllByFilmRollIdOrderBySequenceAscForUpdate(2L))
                .thenReturn(List.of(photo));
        when(photo.getStatus()).thenReturn(PhotoStatus.UPLOADED);

        RenderResultProcessingOutcome outcome = processor.process(
                message,
                "started-result-message"
        );

        assertThat(outcome).isEqualTo(RenderResultProcessingOutcome.APPLIED);
        verify(renderJob).markProcessingFromResult(
                2,
                "request-1",
                occurredAt
        );
        verify(filmRoll).markProcessingFromResult();
        verify(photo).markProcessingFromResult();
        verify(notificationOutboxService).enqueueRenderStarted(
                3L,
                2L,
                renderJobId,
                occurredAt
        );
    }

    @Test
    @DisplayName("완료 뒤 늦게 도착한 처리 시작 결과는 상태를 되돌리지 않고 RenderJob 시작 시각만 보완한다")
    void applyLateStartedResultAfterCompletion() {
        RenderResultMessage message = startedMessage(1);
        LocalDateTime occurredAt = LocalDateTime.ofInstant(
                message.occurredAt(),
                ZoneId.of("Asia/Seoul")
        );

        when(renderJob.getStatus()).thenReturn(RenderJobStatus.COMPLETED);
        when(filmRoll.getStatus()).thenReturn(FilmRollStatus.COMPLETED);

        RenderResultProcessingOutcome outcome = processor.process(
                message,
                "late-started-result-message"
        );

        assertThat(outcome).isEqualTo(RenderResultProcessingOutcome.APPLIED);
        verify(renderJob).markProcessingFromResult(
                1,
                "request-1",
                occurredAt
        );
        verify(filmRoll, never()).markProcessingFromResult();
        verifyNoInteractions(photoRepository);
        verify(notificationOutboxService, never())
                .enqueueRenderStarted(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    @DisplayName("재시도 작업 뒤에 도착한 QUEUE_FAILED 이전 작업의 완료 결과는 무시한다")
    void ignoreCompletedResultFromOlderQueueFailedJob() {
        RenderResultMessage message = completedMessage();
        UUID latestRenderJobId = UUID.randomUUID();

        when(renderJobRepository.findFirstByFilmRollIdOrderByCreatedAtDesc(2L))
                .thenReturn(Optional.of(latestRenderJob));
        when(latestRenderJob.getId()).thenReturn(latestRenderJobId);

        RenderResultProcessingOutcome outcome = processor.process(
                message,
                "result-message-old-completed"
        );

        assertThat(outcome)
                .isEqualTo(RenderResultProcessingOutcome.STALE_IGNORED);

        verifyNoInteractions(photoRepository);
        verify(renderJob, never()).completeFromResult(
                anyInt(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(filmRoll, never()).completeFromResult(
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("재시도 작업 뒤에 도착한 QUEUE_FAILED 이전 작업의 실패 결과는 무시한다")
    void ignoreFailedResultFromOlderQueueFailedJob() {
        RenderResultMessage message = failedMessage();
        UUID latestRenderJobId = UUID.randomUUID();

        when(renderJobRepository.findFirstByFilmRollIdOrderByCreatedAtDesc(2L))
                .thenReturn(Optional.of(latestRenderJob));
        when(latestRenderJob.getId()).thenReturn(latestRenderJobId);

        RenderResultProcessingOutcome outcome = processor.process(
                message,
                "result-message-old-failed"
        );

        assertThat(outcome)
                .isEqualTo(RenderResultProcessingOutcome.STALE_IGNORED);

        verifyNoInteractions(photoRepository);
        verify(renderJob, never()).failFromResult(
                anyInt(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(filmRoll, never()).failFromResult(any(), any());
    }

    @Test
    @DisplayName("완료된 작업 뒤에 도착한 실패 결과는 상태를 되돌리지 않고 삭제 가능하게 처리한다")
    void ignoreStaleFailedResult() {
        RenderResultMessage message = failedMessage();

        when(renderJob.getStatus()).thenReturn(RenderJobStatus.COMPLETED);
        when(filmRoll.getStatus()).thenReturn(FilmRollStatus.COMPLETED);
        RenderResultProcessingOutcome outcome = processor.process(
                message,
                "result-message-2"
        );

        assertThat(outcome)
                .isEqualTo(RenderResultProcessingOutcome.STALE_IGNORED);

        verify(renderJob, never()).failFromResult(
                anyInt(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(filmRoll, never()).failFromResult(any(), any());
    }

    private RenderResultMessage startedMessage(int attempt) {
        return new RenderResultMessage(
                1,
                RenderResultMessage.EVENT_STARTED,
                "request-1",
                renderJobId,
                2L,
                3L,
                1L,
                "bucket",
                "PROCESSING",
                attempt,
                false,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-08-05T03:40:00Z"),
                null,
                null
        );
    }

    private RenderResultMessage completedMessage() {
        return new RenderResultMessage(
                1,
                RenderResultMessage.EVENT_COMPLETED,
                "request-1",
                renderJobId,
                2L,
                3L,
                1L,
                "bucket",
                "COMPLETED",
                1,
                false,
                List.of(
                        new RenderResultMessage.FilteredPhotoResult(
                                10L,
                                1,
                                "filtered/001.jpg",
                                100L
                        )
                ),
                "result.zip",
                200L,
                "result.mp4",
                300L,
                "manifest.json",
                Instant.parse("2026-08-05T03:42:31Z"),
                null,
                null
        );
    }

    private RenderResultMessage failedMessage() {
        return new RenderResultMessage(
                1,
                RenderResultMessage.EVENT_FAILED,
                "request-1",
                renderJobId,
                2L,
                3L,
                1L,
                "bucket",
                "FAILED",
                3,
                false,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-08-05T03:42:31Z"),
                "MEDIA_GENERATION_FAILED",
                "FFmpeg failed"
        );
    }
}
