package com.chaerok.backend.render.result;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
        List<FilteredPhotoResult> filteredPhotos,
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
    public static final String EVENT_COMPLETED = "CHAEROK_RENDER_COMPLETED";
    public static final String EVENT_FAILED = "CHAEROK_RENDER_FAILED";

    public RenderResultMessage {
        filteredPhotos = filteredPhotos == null
                ? List.of()
                : List.copyOf(filteredPhotos);
    }

    @JsonIgnore
    public boolean isCompleted() {
        return EVENT_COMPLETED.equals(eventType);
    }

    @JsonIgnore
    public boolean isFailed() {
        return EVENT_FAILED.equals(eventType);
    }

    public record FilteredPhotoResult(
            Long photoId,
            Integer sequence,
            String objectKey,
            Long fileSize
    ) {
    }
}
