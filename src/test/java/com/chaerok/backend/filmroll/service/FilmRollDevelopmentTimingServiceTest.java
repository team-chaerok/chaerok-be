package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FilmRollDevelopmentTimingServiceTest {

    private final FilmRollDevelopmentTimingService service =
            new FilmRollDevelopmentTimingService();

    @Test
    @DisplayName("지역 이탈 전에는 현상을 거부한다")
    void rejectsBeforeExitConfirmation() {
        FilmRoll filmRoll = newFilmRoll();

        assertThatThrownBy(() -> service.requireAvailable(filmRoll))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(FilmRollErrorCode.FILM_ROLL_EXIT_REQUIRED)
                );
    }

    @Test
    @DisplayName("지역 이탈 후 1시간 전에는 현상을 거부한다")
    void rejectsBeforeOneHourPasses() {
        FilmRoll filmRoll = newFilmRoll();
        filmRoll.confirmExit(LocalDateTime.now());

        assertThatThrownBy(() -> service.requireAvailable(filmRoll))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(
                                        FilmRollErrorCode.DEVELOPMENT_WAIT_NOT_FINISHED
                                )
                );
    }

    @Test
    @DisplayName("지역 이탈 후 1시간이 지나면 현상을 허용한다")
    void allowsAfterOneHourPasses() {
        FilmRoll filmRoll = newFilmRoll();
        filmRoll.confirmExit(LocalDateTime.now().minusHours(2));

        assertThatCode(() -> service.requireAvailable(filmRoll))
                .doesNotThrowAnyException();
    }

    private FilmRoll newFilmRoll() {
        return FilmRoll.create(
                mock(User.class),
                mock(Region.class),
                "gongju",
                0.8,
                1
        );
    }
}
