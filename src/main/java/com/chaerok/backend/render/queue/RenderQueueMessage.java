package com.chaerok.backend.render.queue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RenderQueueMessage(
        int schemaVersion,
        UUID renderJobId,
        Long filmRollId,
        Long userId,
        Long regionId,
        String bucket,
        String filterId,
        double filterStrength,
        int filterVersion,
        int totalPhotoCount,
        LocalDateTime requestedAt,
        List<PhotoItem> photos
) {

    public static final int CURRENT_SCHEMA_VERSION = 2;

    public RenderQueueMessage {
        photos = List.copyOf(photos);
    }

    public record PhotoItem(
            Long photoId,
            int sequence,
            String originalObjectKey,
            LocalDateTime takenAt
    ) {
    }
}
