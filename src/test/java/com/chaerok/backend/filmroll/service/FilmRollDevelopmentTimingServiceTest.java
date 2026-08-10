package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.exception.FilmRollDevelopmentWaitException;
import com.chaerok.backend.filmroll.exception.FilmRollExitRequiredException;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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
                .isInstanceOf(FilmRollExitRequiredException.class);
    }

    @Test
    @DisplayName("지역 이탈 후 1시간 전에는 현상을 거부한다")
    void rejectsBeforeOneHourPasses() {
        FilmRoll filmRoll = newFilmRoll();
        filmRoll.confirmExit(LocalDateTime.now());

        assertThatThrownBy(() -> service.requireAvailable(filmRoll))
                .isInstanceOf(FilmRollDevelopmentWaitException.class);
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
