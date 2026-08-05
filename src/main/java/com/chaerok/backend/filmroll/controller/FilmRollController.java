package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.filmroll.dto.FilmRollCreateRequest;
import com.chaerok.backend.filmroll.dto.FilmRollResponse;
import com.chaerok.backend.filmroll.service.FilmRollCommandService;
import com.chaerok.backend.filmroll.service.FilmRollQueryService;
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
        name = "Film Roll",
        description = "필름 롤 생성·조회·상태 전환 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/film-rolls")
public class FilmRollController {

    private final FilmRollCommandService filmRollCommandService;
    private final FilmRollQueryService filmRollQueryService;

    @Operation(
            summary = "필름 롤 생성",
            description = """
                    로그인 사용자의 촬영용 필름 롤을 생성합니다.
                    사용자에게 이미 미완료 필름 롤이 있으면
                    새 필름 롤을 생성할 수 없습니다.
                    FAILED 상태도 재시도 가능한 미완료 상태에 포함됩니다.
                    """
    )
    @PostMapping
    public ResponseEntity<FilmRollResponse> createFilmRoll(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @Valid
            @RequestBody
            FilmRollCreateRequest request
    ) {
        FilmRollResponse response =
                filmRollCommandService.createFilmRoll(
                        authenticatedUser.userId(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "현재 진행 중인 필름 롤 조회",
            description = """
                    로그인 사용자의 미완료 필름 롤을 조회합니다.
                    미완료 상태는 CAPTURING, READY, QUEUED,
                    PROCESSING, FAILED입니다.
                    진행 중인 필름 롤이 없으면 204 No Content를 반환합니다.
                    """
    )
    @GetMapping("/current")
    public ResponseEntity<FilmRollResponse> getCurrentFilmRoll(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
    ) {
        return filmRollQueryService
                .findCurrentFilmRoll(authenticatedUser.userId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(
            summary = "필름 롤 상세 조회",
            description = "로그인 사용자가 소유한 필름 롤의 현재 상태를 조회합니다."
    )
    @GetMapping("/{filmRollId}")
    public ResponseEntity<FilmRollResponse> getFilmRoll(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long filmRollId
    ) {
        return ResponseEntity.ok(
                filmRollQueryService.getFilmRoll(
                        authenticatedUser.userId(),
                        filmRollId
                )
        );
    }

    @Operation(
            summary = "필름 롤 촬영 종료",
            description = """
                    저장된 모든 사진의 업로드가 완료됐는지 확인하고
                    필름 롤을 READY 상태로 전환합니다.
                    """
    )
    @PostMapping("/{filmRollId}/ready")
    public ResponseEntity<FilmRollResponse> markFilmRollReady(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long filmRollId
    ) {
        return ResponseEntity.ok(
                filmRollCommandService.markReady(
                        authenticatedUser.userId(),
                        filmRollId
                )
        );
    }
}
