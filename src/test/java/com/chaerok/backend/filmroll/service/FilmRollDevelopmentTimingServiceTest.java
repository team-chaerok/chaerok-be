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
import static org.mockito.Mockito.when;

class FilmRollDevelopmentTimingServiceTest {

    private final FilmRollDevelopmentTimingService service =
            new FilmRollDevelopmentTimingService();

    @Test
    @DisplayName("吏???댄깉 ?꾩뿉???꾩긽??嫄곕??쒕떎")
    void rejectsBeforeExitConfirmation() {
        FilmRoll filmRoll = newFilmRoll(mock(User.class));

        assertThatThrownBy(() -> service.requireAvailable(filmRoll))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(
                                        FilmRollErrorCode.FILM_ROLL_EXIT_REQUIRED
                                )
                );
    }

    @Test
    @DisplayName("?ъ궗??怨꾩젙??吏???댄깉 ?꾩뿉???꾩긽??嫄곕??쒕떎")
    void reviewUserStillRequiresExitConfirmation() {
        User user = mock(User.class);
        when(user.isReviewMode()).thenReturn(true);
        FilmRoll filmRoll = newFilmRoll(user);

        assertThatThrownBy(() -> service.requireAvailable(filmRoll))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(
                                        FilmRollErrorCode.FILM_ROLL_EXIT_REQUIRED
                                )
                );
    }

    @Test
    @DisplayName("?쇰컲 ?ъ슜?먮뒗 吏???댄깉 ??1?쒓컙 ???꾩긽??嫄곕??쒕떎")
    void rejectsNormalUserBeforeOneHourPasses() {
        FilmRoll filmRoll = newFilmRoll(mock(User.class));
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
    @DisplayName("?ъ궗??怨꾩젙? 湲곗〈 1?쒓컙 ?ㅼ?以꾩쓣 ?좎??섎㈃???湲?寃?щ쭔 硫댁젣?쒕떎")
    void allowsReviewUserImmediatelyAfterExit() {
        User user = mock(User.class);
        when(user.isReviewMode()).thenReturn(true);

        FilmRoll filmRoll = newFilmRoll(user);
        LocalDateTime exitedAt = LocalDateTime.now();
        filmRoll.confirmExit(exitedAt);

        assertThat(filmRoll.getDevelopAvailableAt())
                .isEqualTo(
                        exitedAt.plusHours(
                                FilmRoll.DEVELOPMENT_DELAY_HOURS
                        )
                );
        assertThat(filmRoll.isDevelopmentAvailable(exitedAt)).isFalse();

        assertThatCode(() -> service.requireAvailable(filmRoll))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("?쇰컲 ?ъ슜?먮뒗 吏???댄깉 ??1?쒓컙??吏?섎㈃ ?꾩긽???덉슜?쒕떎")
    void allowsNormalUserAfterOneHourPasses() {
        FilmRoll filmRoll = newFilmRoll(mock(User.class));
        filmRoll.confirmExit(LocalDateTime.now().minusHours(2));

        assertThatCode(() -> service.requireAvailable(filmRoll))
                .doesNotThrowAnyException();
    }

    private FilmRoll newFilmRoll(User user) {
        return FilmRoll.create(
                user,
                mock(Region.class),
                "gongju",
                0.8,
                1
        );
    }
}