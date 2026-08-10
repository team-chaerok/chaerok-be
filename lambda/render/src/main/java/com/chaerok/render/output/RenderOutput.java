package com.chaerok.render.output;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RenderOutput(
        int schemaVersion,
        UUID renderJobId,
        Long filmRollId,
        String status,
        List<FilteredPhotoOutput> filteredPhotos,
        String zipObjectKey,
        long zipFileSize,
        String reelObjectKey,
        long reelFileSize,
        String manifestObjectKey,
        Instant completedAt
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public RenderOutput {
        filteredPhotos = List.copyOf(filteredPhotos);
    }
}
