package com.chaerok.backend.filter.service;

import com.chaerok.backend.filter.dto.FilterResponse;
import com.chaerok.backend.filter.preset.FilmFilterPresetProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FilterService {

    private final FilmFilterPresetProvider presetProvider;

    public List<FilterResponse> getFilters() {
        return presetProvider.getAll()
                .stream()
                .map(FilterResponse::from)
                .toList();
    }
}