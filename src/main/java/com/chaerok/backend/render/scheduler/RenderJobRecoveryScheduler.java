package com.chaerok.backend.render.scheduler;

import com.chaerok.backend.render.entity.RenderJobStatus;
import com.chaerok.backend.render.repository.RenderJobRepository;
import com.chaerok.backend.render.service.RenderJobRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class RenderJobRecoveryScheduler {

    private static final long STALE_SECONDS = 300L;
    private static final int BATCH_SIZE = 20;

    private final RenderJobRepository renderJobRepository;
    private final RenderJobRecoveryService recoveryService;

    @Scheduled(
            fixedDelayString =
                    "${chaerok.render.created-recovery-poll-delay-ms:60000}"
    )
    public void recoverStaleCreatedJobs() {
        LocalDateTime createdBefore =
                LocalDateTime.now()
                        .minusSeconds(STALE_SECONDS);

        List<UUID> renderJobIds =
                renderJobRepository.findStaleIds(
                        RenderJobStatus.CREATED,
                        createdBefore,
                        PageRequest.of(0, BATCH_SIZE)
                );

        for (UUID renderJobId : renderJobIds) {
            try {
                recoveryService.recover(
                        renderJobId,
                        createdBefore
                );
            } catch (RuntimeException exception) {
                log.error(
                        "CREATED 렌더링 작업 복구 처리 중 오류: renderJobId={}",
                        renderJobId,
                        exception
                );
            }
        }
    }
}