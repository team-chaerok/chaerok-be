package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollExitResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.visit.service.VisitRequirementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollExitServiceTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private VisitRequirementService visitRequirementService;

    private FilmRollExitService service;
    private FilmRoll filmRoll;

    @BeforeEach
    void setUp() {
        service = new FilmRollExitService(
                filmRollRepository,
                visitRequirementService
        );

        filmRoll = FilmRoll.create(
                mock(User.class),
                mock(Region.class),
                "gongju",
                0.8,
                1
        );
        ReflectionTestUtils.setField(filmRoll, "id", 100L);

        when(filmRollRepository.findByIdAndUserIdForUpdate(100L, 1L))
                .thenReturn(Optional.of(filmRoll));
    }

    @Test
    @DisplayName("Visit 3유형과 사진 조건이 충족되면 지역 이탈과 1시간 뒤 현상 시각을 저장한다")
    void confirmsExitAndSchedulesDevelopment() {
        filmRoll.increasePhotoCount();
        when(visitRequirementService.isSatisfied(100L))
                .thenReturn(true);

        FilmRollExitResponse response =
                service.confirmExit(1L, 100L);

        assertThat(response.filmRollId()).isEqualTo(100L);
        assertThat(response.status())
                .isEqualTo(FilmRollStatus.CAPTURING.name());
        assertThat(response.exitedAt()).isNotNull();
        assertThat(response.developAvailableAt())
                .isEqualTo(response.exitedAt().plusHours(1));
        assertThat(response.developAvailable()).isFalse();
        verify(visitRequirementService).isSatisfied(100L);
    }

    @Test
    @DisplayName("Visit 3유형이 부족하면 사진이 있어도 이탈을 기록하고 EXPIRED로 종료한다")
    void expiresWhenVisitRequirementIsNotMet() {
        filmRoll.increasePhotoCount();
        when(visitRequirementService.isSatisfied(100L))
                .thenReturn(false);

        FilmRollExitResponse response =
                service.confirmExit(1L, 100L);

        assertExpiredExit(response);
        assertThat(filmRoll.getTotalPhotoCount()).isEqualTo(1);
        verify(visitRequirementService).isSatisfied(100L);
    }

    @Test
    @DisplayName("사진이 없으면 Visit 3유형을 충족해도 이탈을 기록하고 EXPIRED로 종료한다")
    void expiresWhenPhotoRequirementIsNotMet() {
        when(visitRequirementService.isSatisfied(100L))
                .thenReturn(true);

        FilmRollExitResponse response =
                service.confirmExit(1L, 100L);

        assertExpiredExit(response);
        verify(visitRequirementService).isSatisfied(100L);
    }

    @Test
    @DisplayName("필름 롤만 생성하고 아무 활동 없이 이탈해도 EXPIRED로 정상 종료한다")
    void expiresCompletelyUnusedFilmRoll() {
        when(visitRequirementService.isSatisfied(100L))
                .thenReturn(false);

        FilmRollExitResponse response =
                service.confirmExit(1L, 100L);

        assertExpiredExit(response);
        assertThat(filmRoll.getTotalPhotoCount()).isZero();
    }

    @Test
    @DisplayName("현상 예약된 지역 이탈 확정 재요청은 기존 시각을 유지한다")
    void scheduledExitIsIdempotent() {
        filmRoll.increasePhotoCount();
        when(visitRequirementService.isSatisfied(100L))
                .thenReturn(true);

        FilmRollExitResponse first = service.confirmExit(1L, 100L);
        FilmRollExitResponse second = service.confirmExit(1L, 100L);

        assertThat(second.exitedAt()).isEqualTo(first.exitedAt());
        assertThat(second.developAvailableAt())
                .isEqualTo(first.developAvailableAt());
    }

    @Test
    @DisplayName("EXPIRED 처리된 지역 이탈 확정 재요청도 기존 시각을 유지한다")
    void expiredExitIsIdempotent() {
        when(visitRequirementService.isSatisfied(100L))
                .thenReturn(false);

        FilmRollExitResponse first = service.confirmExit(1L, 100L);
        FilmRollExitResponse second = service.confirmExit(1L, 100L);

        assertThat(second.status())
                .isEqualTo(FilmRollStatus.EXPIRED.name());
        assertThat(second.exitedAt()).isEqualTo(first.exitedAt());
        assertThat(second.developAvailableAt()).isNull();
        assertThat(second.developAvailable()).isFalse();
    }

    private void assertExpiredExit(FilmRollExitResponse response) {
        assertThat(response.status())
                .isEqualTo(FilmRollStatus.EXPIRED.name());
        assertThat(response.exitedAt()).isNotNull();
        assertThat(response.developAvailableAt()).isNull();
        assertThat(response.developAvailable()).isFalse();
        assertThat(filmRoll.isExitConfirmed()).isTrue();
    }
}
