package com.chaerok.backend.place.controller;

import com.chaerok.backend.place.dto.PlaceDetailResponse;
import com.chaerok.backend.place.dto.PlaceListResponse;
import com.chaerok.backend.place.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Place", description = "지역별 장소 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    @Operation(summary = "지역별 장소 목록 조회", description = "regionId를 기준으로 해당 지역의 관광지, 음식점, 카페·디저트 장소 목록을 조회한다.")
    @GetMapping
    public ResponseEntity<List<PlaceListResponse>> getPlaces(
            @RequestParam Long regionId
    ) {
        return ResponseEntity.ok(placeService.getPlacesByRegion(regionId));
    }

    @GetMapping("/external")
    @Operation(summary = "TourAPI 기반 추가 추천 장소 조회", description = "regionId를 기준으로 TourAPI 추가 추천 장소를 조회한다. 조회 결과는 DB에 저장하지 않는다.")
    public ResponseEntity<List<PlaceListResponse>> getExternalPlaces(
            @RequestParam Long regionId
    ) {
        return ResponseEntity.ok(placeService.getExternalPlaces(regionId));
    }

    @Operation(summary = "장소 상세 조회", description = "placeId를 기준으로 장소 상세 정보를 조회한다.")
    @GetMapping("/{placeId}")
    public ResponseEntity<PlaceDetailResponse> getPlace(
            @PathVariable Long placeId
    ) {
        return ResponseEntity.ok(placeService.getPlace(placeId));
    }
}