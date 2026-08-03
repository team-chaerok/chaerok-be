package com.chaerok.backend.render.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollConflictException;
import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.render.dto.RenderRequestResponse;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.queue.RenderQueuePublishResult;
import com.chaerok.backend.render.repository.RenderJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

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
                .findById(renderJobId)
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
                .orElseThrow(FilmRollNotFoundException::new);

        if (filmRoll.getStatus() != FilmRollStatus.READY) {
            throw new FilmRollConflictException(
                    "READY 상태에서만 현상 대기 상태로 전환할 수 있습니다."
            );
        }

        LocalDateTime queuedAt = LocalDateTime.now();

        renderJob.markQueued(queuedAt);
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
        renderJobRepository.findById(renderJobId)
                .ifPresent(renderJob ->
                        renderJob.queueFailed(
                                "SQS_SEND_FAILED",
                                summarize(cause)
                        )
                );
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
