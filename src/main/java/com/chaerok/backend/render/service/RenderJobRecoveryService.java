package com.chaerok.backend.render.service;

import com.chaerok.backend.render.queue.RenderQueuePublishResult;
import com.chaerok.backend.render.queue.RenderQueuePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class RenderJobRecoveryService {

    private final RenderJobRecoveryPreparationService preparationService;
    private final RenderQueuePublisher queuePublisher;
    private final RenderJobStateService stateService;

    public void recover(
            UUID renderJobId,
            LocalDateTime createdBefore
    ) {
        Optional<PreparedRenderJob> preparedOptional =
                preparationService.prepare(
                        renderJobId,
                        createdBefore
                );

        if (preparedOptional.isEmpty()) {
            return;
        }

        PreparedRenderJob prepared =
                preparedOptional.get();

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

            log.warn(
                    "CREATED 렌더링 작업 SQS 재발행 실패: renderJobId={}, filmRollId={}, error={}",
                    prepared.renderJobId(),
                    prepared.filmRollId(),
                    exception.getMessage()
            );
            return;
        }

        /*
         * SQS 재발행은 이미 성공했습니다.
         *
         * QUEUED 기록이 실패했다고 해서
         * SQS_SEND_FAILED로 상태를 덮어쓰지 않습니다.
         */
        stateService.markQueued(
                prepared.renderJobId(),
                prepared.filmRollId(),
                prepared.userId(),
                publishResult
        );

        log.info(
                "CREATED 렌더링 작업 SQS 복구 완료: renderJobId={}, filmRollId={}, messageId={}",
                prepared.renderJobId(),
                prepared.filmRollId(),
                publishResult.messageId()
        );
    }
}