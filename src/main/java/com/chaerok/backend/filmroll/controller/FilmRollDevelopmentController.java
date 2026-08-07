package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.filmroll.dto.FilmRollDevelopmentResponse;
import com.chaerok.backend.filmroll.service.FilmRollDevelopmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Film Roll",
        description = "필름 롤 촬영 및 현상 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/film-rolls")
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class FilmRollDevelopmentController {

    private final FilmRollDevelopmentService developmentService;

    @Operation(
            summary = "필름 롤 현상 시작",
            description = """
                    사진 업로드 정합성과 방문 유형 조건을 확인한 뒤 필름 롤의 현상을 시작합니다.
                    CAPTURING 상태는 내부적으로 READY로 전환한 후 현상 요청을
                    SQS에 등록하며, FAILED 상태는 같은 필름 롤로 재시도합니다.
                    이미 QUEUED 또는 PROCESSING 상태이면 새 RenderJob을 만들지 않고
                    현재 현상 상태를 반환합니다. RenderJob과 SQS 정보는 노출하지 않습니다.
                    """
    )
    @PostMapping("/{filmRollId}/develop")
    public ResponseEntity<FilmRollDevelopmentResponse> develop(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long filmRollId
    ) {
        FilmRollDevelopmentResponse response =
                developmentService.develop(
                        authenticatedUser.userId(),
                        filmRollId
                );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }
}
