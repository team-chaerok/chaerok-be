package com.chaerok.backend.odii.controller;

import com.chaerok.backend.odii.dto.OdiiGuideResponse;
import com.chaerok.backend.odii.service.OdiiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Odii",
        description = "유적지 대표 오디오 가이드 조회 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/odii")
public class OdiiController {

    private final OdiiService odiiService;

    @Operation(
            summary = "유적지 대표 오디오 가이드 조회",
            description = """
                    장소의 Heritage 대상 여부를 확인한 뒤 Odii API를 실시간으로 조회합니다.
                    재생 가능한 오디오가 있으면 대표 가이드 한 개를 반환하고,
                    없으면 한국관광공사 TourAPI 소개 문구를 대체 정보로 반환합니다.
                    """
    )
    @GetMapping("/places/{placeId}")
    public ResponseEntity<OdiiGuideResponse> getAudioGuide(
            @Parameter(
                    description = "장소 ID",
                    example = "1"
            )
            @PathVariable Long placeId
    ) {
        return ResponseEntity.ok(
                odiiService.getAudioGuide(placeId)
        );
    }
}