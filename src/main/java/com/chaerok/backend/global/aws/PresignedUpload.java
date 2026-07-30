package com.chaerok.backend.global.aws;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PresignedUpload(
        String objectKey,
        String uploadUrl,
        Instant expiresAt,
        Map<String, List<String>> requiredHeaders
) {
}
