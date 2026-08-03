package com.chaerok.backend.render.dto;

import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.queue.RenderQueuePublishResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record RenderRequestResponse(
        UUID renderJobId,
        Long filmRollId,
        String renderJobStatus,
        String filmRollStatus,
        String queueMessageId,
        LocalDateTime queuedAt
) {

    public static RenderRequestResponse of(
            RenderJob renderJob,
            RenderQueuePublishResult publishResult
    ) {
        return new RenderRequestResponse(
                renderJob.getId(),
                renderJob.getFilmRoll().getId(),
                renderJob.getStatus().name(),
                renderJob.getFilmRoll().getStatus().name(),
                publishResult.messageId(),
                renderJob.getQueuedAt()
        );
    }
}
