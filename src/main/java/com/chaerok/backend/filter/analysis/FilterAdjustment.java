package com.chaerok.backend.filter.analysis;

public record FilterAdjustment(
        double exposure,
        double contrast,
        double temperature,
        double fade,
        double grain,
        double vignette,
        double overlay
) {
}