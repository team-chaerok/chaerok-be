package com.chaerok.backend.filter.analysis;

public record ImageAnalysis(
        double brightness,
        double darkPixelRatio,
        double highlightPixelRatio,
        double contrast,
        boolean hasFace,
        SceneType sceneType
) {
}