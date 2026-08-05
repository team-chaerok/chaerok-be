package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.filmroll.dto.FilmRollResponse;
import com.chaerok.backend.filmroll.service.FilmRollCommandService;
import com.chaerok.backend.filmroll.service.FilmRollQueryService;
import com.chaerok.backend.photo.service.PhotoUploadService;
import com.chaerok.backend.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollControllerTest {

    @Mock
    private FilmRollCommandService filmRollCommandService;

    @Mock
    private FilmRollQueryService filmRollQueryService;

    @Mock
    private PhotoUploadService photoUploadService;

    private FilmRollController controller;
    private AuthenticatedUser authenticatedUser;

    @BeforeEach
    void setUp() {
        controller = new FilmRollController(
                filmRollCommandService,
                filmRollQueryService,
                photoUploadService
        );
        authenticatedUser = new AuthenticatedUser(1L, UserRole.USER);
    }

    @Test
    @DisplayName("현재 미완료 필름 롤이 있으면 200으로 반환한다")
    void getCurrentFilmRoll() {
        FilmRollResponse response = response();

        when(filmRollQueryService.findCurrentFilmRoll(1L))
                .thenReturn(Optional.of(response));

        ResponseEntity<FilmRollResponse> result =
                controller.getCurrentFilmRoll(authenticatedUser);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    @DisplayName("현재 미완료 필름 롤이 없으면 204를 반환한다")
    void getCurrentFilmRollReturnsNoContent() {
        when(filmRollQueryService.findCurrentFilmRoll(1L))
                .thenReturn(Optional.empty());

        ResponseEntity<FilmRollResponse> result =
                controller.getCurrentFilmRoll(authenticatedUser);

        assertThat(result.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
    }

    @Test
    @DisplayName("소유한 필름 롤 상세를 200으로 반환한다")
    void getFilmRoll() {
        FilmRollResponse response = response();

        when(filmRollQueryService.getFilmRoll(1L, 100L))
                .thenReturn(response);

        ResponseEntity<FilmRollResponse> result =
                controller.getFilmRoll(authenticatedUser, 100L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }
    private FilmRollResponse response() {
        return new FilmRollResponse(
                100L,
                10L,
                "gongju_baekje_love",
                0.8,
                1,
                "CAPTURING",
                0,
                0,
                24,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

}
