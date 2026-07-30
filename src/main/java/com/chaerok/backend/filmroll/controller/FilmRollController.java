package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.filmroll.dto.FilmRollCreateRequest;
import com.chaerok.backend.filmroll.dto.FilmRollResponse;
import com.chaerok.backend.filmroll.service.FilmRollCommandService;
import com.chaerok.backend.photo.dto.PhotoUploadCompleteResponse;
import com.chaerok.backend.photo.dto.PhotoUploadUrlRequest;
import com.chaerok.backend.photo.dto.PhotoUploadUrlResponse;
import com.chaerok.backend.photo.service.PhotoUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Film Roll",
        description = "필름 롤 촬영 및 사진 업로드 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/film-rolls")
@ConditionalOnProperty(
        prefix = "aws.s3",
        name = "bucket"
)
public class FilmRollController {

    private final FilmRollCommandService filmRollCommandService;
    private final PhotoUploadService photoUploadService;

    @Operation(
            summary = "필름 롤 생성",
            description = """
                    로그인 사용자의 촬영용 필름 롤을 생성합니다.
                    사용자에게 이미 촬영 또는 현상 중인 필름 롤이 있으면
                    새 필름 롤을 생성할 수 없습니다.
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
            summary = "사진 업로드 URL 발급",
            description = """
                    앱이 JPEG 원본 사진을 S3에 직접 업로드할 수 있는
                    Presigned PUT URL을 발급합니다.
                    같은 순서의 사진이 UPLOADING 상태이면
                    만료된 URL을 다시 발급할 수 있습니다.
                    """
    )
    @PostMapping("/{filmRollId}/photos/upload-url")
    public ResponseEntity<PhotoUploadUrlResponse> createPhotoUploadUrl(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long filmRollId,

            @Valid
            @RequestBody
            PhotoUploadUrlRequest request
    ) {
        return ResponseEntity.ok(
                photoUploadService.createUploadUrl(
                        authenticatedUser.userId(),
                        filmRollId,
                        request
                )
        );
    }

    @Operation(
            summary = "사진 업로드 완료 확인",
            description = """
                    S3 HeadObject로 파일의 실제 존재 여부, 크기,
                    Content-Type을 검증한 뒤 사진을 UPLOADED 상태로 전환합니다.
                    같은 요청이 반복되어도 사진 수는 한 번만 증가합니다.
                    """
    )
    @PostMapping(
            "/{filmRollId}/photos/{photoId}/complete"
    )
    public ResponseEntity<PhotoUploadCompleteResponse>
    completePhotoUpload(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long filmRollId,

            @PathVariable
            Long photoId
    ) {
        return ResponseEntity.ok(
                photoUploadService.completeUpload(
                        authenticatedUser.userId(),
                        filmRollId,
                        photoId
                )
        );
    }

    @Operation(
            summary = "필름 롤 촬영 종료",
            description = """
                    저장된 모든 사진의 S3 업로드가 완료됐는지 확인하고
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
