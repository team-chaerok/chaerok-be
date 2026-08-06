package com.chaerok.backend.render.service;

import com.chaerok.backend.render.dto.RenderRequestResponse;
import com.chaerok.backend.render.queue.RenderQueueMessage;
import com.chaerok.backend.render.queue.RenderQueuePublishResult;
import com.chaerok.backend.render.queue.RenderQueuePublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderRequestServiceTest {

    @Mock
    private RenderJobPreparationService preparationService;

    @Mock
    private RenderQueuePublisher queuePublisher;

    @Mock
    private RenderJobStateService stateService;

    @Test
    @DisplayName("DB 준비 후 SQS 전송과 QUEUED 전환을 순서대로 수행한다")
    void requestRender() {
        UUID renderJobId = UUID.randomUUID();

        RenderQueueMessage message = new RenderQueueMessage(
                1,
                renderJobId,
                2L,
                6L,
                1L,
                "bucket",
                "gongju",
                0.8,
                1,
                1,
                LocalDateTime.now(),
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
                        "message-123"
                );

        RenderRequestResponse expected =
                mock(RenderRequestResponse.class);

        when(preparationService.prepare(6L, 2L))
                .thenReturn(prepared);

        when(queuePublisher.publish(message))
                .thenReturn(publishResult);

        when(stateService.markQueued(
                renderJobId,
                2L,
                6L,
                publishResult
        )).thenReturn(expected);

        RenderRequestService service =
                new RenderRequestService(
                        preparationService,
                        queuePublisher,
                        stateService
                );

        RenderRequestResponse actual =
                service.requestRender(6L, 2L);

        assertThat(actual).isSameAs(expected);

        InOrder inOrder = inOrder(
                preparationService,
                queuePublisher,
                stateService
        );

        inOrder.verify(preparationService)
                .prepare(6L, 2L);

        inOrder.verify(queuePublisher)
                .publish(message);

        inOrder.verify(stateService)
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
    @DisplayName("SQS 전송 실패 시 QUEUE_FAILED 상태 기록을 요청한다")
    void markQueueFailedWhenPublishingFails() {
        UUID renderJobId = UUID.randomUUID();

        RenderQueueMessage message = new RenderQueueMessage(
                1,
                renderJobId,
                2L,
                6L,
                1L,
                "bucket",
                "gongju",
                0.8,
                1,
                1,
                LocalDateTime.now(),
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
                new RuntimeException("SQS failure");

        when(preparationService.prepare(6L, 2L))
                .thenReturn(prepared);

        when(queuePublisher.publish(message))
                .thenThrow(failure);

        RenderRequestService service =
                new RenderRequestService(
                        preparationService,
                        queuePublisher,
                        stateService
                );

        assertThatThrownBy(() ->
                service.requestRender(6L, 2L)
        ).isSameAs(failure);

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
}
