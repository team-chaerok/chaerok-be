package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.filmroll.dto.FilmRollPhotoListResponse;
import com.chaerok.backend.filmroll.service.FilmRollPhotoQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Film Roll",
        description = "필름 롤 사진 조회 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/film-rolls")
public class FilmRollPhotoController {

    private final FilmRollPhotoQueryService photoQueryService;

    @Operation(
            summary = "필름 롤 사진 목록 조회",
            description = """
                    사용자가 소유한 필름 롤의 사진을 순서대로 조회합니다.
                    촬영 중 앱 재진입과 업로드 진행상태 복구에 사용할 수 있습니다.
                    S3 객체 키와 다운로드 URL은 노출하지 않습니다.
                    """
    )
    @GetMapping("/{filmRollId}/photos")
    public ResponseEntity<FilmRollPhotoListResponse> getPhotos(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long filmRollId
    ) {
        return ResponseEntity.ok(
                photoQueryService.getPhotos(
                        authenticatedUser.userId(),
                        filmRollId
                )
        );
    }
}
