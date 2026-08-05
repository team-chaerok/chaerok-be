package com.chaerok.backend.filmroll.entity;

import java.util.List;

public enum FilmRollStatus {
    CAPTURING,
    READY,
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED;

    private static final List<FilmRollStatus> INCOMPLETE_STATUSES =
            List.of(
                    CAPTURING,
                    READY,
                    QUEUED,
                    PROCESSING,
                    FAILED
            );

    public static List<FilmRollStatus> incompleteStatuses() {
        return INCOMPLETE_STATUSES;
    }
}
