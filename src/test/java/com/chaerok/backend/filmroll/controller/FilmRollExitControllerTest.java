package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.filmroll.dto.FilmRollExitResponse;
import com.chaerok.backend.filmroll.service.FilmRollExitService;
import com.chaerok.backend.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollExitControllerTest {

    @Mock
    private FilmRollExitService filmRollExitService;

    @Test
    @DisplayName("인증 사용자의 지역 이탈 확정 결과를 반환한다")
    void confirmExit() {
        LocalDateTime exitedAt =
                LocalDateTime.of(2026, 8, 7, 18, 0);
        FilmRollExitResponse expected =
                new FilmRollExitResponse(
                        100L,
                        "CAPTURING",
                        exitedAt,
                        exitedAt.plusHours(1),
                        false
                );

        when(filmRollExitService.confirmExit(1L, 100L))
                .thenReturn(expected);

        FilmRollExitController controller =
                new FilmRollExitController(filmRollExitService);

        var response = controller.confirmExit(
                new AuthenticatedUser(1L, UserRole.USER),
                100L
        );

        assertThat(response.getBody()).isEqualTo(expected);
    }
}
