package com.chaerok.backend.filmroll.dto;

import com.chaerok.backend.filmroll.entity.FilmRoll;

import java.time.LocalDateTime;

public record FilmRollExitResponse(
        Long filmRollId,
        String status,
        LocalDateTime exitedAt,
        LocalDateTime developAvailableAt,
        boolean developAvailable
) {

    public static FilmRollExitResponse from(FilmRoll filmRoll) {
        return new FilmRollExitResponse(
                filmRoll.getId(),
                filmRoll.getStatus().name(),
                filmRoll.getExitedAt(),
                filmRoll.getDevelopAvailableAt(),
                filmRoll.isDevelopmentAvailable(LocalDateTime.now())
        );
    }
}
