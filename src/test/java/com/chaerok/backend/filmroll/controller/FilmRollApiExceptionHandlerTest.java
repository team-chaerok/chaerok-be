package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.filmroll.exception.FilmRollApiExceptionHandler;
import com.chaerok.backend.filmroll.exception.FilmRollDevelopmentWaitException;
import com.chaerok.backend.filmroll.exception.FilmRollExitRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilmRollApiExceptionHandlerTest {

    private final FilmRollApiExceptionHandler handler =
            new FilmRollApiExceptionHandler();

    @Test
    @DisplayName("지역 이탈 전 현상 요청은 전용 409 오류 코드로 반환한다")
    void handlesExitRequired() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI())
                .thenReturn("/api/film-rolls/8/develop");

        var response = handler.handleFilmRollExitRequired(
                new FilmRollExitRequiredException(),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code())
                .isEqualTo("FILM_ROLL_EXIT_REQUIRED");
    }

    @Test
    @DisplayName("지역 이탈 후 1시간 전 현상 요청은 전용 409 오류 코드로 반환한다")
    void handlesDevelopmentWait() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI())
                .thenReturn("/api/film-rolls/8/develop");

        var response = handler.handleDevelopmentWait(
                new FilmRollDevelopmentWaitException(),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code())
                .isEqualTo("DEVELOPMENT_WAIT_NOT_FINISHED");
    }
}
