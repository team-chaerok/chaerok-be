package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.visit.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollStaleCleanupServiceTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private VisitRepository visitRepository;

    private FilmRollStaleCleanupService service;
    private FilmRoll filmRoll;

    private final LocalDateTime cutoff =
            LocalDateTime.of(2026, 9, 12, 12, 0);
    private final LocalDateTime expiredAt =
            LocalDateTime.of(2026, 9, 19, 12, 0);

    @BeforeEach
    void setUp() {
        service = new FilmRollStaleCleanupService(
                filmRollRepository,
                photoRepository,
                visitRepository
        );

        User user = org.mockito.Mockito.mock(User.class);
        Region region = org.mockito.Mockito.mock(Region.class);

        filmRoll = FilmRoll.create(
                user,
                region,
                UUID.randomUUID(),
                "gongju",
                1.0,
                1
        );

        ReflectionTestUtils.setField(
                filmRoll,
                "id",
                25L
        );
        ReflectionTestUtils.setField(
                filmRoll,
                "updatedAt",
                cutoff.minusMinutes(1)
        );

        when(filmRollRepository.findByIdForUpdate(25L))
                .thenReturn(Optional.of(filmRoll));
    }

    @Test
    @DisplayName("7일 이상 활동이 없는 CAPTURING 필름 롤을 EXPIRED 처리한다")
    void expiresStaleCapturingFilmRoll() {
        when(photoRepository
                .existsByFilmRollIdAndUpdatedAtGreaterThanEqual(
                        25L,
                        cutoff
                ))
                .thenReturn(false);
        when(visitRepository
                .existsByFilmRollIdAndCreatedAtGreaterThanEqual(
                        25L,
                        cutoff
                ))
                .thenReturn(false);

        boolean expired = service.expireIfStale(
                25L,
                cutoff,
                expiredAt
        );

        assertThat(expired).isTrue();
        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.EXPIRED);
        assertThat(filmRoll.getExitedAt())
                .isEqualTo(expiredAt);
        assertThat(filmRoll.getDevelopAvailableAt())
                .isNull();
    }

    @Test
    @DisplayName("최근 사진 활동이 있으면 오래된 FilmRoll 행이어도 만료하지 않는다")
    void keepsFilmRollWithRecentPhotoActivity() {
        when(photoRepository
                .existsByFilmRollIdAndUpdatedAtGreaterThanEqual(
                        25L,
                        cutoff
                ))
                .thenReturn(true);

        boolean expired = service.expireIfStale(
                25L,
                cutoff,
                expiredAt
        );

        assertThat(expired).isFalse();
        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.CAPTURING);
        assertThat(filmRoll.getExitedAt()).isNull();
        verifyNoInteractions(visitRepository);
    }

    @Test
    @DisplayName("최근 Visit 활동이 있으면 만료하지 않는다")
    void keepsFilmRollWithRecentVisitActivity() {
        when(photoRepository
                .existsByFilmRollIdAndUpdatedAtGreaterThanEqual(
                        25L,
                        cutoff
                ))
                .thenReturn(false);
        when(visitRepository
                .existsByFilmRollIdAndCreatedAtGreaterThanEqual(
                        25L,
                        cutoff
                ))
                .thenReturn(true);

        boolean expired = service.expireIfStale(
                25L,
                cutoff,
                expiredAt
        );

        assertThat(expired).isFalse();
        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.CAPTURING);
        assertThat(filmRoll.getExitedAt()).isNull();
    }

    @Test
    @DisplayName("FilmRoll 자체가 stale 기준보다 최근이면 하위 활동 조회 없이 유지한다")
    void keepsRecentlyUpdatedFilmRoll() {
        ReflectionTestUtils.setField(
                filmRoll,
                "updatedAt",
                cutoff
        );

        boolean expired = service.expireIfStale(
                25L,
                cutoff,
                expiredAt
        );

        assertThat(expired).isFalse();
        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.CAPTURING);
        verifyNoInteractions(
                photoRepository,
                visitRepository
        );
    }

    @Test
    @DisplayName("이미 이탈 처리된 FilmRoll은 stale cleanup이 건드리지 않는다")
    void skipsAlreadyExitedFilmRoll() {
        filmRoll.confirmExit(
                cutoff.minusHours(1)
        );

        boolean expired = service.expireIfStale(
                25L,
                cutoff,
                expiredAt
        );

        assertThat(expired).isFalse();
        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.CAPTURING);
        verifyNoInteractions(
                photoRepository,
                visitRepository
        );
        verify(filmRollRepository)
                .findByIdForUpdate(25L);
    }
}
