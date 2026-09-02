package com.chaerok.backend.render.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.render.dto.RenderRequestResponse;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.entity.RenderJobStatus;
import com.chaerok.backend.render.exception.RenderErrorCode;
import com.chaerok.backend.render.queue.RenderQueuePublishResult;
import com.chaerok.backend.render.repository.RenderJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class RenderJobStateService {

    private final RenderJobRepository renderJobRepository;
    private final FilmRollRepository filmRollRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RenderRequestResponse markQueued(
            UUID renderJobId,
            Long filmRollId,
            Long userId,
            RenderQueuePublishResult publishResult
    ) {
        RenderJob renderJob = renderJobRepository
                .findByIdForUpdate(renderJobId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "렌더링 작업을 찾을 수 없습니다."
                        )
                );

        FilmRoll filmRoll = filmRollRepository
                .findByIdAndUserIdForUpdate(
                        filmRollId,
                        userId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                FilmRollErrorCode.FILM_ROLL_NOT_FOUND
                        )
                );

        if (isAlreadyAdvanced(renderJob, filmRoll)) {
            log.info(
                    "큐 전송 성공 기록 전에 처리 상태가 먼저 반영됨: renderJobId={}, renderJobStatus={}, filmRollStatus={}",
                    renderJobId,
                    renderJob.getStatus(),
                    filmRoll.getStatus()
            );

            return RenderRequestResponse.of(
                    renderJob,
                    publishResult
            );
        }

        if (filmRoll.getStatus() != FilmRollStatus.READY) {
            throw new BusinessException(
                    RenderErrorCode.FILM_ROLL_NOT_READY_FOR_QUEUE
            );
        }

        LocalDateTime queuedAt = LocalDateTime.now();

        renderJob.markQueued(
                queuedAt,
                publishResult.messageId()
        );
        filmRoll.markQueued(queuedAt);

        return RenderRequestResponse.of(
                renderJob,
                publishResult
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markQueueFailed(
            UUID renderJobId,
            RuntimeException cause
    ) {
        renderJobRepository.findByIdForUpdate(renderJobId)
                .ifPresent(renderJob -> {
                    if (renderJob.getStatus() == RenderJobStatus.CREATED
                            || renderJob.getStatus() == RenderJobStatus.QUEUED) {
                        renderJob.queueFailed(
                                "SQS_SEND_FAILED",
                                summarize(cause)
                        );
                        return;
                    }

                    log.warn(
                            "큐 전송 실패 기록을 건너뜁니다. 이미 결과가 반영됐을 수 있습니다: renderJobId={}, status={}",
                            renderJobId,
                            renderJob.getStatus()
                    );
                });
    }

    private boolean isAlreadyAdvanced(
            RenderJob renderJob,
            FilmRoll filmRoll
    ) {
        boolean bothProcessing =
                renderJob.getStatus() == RenderJobStatus.PROCESSING
                        && filmRoll.getStatus() == FilmRollStatus.PROCESSING;

        boolean renderTerminal =
                renderJob.getStatus() == RenderJobStatus.COMPLETED
                        || renderJob.getStatus() == RenderJobStatus.FAILED;

        boolean filmRollTerminal =
                filmRoll.getStatus() == FilmRollStatus.COMPLETED
                        || filmRoll.getStatus() == FilmRollStatus.FAILED
                        || filmRoll.getStatus() == FilmRollStatus.EXPIRED;

        return bothProcessing || (renderTerminal && filmRollTerminal);
    }

    private String summarize(RuntimeException cause) {
        String message = cause.getMessage();

        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }

        return message.length() <= 500
                ? message
                : message.substring(0, 500);
    }
}
