package com.chaerok.backend.filter.controller;

import com.chaerok.backend.filter.dto.FilterResponse;
import com.chaerok.backend.filter.service.FilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Filter", description = "필름 필터 API")
@RestController
@RequestMapping("/api/filters")
@RequiredArgsConstructor
public class FilterController {

    private final FilterService filterService;

    @Operation(summary = "필터 목록 조회")
    @GetMapping
    public List<FilterResponse> getFilters() {
        return filterService.getFilters();
    }
}