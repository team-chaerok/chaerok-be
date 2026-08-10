package com.chaerok.backend.global.aws;

import java.time.Instant;

public record StoredObjectMetadata(
        String objectKey,
        long contentLength,
        String contentType,
        String eTag,
        Instant lastModified
) {
}
