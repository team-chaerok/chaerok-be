package com.chaerok.backend.region.controller;

import com.chaerok.backend.region.dto.RegionResponse;
import com.chaerok.backend.region.dto.ResolveRegionRequest;
import com.chaerok.backend.region.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Region", description = "지역 검증 API")
@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(
            summary = "현재 지역 검증",
            description = "프론트에서 판별한 시·도명과 시·군명을 기준으로 채록 서비스 대상 지역 여부를 검증합니다."
    )
    @PostMapping("/resolve")
    public ResponseEntity<RegionResponse> resolveRegion(
            @Valid @RequestBody ResolveRegionRequest request
    ) {
        RegionResponse response = regionService.resolve(request);

        return ResponseEntity.ok(response);
    }
}
