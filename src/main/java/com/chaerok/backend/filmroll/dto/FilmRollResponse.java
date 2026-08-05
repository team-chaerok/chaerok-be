package com.chaerok.backend.filmroll.dto;

import com.chaerok.backend.filmroll.entity.FilmRoll;

import java.time.LocalDateTime;

public record FilmRollResponse(
        Long filmRollId,
        Long regionId,
        String filterId,
        double filterStrength,
        int filterVersion,
        String status,
        int totalPhotoCount,
        int processedPhotoCount,
        int maxPhotoCount,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime expiresAt,
        FailureResponse failure,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static FilmRollResponse from(FilmRoll filmRoll) {
        return new FilmRollResponse(
                filmRoll.getId(),
                filmRoll.getRegion().getId(),
                filmRoll.getFilterId(),
                filmRoll.getFilterStrength(),
                filmRoll.getFilterVersion(),
                filmRoll.getStatus().name(),
                filmRoll.getTotalPhotoCount(),
                filmRoll.getProcessedPhotoCount(),
                FilmRoll.MAX_PHOTO_COUNT,
                filmRoll.getRequestedAt(),
                filmRoll.getCompletedAt(),
                filmRoll.getExpiresAt(),
                FailureResponse.from(filmRoll),
                filmRoll.getCreatedAt(),
                filmRoll.getUpdatedAt()
        );
    }

    public record FailureResponse(
            String code,
            String message
    ) {
        private static FailureResponse from(FilmRoll filmRoll) {
            if (filmRoll.getErrorCode() == null
                    && filmRoll.getErrorMessage() == null) {
                return null;
            }

            return new FailureResponse(
                    filmRoll.getErrorCode(),
                    filmRoll.getErrorMessage()
            );
        }
    }
}
