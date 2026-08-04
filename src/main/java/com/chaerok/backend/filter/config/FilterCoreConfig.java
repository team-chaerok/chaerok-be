package com.chaerok.backend.filter.config;

import com.chaerok.backend.filter.analysis.AdaptiveFilterPolicy;
import com.chaerok.backend.filter.analysis.FilterOverlayTuningPolicy;
import com.chaerok.backend.filter.analysis.ImageSceneAnalyzer;
import com.chaerok.backend.filter.engine.FilmFilterEngine;
import com.chaerok.backend.filter.preset.FilmFilterPresetProvider;
import com.chaerok.backend.filter.processor.OverlayImageCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterCoreConfig {

    @Bean
    public FilmFilterPresetProvider filmFilterPresetProvider() {
        return new FilmFilterPresetProvider();
    }

    @Bean
    public OverlayImageCache overlayImageCache() {
        return new OverlayImageCache();
    }

    @Bean
    public ImageSceneAnalyzer imageSceneAnalyzer() {
        return new ImageSceneAnalyzer();
    }

    @Bean
    public AdaptiveFilterPolicy adaptiveFilterPolicy() {
        return new AdaptiveFilterPolicy();
    }

    @Bean
    public FilterOverlayTuningPolicy filterOverlayTuningPolicy() {
        return new FilterOverlayTuningPolicy();
    }

    @Bean
    public FilmFilterEngine filmFilterEngine(
            FilmFilterPresetProvider presetProvider,
            OverlayImageCache overlayImageCache,
            ImageSceneAnalyzer imageSceneAnalyzer,
            AdaptiveFilterPolicy adaptiveFilterPolicy,
            FilterOverlayTuningPolicy filterOverlayTuningPolicy
    ) {
        return new FilmFilterEngine(
                presetProvider,
                overlayImageCache,
                imageSceneAnalyzer,
                adaptiveFilterPolicy,
                filterOverlayTuningPolicy
        );
    }
}
