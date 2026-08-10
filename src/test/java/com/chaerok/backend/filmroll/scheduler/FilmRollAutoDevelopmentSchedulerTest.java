package com.chaerok.backend.filmroll.scheduler;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.filmroll.service.FilmRollDevelopmentService;
import com.chaerok.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollAutoDevelopmentSchedulerTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private FilmRollDevelopmentService developmentService;

    @Test
    @DisplayName("현상 가능 시각이 지난 필름 롤을 기존 develop 흐름으로 자동 요청한다")
    void developsDueFilmRolls() {
        FilmRoll filmRoll = mock(FilmRoll.class);
        User user = mock(User.class);

        when(filmRoll.getId()).thenReturn(100L);
        when(filmRoll.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(filmRollRepository.findDueForAutoDevelopment(
                anyList(),
                any()
        )).thenReturn(List.of(filmRoll));

        FilmRollAutoDevelopmentScheduler scheduler =
                new FilmRollAutoDevelopmentScheduler(
                        filmRollRepository,
                        developmentService
                );

        scheduler.developDueFilmRolls();

        verify(developmentService).develop(1L, 100L);
    }
}
