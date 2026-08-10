package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.filmroll.dto.FilmRollResultResponse;
import com.chaerok.backend.filmroll.service.FilmRollResultService;
import com.chaerok.backend.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollResultControllerTest {

    @Mock
    private FilmRollResultService filmRollResultService;

    @Test
    @DisplayName("현상 결과를 조회하고 내부 객체 키는 응답 모델에 포함하지 않는다")
    void getResult() {
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(1L, UserRole.USER);
        FilmRollResultResponse expected =
                new FilmRollResultResponse(
                        100L,
                        "QUEUED",
                        3,
                        0,
                        List.of(),
                        null,
                        null,
                        LocalDateTime.of(2026, 8, 5, 18, 20),
                        null,
                        null,
                        null
                );

        when(filmRollResultService.getResult(1L, 100L))
                .thenReturn(expected);

        FilmRollResultController controller =
                new FilmRollResultController(
                        filmRollResultService
                );

        ResponseEntity<FilmRollResultResponse> response =
                controller.getResult(
                        authenticatedUser,
                        100L
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(filmRollResultService).getResult(1L, 100L);
    }
}
