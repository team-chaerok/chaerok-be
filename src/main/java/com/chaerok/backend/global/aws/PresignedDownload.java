package com.chaerok.backend.global.aws;

import java.time.Instant;

public record PresignedDownload(
        String objectKey,
        String downloadUrl,
        Instant expiresAt
) {
}
