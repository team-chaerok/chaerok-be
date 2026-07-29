package com.chaerok.backend.filter.dto;

import com.chaerok.backend.filter.preset.FilmFilterPreset;

public record FilterResponse(
        String filterId,
        String name,
        String description
) {

    public static FilterResponse from(FilmFilterPreset preset) {
        return new FilterResponse(
                preset.filterId(),
                preset.name(),
                preset.description()
        );
    }
}