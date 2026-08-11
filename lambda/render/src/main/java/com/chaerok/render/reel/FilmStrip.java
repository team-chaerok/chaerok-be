package com.chaerok.render.reel;

import java.nio.file.Path;

public record FilmStrip(
        Path path,
        int width,
        int height,
        int photoCount
) {

    public FilmStrip {
        if (path == null) {
            throw new IllegalArgumentException("path is required.");
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException(
                    "Film strip dimensions must be positive."
            );
        }
        if (photoCount < 1) {
            throw new IllegalArgumentException(
                    "Film strip photoCount must be positive."
            );
        }
    }
}
