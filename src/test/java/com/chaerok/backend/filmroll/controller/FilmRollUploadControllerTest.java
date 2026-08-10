package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.photo.dto.PhotoUploadCompleteResponse;
import com.chaerok.backend.photo.dto.PhotoUploadUrlRequest;
import com.chaerok.backend.photo.dto.PhotoUploadUrlResponse;
import com.chaerok.backend.photo.service.PhotoUploadService;
import com.chaerok.backend.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollUploadControllerTest {

    @Mock
    private PhotoUploadService photoUploadService;

    @Test
    @DisplayName("사진 업로드 URL 발급 결과를 반환한다")
    void createPhotoUploadUrl() {
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(1L, UserRole.USER);
        PhotoUploadUrlRequest request =
                new PhotoUploadUrlRequest(
                        1,
                        "image/jpeg",
                        1024L,
                        LocalDateTime.of(
                                2026,
                                8,
                                5,
                                19,
                                0
                        )
                );
        PhotoUploadUrlResponse expected =
                new PhotoUploadUrlResponse(
                        200L,
                        100L,
                        1,
                        "users/1/rolls/100/original/1.jpg",
                        "https://example.com/upload",
                        Instant.parse(
                                "2026-08-05T10:10:00Z"
                        ),
                        Map.of(
                                "Content-Type",
                                List.of("image/jpeg")
                        )
                );

        when(photoUploadService.createUploadUrl(
                1L,
                100L,
                request
        )).thenReturn(expected);

        FilmRollUploadController controller =
                new FilmRollUploadController(
                        photoUploadService
                );

        ResponseEntity<PhotoUploadUrlResponse> response =
                controller.createPhotoUploadUrl(
                        authenticatedUser,
                        100L,
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);

        verify(photoUploadService)
                .createUploadUrl(1L, 100L, request);
    }

    @Test
    @DisplayName("사진 업로드 완료 결과를 반환한다")
    void completePhotoUpload() {
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(1L, UserRole.USER);
        PhotoUploadCompleteResponse expected =
                new PhotoUploadCompleteResponse(
                        200L,
                        100L,
                        1,
                        "UPLOADED",
                        1,
                        LocalDateTime.of(
                                2026,
                                8,
                                5,
                                19,
                                5
                        )
                );

        when(photoUploadService.completeUpload(
                1L,
                100L,
                200L
        )).thenReturn(expected);

        FilmRollUploadController controller =
                new FilmRollUploadController(
                        photoUploadService
                );

        ResponseEntity<PhotoUploadCompleteResponse> response =
                controller.completePhotoUpload(
                        authenticatedUser,
                        100L,
                        200L
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);

        verify(photoUploadService)
                .completeUpload(1L, 100L, 200L);
    }
}
