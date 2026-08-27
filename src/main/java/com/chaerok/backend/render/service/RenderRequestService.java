package com.chaerok.backend.render.service;

import com.chaerok.backend.render.dto.RenderRequestResponse;
import com.chaerok.backend.render.queue.RenderQueuePublishResult;
import com.chaerok.backend.render.queue.RenderQueuePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class RenderRequestService {

    private final RenderJobPreparationService preparationService;
    private final RenderQueuePublisher queuePublisher;
    private final RenderJobStateService stateService;

    public RenderRequestResponse requestRender(
            Long userId,
            Long filmRollId
    ) {
        PreparedRenderJob prepared =
                preparationService.prepare(
                        userId,
                        filmRollId
                );

        RenderQueuePublishResult publishResult;

        try {
            publishResult =
                    queuePublisher.publish(
                            prepared.message()
                    );
        } catch (RuntimeException exception) {
            stateService.markQueueFailed(
                    prepared.renderJobId(),
                    exception
            );

            throw exception;
        }

        /*
         * 여기까지 왔으면 SQS 전송은 이미 성공했습니다.
         *
         * 따라서 아래 QUEUED DB 기록이 실패하더라도
         * SQS_SEND_FAILED로 기록하면 안 됩니다.
         *
         * CREATED로 남은 작업은 결과 메시지 또는
         * CREATED recovery가 다시 처리할 수 있습니다.
         */
        return stateService.markQueued(
                prepared.renderJobId(),
                prepared.filmRollId(),
                prepared.userId(),
                publishResult
        );
    }
}