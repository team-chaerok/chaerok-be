package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.filmroll.dto.FilmRollDevelopmentResponse;
import com.chaerok.backend.filmroll.service.FilmRollDevelopmentService;
import com.chaerok.backend.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollDevelopmentControllerTest {

    @Mock
    private FilmRollDevelopmentService developmentService;

    @Test
    @DisplayName("현상 요청을 202로 반환하고 내부 작업 정보는 노출하지 않는다")
    void develop() {
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(1L, UserRole.USER);
        LocalDateTime requestedAt =
                LocalDateTime.of(2026, 8, 5, 18, 20);
        FilmRollDevelopmentResponse expected =
                new FilmRollDevelopmentResponse(
                        100L,
                        "QUEUED",
                        3,
                        requestedAt
                );

        when(developmentService.develop(1L, 100L))
                .thenReturn(expected);

        FilmRollDevelopmentController controller =
                new FilmRollDevelopmentController(
                        developmentService
                );

        ResponseEntity<FilmRollDevelopmentResponse> response =
                controller.develop(
                        authenticatedUser,
                        100L
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isSameAs(expected);
        verify(developmentService).develop(1L, 100L);
    }
}
