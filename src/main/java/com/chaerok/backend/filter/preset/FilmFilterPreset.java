package com.chaerok.backend.filter.preset;

public record FilmFilterPreset(
        String filterId,
        String name,
        String description,

        double exposure,
        double contrast,
        double temperature,
        double fade,
        double grain,
        double vignette,

        String overlayPath,
        double overlayOpacity,
        String overlayBlendMode
) {
}