package com.chaerok.backend.filmroll.dto;

import java.time.LocalDateTime;

public record FilmRollDevelopmentResponse(
        Long filmRollId,
        String status,
        int totalPhotoCount,
        LocalDateTime requestedAt
) {
}
