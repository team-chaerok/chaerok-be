package com.chaerok.backend.filmroll.dto;

import com.chaerok.backend.filmroll.entity.FilmRoll;

public record FilmRollResponse(
        Long filmRollId,
        Long regionId,
        String filterId,
        double filterStrength,
        int filterVersion,
        String status,
        int totalPhotoCount,
        int processedPhotoCount
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
                filmRoll.getProcessedPhotoCount()
        );
    }
}
