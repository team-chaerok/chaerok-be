package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;

import java.time.LocalDateTime;

public record PreparedFilmRollDevelopment(
        Long filmRollId,
        String status,
        int totalPhotoCount,
        LocalDateTime requestedAt,
        boolean renderRequestRequired
) {

    public static PreparedFilmRollDevelopment requestRequired(
            FilmRoll filmRoll
    ) {
        return from(filmRoll, true);
    }

    public static PreparedFilmRollDevelopment alreadyRequested(
            FilmRoll filmRoll
    ) {
        return from(filmRoll, false);
    }

    private static PreparedFilmRollDevelopment from(
            FilmRoll filmRoll,
            boolean renderRequestRequired
    ) {
        if (filmRoll == null) {
            throw new IllegalArgumentException(
                    "필름 롤은 필수입니다."
            );
        }

        return new PreparedFilmRollDevelopment(
                filmRoll.getId(),
                filmRoll.getStatus().name(),
                filmRoll.getTotalPhotoCount(),
                filmRoll.getRequestedAt(),
                renderRequestRequired
        );
    }
}
