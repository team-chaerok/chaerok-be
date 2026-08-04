package com.chaerok.backend.filter.engine;

import com.chaerok.backend.filter.analysis.AdaptiveFilterPolicy;
import com.chaerok.backend.filter.analysis.FilterAdjustment;
import com.chaerok.backend.filter.analysis.FilterOverlayTuning;
import com.chaerok.backend.filter.analysis.FilterOverlayTuningPolicy;
import com.chaerok.backend.filter.analysis.ImageAnalysis;
import com.chaerok.backend.filter.analysis.ImageSceneAnalyzer;
import com.chaerok.backend.filter.analysis.SceneType;
import com.chaerok.backend.filter.preset.FilmFilterPreset;
import com.chaerok.backend.filter.preset.FilmFilterPresetProvider;
import com.chaerok.backend.filter.processor.ContrastProcessor;
import com.chaerok.backend.filter.processor.ExposureProcessor;
import com.chaerok.backend.filter.processor.FadeProcessor;
import com.chaerok.backend.filter.processor.GrainProcessor;
import com.chaerok.backend.filter.processor.OverlayImageCache;
import com.chaerok.backend.filter.processor.OverlayProcessor;
import com.chaerok.backend.filter.processor.TemperatureProcessor;
import com.chaerok.backend.filter.processor.VignetteProcessor;

import java.awt.image.BufferedImage;

public class FilmFilterEngine {

    private static final double MAX_ADAPTIVE_STRENGTH = 1.25;

    private final FilmFilterPresetProvider presetProvider;
    private final OverlayImageCache overlayImageCache;
    private final ImageSceneAnalyzer imageSceneAnalyzer;
    private final AdaptiveFilterPolicy adaptiveFilterPolicy;
    private final FilterOverlayTuningPolicy filterOverlayTuningPolicy;

    public FilmFilterEngine(
            FilmFilterPresetProvider presetProvider,
            OverlayImageCache overlayImageCache,
            ImageSceneAnalyzer imageSceneAnalyzer,
            AdaptiveFilterPolicy adaptiveFilterPolicy,
            FilterOverlayTuningPolicy filterOverlayTuningPolicy
    ) {
        this.presetProvider = presetProvider;
        this.overlayImageCache = overlayImageCache;
        this.imageSceneAnalyzer = imageSceneAnalyzer;
        this.adaptiveFilterPolicy = adaptiveFilterPolicy;
        this.filterOverlayTuningPolicy = filterOverlayTuningPolicy;
    }

    public BufferedImage apply(
            BufferedImage original,
            String filterId,
            double strength
    ) {
        return apply(
                original,
                filterId,
                strength,
                false,
                null
        );
    }

    public BufferedImage apply(
            BufferedImage original,
            String filterId,
            double strength,
            boolean hasFace,
            SceneType forcedSceneType
    ) {
        validateOriginal(original);
        validateFilterId(filterId);

        String safeFilterId = filterId.trim();

        FilmFilterPreset preset =
                presetProvider.getByFilterId(safeFilterId);

        double userStrength = clamp(
                strength,
                0.0,
                1.0
        );

        ImageAnalysis analysis =
                imageSceneAnalyzer.analyze(
                        original,
                        hasFace,
                        forcedSceneType
                );

        FilterAdjustment adjustment =
                adaptiveFilterPolicy.calculate(analysis);

        FilterOverlayTuning overlayTuning =
                filterOverlayTuningPolicy.getTuning(
                        safeFilterId
                );

        double finalOverlayStrength =
                calculateOverlayStrength(
                        userStrength,
                        adjustment.overlay(),
                        overlayTuning
                );

        printAnalysisLog(
                safeFilterId,
                userStrength,
                analysis,
                adjustment,
                overlayTuning,
                finalOverlayStrength
        );

        BufferedImage result = original;

        result = new ExposureProcessor(
                preset.exposure()
        ).process(
                result,
                calculateAdaptiveStrength(
                        userStrength,
                        adjustment.exposure()
                )
        );

        result = new TemperatureProcessor(
                preset.temperature()
        ).process(
                result,
                calculateAdaptiveStrength(
                        userStrength,
                        adjustment.temperature()
                )
        );

        result = new ContrastProcessor(
                preset.contrast()
        ).process(
                result,
                calculateAdaptiveStrength(
                        userStrength,
                        adjustment.contrast()
                )
        );

        result = new FadeProcessor(
                preset.fade()
        ).process(
                result,
                calculateAdaptiveStrength(
                        userStrength,
                        adjustment.fade()
                )
        );

        result = new GrainProcessor(
                preset.grain()
        ).process(
                result,
                calculateAdaptiveStrength(
                        userStrength,
                        adjustment.grain()
                )
        );

        result = new OverlayProcessor(
                overlayImageCache,
                preset.overlayPath(),
                preset.overlayOpacity(),
                preset.overlayBlendMode()
        ).process(
                result,
                finalOverlayStrength
        );

        result = new VignetteProcessor(
                preset.vignette()
        ).process(
                result,
                calculateAdaptiveStrength(
                        userStrength,
                        adjustment.vignette()
                )
        );

        return result;
    }

