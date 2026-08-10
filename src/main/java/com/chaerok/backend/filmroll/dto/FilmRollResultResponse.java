package com.chaerok.backend.filmroll.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record FilmRollResultResponse(
        Long filmRollId,
        String status,
        int totalPhotoCount,
        int processedPhotoCount,
        List<FilteredPhotoResponse> filteredPhotos,
        DownloadResponse zip,
        DownloadResponse reel,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime expiresAt,
        FailureResponse failure
) {

    public FilmRollResultResponse {
        filteredPhotos = filteredPhotos == null
                ? List.of()
                : List.copyOf(filteredPhotos);
    }

    public record FilteredPhotoResponse(
            Long photoId,
            int sequence,
            String downloadUrl,
            Instant downloadUrlExpiresAt
    ) {
    }

    public record DownloadResponse(
            String downloadUrl,
            Instant downloadUrlExpiresAt,
            long fileSize
    ) {
    }

    public record FailureResponse(
            String code,
            String message
    ) {
    }
}
