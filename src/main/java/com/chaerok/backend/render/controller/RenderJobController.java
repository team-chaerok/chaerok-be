package com.chaerok.backend.render.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.render.dto.RenderRequestResponse;
import com.chaerok.backend.render.service.RenderRequestService;
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
        name = "Render Job",
        description = "필름 롤 현상 요청 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/film-rolls/{filmRollId}/render-jobs")
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class RenderJobController {

    private final RenderRequestService renderRequestService;

    @Operation(
            summary = "현상 요청",
            description = """
                    READY 상태의 필름 롤에 RenderJob을 생성하고
                    사진·필터 스냅샷을 SQS로 전송합니다.
                    전송되면 FilmRoll과 RenderJob은 QUEUED 상태가 됩니다.
                    """
    )
    @PostMapping
    public ResponseEntity<RenderRequestResponse> requestRender(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long filmRollId
    ) {
        RenderRequestResponse response =
                renderRequestService.requestRender(
                        authenticatedUser.userId(),
                        filmRollId
                );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }
}