    private double calculateAdaptiveStrength(
            double userStrength,
            double adaptiveMultiplier
    ) {
        return clamp(
                userStrength * adaptiveMultiplier,
                0.0,
                MAX_ADAPTIVE_STRENGTH
        );
    }

    /**
     * 장면·밝기 배율에 필터별 배율을 추가로 곱합니다.
     *
     * 결과는 필터별 maxStrength를 초과할 수 없습니다.
     */
    private double calculateOverlayStrength(
            double userStrength,
            double adaptiveMultiplier,
            FilterOverlayTuning tuning
    ) {
        double calculatedStrength =
                userStrength
                        * adaptiveMultiplier
                        * tuning.multiplier();

        return clamp(
                calculatedStrength,
                0.0,
                tuning.maxStrength()
        );
    }

    private void validateOriginal(
            BufferedImage original
    ) {
        if (original == null) {
            throw new IllegalArgumentException(
                    "원본 이미지가 비어 있습니다."
            );
        }

        if (
                original.getWidth() <= 0
                        || original.getHeight() <= 0
        ) {
            throw new IllegalArgumentException(
                    "원본 이미지 크기가 올바르지 않습니다."
            );
        }
    }

    private void validateFilterId(String filterId) {
        if (filterId == null || filterId.isBlank()) {
            throw new IllegalArgumentException(
                    "filterId는 필수입니다."
            );
        }
    }

    private void printAnalysisLog(
            String filterId,
            double userStrength,
            ImageAnalysis analysis,
            FilterAdjustment adjustment,
            FilterOverlayTuning overlayTuning,
            double finalOverlayStrength
    ) {
        System.out.printf(
                """
                        
                        [Adaptive Film Filter]
                        filterId=%s
                        scene=%s
                        hasFace=%s
                        brightness=%.3f
                        darkPixelRatio=%.3f
                        highlightPixelRatio=%.3f
                        contrast=%.3f
                        userStrength=%.3f
                        exposureMultiplier=%.3f
                        contrastMultiplier=%.3f
                        temperatureMultiplier=%.3f
                        fadeMultiplier=%.3f
                        grainMultiplier=%.3f
                        vignetteMultiplier=%.3f
                        sceneOverlayMultiplier=%.3f
                        filterOverlayMultiplier=%.3f
                        filterOverlayMaxStrength=%.3f
                        finalOverlayStrength=%.3f
                        
                        """,
                filterId,
                analysis.sceneType(),
                analysis.hasFace(),
                analysis.brightness(),
                analysis.darkPixelRatio(),
                analysis.highlightPixelRatio(),
                analysis.contrast(),
                userStrength,
                adjustment.exposure(),
                adjustment.contrast(),
                adjustment.temperature(),
                adjustment.fade(),
                adjustment.grain(),
                adjustment.vignette(),
                adjustment.overlay(),
                overlayTuning.multiplier(),
                overlayTuning.maxStrength(),
                finalOverlayStrength
        );
    }

    private double clamp(
            double value,
            double min,
            double max
    ) {
        return Math.max(
                min,
                Math.min(max, value)
        );
    }
}