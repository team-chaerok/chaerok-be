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
        /*
         * 1) DB 트랜잭션을 먼저 커밋해 RenderJob(CREATED)을 보존합니다.
         * 2) DB 트랜잭션 밖에서 SQS에 전송합니다.
         * 3) 별도 트랜잭션으로 성공/실패 상태를 기록합니다.
         */
        PreparedRenderJob prepared =
                preparationService.prepare(
                        userId,
                        filmRollId
                );

        try {
            RenderQueuePublishResult publishResult =
                    queuePublisher.publish(
                            prepared.message()
                    );

            return stateService.markQueued(
                    prepared.renderJobId(),
                    prepared.filmRollId(),
                    prepared.userId(),
                    publishResult
            );
        } catch (RuntimeException exception) {
            stateService.markQueueFailed(
                    prepared.renderJobId(),
                    exception
            );

            throw exception;
        }
    }
}
