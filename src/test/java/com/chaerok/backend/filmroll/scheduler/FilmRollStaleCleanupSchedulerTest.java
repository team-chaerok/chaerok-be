package com.chaerok.backend.filmroll.scheduler;

import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.filmroll.service.FilmRollStaleCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollStaleCleanupSchedulerTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private FilmRollStaleCleanupService cleanupService;

    @Test
    @DisplayName("stale CAPTURING 후보를 조회해 개별 만료 서비스에 전달한다")
    void expiresStaleCapturingFilmRolls() {
        FilmRollStaleCleanupScheduler scheduler =
                new FilmRollStaleCleanupScheduler(
                        filmRollRepository,
                        cleanupService
                );
        ReflectionTestUtils.setField(
                scheduler,
                "configuredStaleHours",
                168L
        );

        when(filmRollRepository.findStaleCapturingIds(
                eq(FilmRollStatus.CAPTURING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(25L, 39L));

        when(cleanupService.expireIfStale(
                eq(25L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(true);
        when(cleanupService.expireIfStale(
                eq(39L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(true);

        scheduler.expireStaleCapturingFilmRolls();

        verify(cleanupService).expireIfStale(
                eq(25L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(cleanupService).expireIfStale(
                eq(39L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("한 FilmRoll 정리 실패가 다음 후보 처리를 막지 않는다")
    void continuesAfterIndividualFailure() {
        FilmRollStaleCleanupScheduler scheduler =
                new FilmRollStaleCleanupScheduler(
                        filmRollRepository,
                        cleanupService
                );
        ReflectionTestUtils.setField(
                scheduler,
                "configuredStaleHours",
                168L
        );

        when(filmRollRepository.findStaleCapturingIds(
                eq(FilmRollStatus.CAPTURING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(25L, 54L));

        doThrow(new RuntimeException("test failure"))
                .when(cleanupService)
                .expireIfStale(
                        eq(25L),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );

        scheduler.expireStaleCapturingFilmRolls();

        verify(cleanupService).expireIfStale(
                eq(54L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }
}
