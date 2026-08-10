package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.filmroll.dto.FilmRollResultResponse;
import com.chaerok.backend.filmroll.service.FilmRollResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Film Roll",
        description = "필름 롤 현상 결과 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/film-rolls")
@ConditionalOnProperty(
        prefix = "aws.s3",
        name = "bucket"
)
public class FilmRollResultController {

    private final FilmRollResultService filmRollResultService;

    @Operation(
            summary = "필름 롤 현상 결과 조회",
            description = """
                    현상 진행 상태를 조회합니다.
                    COMPLETED 상태에서는 필터 사진, ZIP, 릴스의
                    짧은 만료시간을 가진 Presigned Download URL을 반환합니다.
                    S3 객체 키와 RenderJob 정보는 노출하지 않습니다.
                    결과 보관 기간이 지나면 EXPIRED 상태와 빈 결과를 반환합니다.
                    """
    )
    @GetMapping("/{filmRollId}/results")
    public ResponseEntity<FilmRollResultResponse> getResult(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long filmRollId
    ) {
        return ResponseEntity.ok(
                filmRollResultService.getResult(
                        authenticatedUser.userId(),
                        filmRollId
                )
        );
    }
}
