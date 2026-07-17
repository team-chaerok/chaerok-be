package com.chaerok.backend.heritage.controller;

import com.chaerok.backend.heritage.dto.HeritagePlaceResponse;
import com.chaerok.backend.heritage.service.HeritageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Heritage", description = "역사 테마 장소 판별 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/heritage")
public class HeritageController {

    private final HeritageService heritageService;

    @Operation(
            summary = "역사 테마 대상 장소 조회",
            description = "장소 ID를 기준으로 TourAPI를 실시간 조회하여 역사 테마 대상 여부를 반환한다."
    )
    @GetMapping("/places/{placeId}")
    public ResponseEntity<HeritagePlaceResponse> getHeritagePlace(
            @PathVariable Long placeId
    ) {
        return ResponseEntity.ok(heritageService.getHeritagePlace(placeId));
    }
}