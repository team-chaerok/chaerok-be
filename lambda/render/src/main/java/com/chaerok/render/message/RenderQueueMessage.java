package com.chaerok.render.message;

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

    public RenderQueueMessage {
        photos = photos == null
                ? List.of()
                : List.copyOf(photos);
    }

    public record PhotoItem(
            Long photoId,
            int sequence,
            String originalObjectKey,
            boolean hasFace,
            String sceneType,
            LocalDateTime takenAt
    ) {
    }
}
