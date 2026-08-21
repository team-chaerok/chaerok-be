package com.chaerok.render.result;

import com.chaerok.render.message.RenderQueueMessage;
import com.chaerok.render.output.FilteredPhotoOutput;
import com.chaerok.render.output.RenderOutput;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RenderResultMessage(
        int schemaVersion,
        String eventType,
        String requestMessageId,
        UUID renderJobId,
        Long filmRollId,
        Long userId,
        Long regionId,
        String bucket,
        String status,
        int attempt,
        boolean retryable,
        List<FilteredPhotoOutput> filteredPhotos,
        String zipObjectKey,
        Long zipFileSize,
        String reelObjectKey,
        Long reelFileSize,
        String manifestObjectKey,
        Instant occurredAt,
        String errorCode,
        String errorMessage
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String EVENT_STARTED =
            "CHAEROK_RENDER_STARTED";
    public static final String EVENT_COMPLETED =
            "CHAEROK_RENDER_COMPLETED";
    public static final String EVENT_FAILED =
            "CHAEROK_RENDER_FAILED";

    public RenderResultMessage {
        filteredPhotos = filteredPhotos == null
                ? List.of()
                : List.copyOf(filteredPhotos);
    }

    public static RenderResultMessage started(
            RenderQueueMessage request,
            String requestMessageId,
            int attempt,
            Instant occurredAt
    ) {
        return new RenderResultMessage(
                CURRENT_SCHEMA_VERSION,
                EVENT_STARTED,
                requestMessageId,
                request.renderJobId(),
                request.filmRollId(),
                request.userId(),
                request.regionId(),
                request.bucket(),
                "PROCESSING",
                normalizeAttempt(attempt),
                false,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                occurredAt,
                null,
                null
        );
    }

    public static RenderResultMessage completed(
            RenderQueueMessage request,
            RenderOutput output,
            String requestMessageId,
            int attempt
    ) {
        return new RenderResultMessage(
                CURRENT_SCHEMA_VERSION,
                EVENT_COMPLETED,
                requestMessageId,
                request.renderJobId(),
                request.filmRollId(),
                request.userId(),
                request.regionId(),
                request.bucket(),
                "COMPLETED",
                normalizeAttempt(attempt),
                false,
                output.filteredPhotos(),
                output.zipObjectKey(),
                output.zipFileSize(),
                output.reelObjectKey(),
                output.reelFileSize(),
                output.manifestObjectKey(),
                output.completedAt(),
                null,
                null
        );
    }

    public static RenderResultMessage failed(
            RenderQueueMessage request,
            String requestMessageId,
            int attempt,
            boolean retryable,
            Instant occurredAt,
            String errorCode,
            String errorMessage
    ) {
        return new RenderResultMessage(
                CURRENT_SCHEMA_VERSION,
                EVENT_FAILED,
                requestMessageId,
                request.renderJobId(),
                request.filmRollId(),
                request.userId(),
                request.regionId(),
                request.bucket(),
                "FAILED",
                normalizeAttempt(attempt),
                retryable,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                occurredAt,
                errorCode,
                errorMessage
        );
    }

    private static int normalizeAttempt(int attempt) {
        return Math.max(attempt, 1);
    }
}
