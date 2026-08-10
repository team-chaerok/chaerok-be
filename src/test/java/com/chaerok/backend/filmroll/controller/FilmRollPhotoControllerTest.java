package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.filmroll.dto.FilmRollPhotoListResponse;
import com.chaerok.backend.filmroll.service.FilmRollPhotoQueryService;
import com.chaerok.backend.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollPhotoControllerTest {

    @Mock
    private FilmRollPhotoQueryService photoQueryService;

    @Test
    @DisplayName("사용자가 소유한 필름 롤의 사진 목록을 반환한다")
    void getPhotos() {
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(1L, UserRole.USER);
        FilmRollPhotoListResponse expected =
                new FilmRollPhotoListResponse(
                        100L,
                        "CAPTURING",
                        0,
                        List.of()
                );

        when(photoQueryService.getPhotos(1L, 100L))
                .thenReturn(expected);

        FilmRollPhotoController controller =
                new FilmRollPhotoController(photoQueryService);

        ResponseEntity<FilmRollPhotoListResponse> response =
                controller.getPhotos(
                        authenticatedUser,
                        100L
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(photoQueryService).getPhotos(1L, 100L);
    }
}
