package com.chaerok.backend.visit.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.visit.dto.VisitCreateRequest;
import com.chaerok.backend.visit.dto.VisitCreateResponse;
import com.chaerok.backend.visit.dto.VisitListResponse;
import com.chaerok.backend.visit.service.VisitCommandService;
import com.chaerok.backend.visit.service.VisitQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Visit",
        description = "필름 롤 방문 인증 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/film-rolls/{filmRollId}/visits")
public class VisitController {

    private final VisitCommandService visitCommandService;
    private final VisitQueryService visitQueryService;

    @Operation(
            summary = "방문 인증",
            description = """
                    프론트가 GPS와 장소 간 거리를 검증하고 방문 사진을 촬영합니다.
                    촬영한 사진을 해당 FilmRoll의 Photo로 업로드 완료한 뒤
                    placeId와 photoId를 전달합니다.
                    백엔드는 GPS 좌표, 정확도, 거리, 이동 경로를 받거나 저장하지 않습니다.
                    같은 필름 롤의 같은 장소는 한 번만 인증할 수 있고,
                    한 장의 사진은 하나의 방문 인증에만 사용할 수 있습니다.
                    """
    )
    @PostMapping
    public ResponseEntity<VisitCreateResponse> createVisit(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long filmRollId,

            @Valid @RequestBody
            VisitCreateRequest request
    ) {
        VisitCreateResponse response =
                visitCommandService.createVisit(
                        authenticatedUser.userId(),
                        filmRollId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "방문 현황 조회",
            description = """
                    필름 롤의 방문 기록과 방문 유형 진행도를 조회합니다.
                    관광지(TOURISM), 식당(FOOD), 카페(CAFE_DESSERT)를
                    각각 1곳 이상 방문하면 현상용 Visit 조건을 충족합니다.
                    """
    )
    @GetMapping
    public ResponseEntity<VisitListResponse> getVisits(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long filmRollId
    ) {
        return ResponseEntity.ok(
                visitQueryService.getVisits(
                        authenticatedUser.userId(),
                        filmRollId
                )
        );
    }
}