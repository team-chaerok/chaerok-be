package com.chaerok.backend.render.service;

import com.chaerok.backend.render.queue.RenderQueueMessage;
import com.chaerok.backend.render.queue.RenderQueuePublishResult;
import com.chaerok.backend.render.queue.RenderQueuePublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderJobRecoveryServiceTest {

    @Mock
    private RenderJobRecoveryPreparationService preparationService;

    @Mock
    private RenderQueuePublisher queuePublisher;

    @Mock
    private RenderJobStateService stateService;

    @Test
    @DisplayName("stale CREATED 작업을 같은 메시지로 재발행하고 QUEUED 처리한다")
    void recoversCreatedJob() {
        UUID renderJobId = UUID.randomUUID();

        LocalDateTime cutoff =
                LocalDateTime.of(2026, 8, 27, 15, 0);

        RenderQueueMessage message =
                new RenderQueueMessage(
                        RenderQueueMessage.CURRENT_SCHEMA_VERSION,
                        renderJobId,
                        2L,
                        6L,
                        1L,
                        "bucket",
                        "gongju",
                        0.8,
                        1,
                        1,
                        cutoff.minusMinutes(10),
                        List.of()
                );

        PreparedRenderJob prepared =
                new PreparedRenderJob(
                        renderJobId,
                        2L,
                        6L,
                        message
                );

        RenderQueuePublishResult publishResult =
                new RenderQueuePublishResult(
                        "recovered-message-id"
                );

        when(preparationService.prepare(
                renderJobId,
                cutoff
        )).thenReturn(Optional.of(prepared));

        when(queuePublisher.publish(message))
                .thenReturn(publishResult);

        RenderJobRecoveryService service =
                new RenderJobRecoveryService(
                        preparationService,
                        queuePublisher,
                        stateService
                );

        service.recover(
                renderJobId,
                cutoff
        );

        verify(queuePublisher)
                .publish(message);

        verify(stateService)
                .markQueued(
                        renderJobId,
                        2L,
                        6L,
                        publishResult
                );

        verify(stateService, never())
                .markQueueFailed(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    @DisplayName("복구 대상이 아니면 SQS를 호출하지 않는다")
    void skipsWhenPreparationReturnsEmpty() {
        UUID renderJobId = UUID.randomUUID();
        LocalDateTime cutoff = LocalDateTime.now();

        when(preparationService.prepare(
                renderJobId,
                cutoff
        )).thenReturn(Optional.empty());

        RenderJobRecoveryService service =
                new RenderJobRecoveryService(
                        preparationService,
                        queuePublisher,
                        stateService
                );

        service.recover(
                renderJobId,
                cutoff
        );

        verifyNoInteractions(
                queuePublisher,
                stateService
        );
    }

    @Test
    @DisplayName("복구 SQS 재발행 실패 시 QUEUE_FAILED를 기록한다")
    void marksQueueFailedWhenRepublishFails() {
        UUID renderJobId = UUID.randomUUID();

        LocalDateTime cutoff =
                LocalDateTime.of(2026, 8, 27, 15, 0);

        RenderQueueMessage message =
                new RenderQueueMessage(
                        RenderQueueMessage.CURRENT_SCHEMA_VERSION,
                        renderJobId,
                        2L,
                        6L,
                        1L,
                        "bucket",
                        "gongju",
                        0.8,
                        1,
                        1,
                        cutoff.minusMinutes(10),
                        List.of()
                );

        PreparedRenderJob prepared =
                new PreparedRenderJob(
                        renderJobId,
                        2L,
                        6L,
                        message
                );

        RuntimeException failure =
                new RuntimeException("SQS unavailable");

        when(preparationService.prepare(
                renderJobId,
                cutoff
        )).thenReturn(Optional.of(prepared));

        when(queuePublisher.publish(message))
                .thenThrow(failure);

        RenderJobRecoveryService service =
                new RenderJobRecoveryService(
                        preparationService,
                        queuePublisher,
                        stateService
                );

        service.recover(
                renderJobId,
                cutoff
        );

        verify(stateService)
                .markQueueFailed(
                        renderJobId,
                        failure
                );

        verify(stateService, never())
                .markQueued(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    @DisplayName("SQS 재발행 성공 후 QUEUED 기록 실패는 QUEUE_FAILED로 오기록하지 않는다")
    void doesNotMarkQueueFailedWhenRecoveryQueuedWriteFails() {
        UUID renderJobId = UUID.randomUUID();

        LocalDateTime cutoff =
                LocalDateTime.of(
                        2026,
                        8,
                        27,
                        15,
                        0
                );

        RenderQueueMessage message =
                new RenderQueueMessage(
                        RenderQueueMessage.CURRENT_SCHEMA_VERSION,
                        renderJobId,
                        2L,
                        6L,
                        1L,
                        "bucket",
                        "gongju",
                        0.8,
                        1,
                        1,
                        cutoff.minusMinutes(10),
                        List.of()
                );

        PreparedRenderJob prepared =
                new PreparedRenderJob(
                        renderJobId,
                        2L,
                        6L,
                        message
                );

        RenderQueuePublishResult publishResult =
                new RenderQueuePublishResult(
                        "recovery-message-success"
                );

        RuntimeException dbFailure =
                new RuntimeException(
                        "QUEUED DB write failed"
                );

        when(preparationService.prepare(
                renderJobId,
                cutoff
        )).thenReturn(
                Optional.of(prepared)
        );

        when(queuePublisher.publish(message))
                .thenReturn(publishResult);

        when(stateService.markQueued(
                renderJobId,
                2L,
                6L,
                publishResult
        )).thenThrow(dbFailure);

        RenderJobRecoveryService service =
                new RenderJobRecoveryService(
                        preparationService,
                        queuePublisher,
                        stateService
                );

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() ->
                        service.recover(
                                renderJobId,
                                cutoff
                        )
                )
                .isSameAs(dbFailure);

        verify(stateService, never())
                .markQueueFailed(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }
}