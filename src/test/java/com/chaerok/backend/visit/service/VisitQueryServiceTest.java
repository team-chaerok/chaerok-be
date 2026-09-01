package com.chaerok.backend.visit.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.visit.dto.VisitListResponse;
import com.chaerok.backend.visit.entity.Visit;
import com.chaerok.backend.visit.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitQueryServiceTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private VisitRequirementService visitRequirementService;

    @Mock
    private FilmRoll filmRoll;

    @Mock
    private Visit visit;

    @Mock
    private Place place;

    private VisitQueryService service;

    @BeforeEach
    void setUp() {
        service = new VisitQueryService(
                filmRollRepository,
                visitRepository,
                visitRequirementService
        );
    }

    @Test
    @DisplayName("방문이 없으면 빈 목록과 0/3 진행도를 반환한다")
    void emptyVisits() {
        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(filmRoll));
        when(visitRepository.findAllWithPlaceByFilmRollId(100L))
                .thenReturn(List.of());
        when(visitRequirementService.getProgress(100L))
                .thenReturn(new VisitRequirementService.Progress(
                        0,
                        3,
                        false
                ));

        VisitListResponse response = service.getVisits(1L, 100L);

        assertThat(response.filmRollId()).isEqualTo(100L);
        assertThat(response.visitedCategoryCount()).isZero();
        assertThat(response.requiredCategoryCount()).isEqualTo(3);
        assertThat(response.visitRequirementMet()).isFalse();
        assertThat(response.visits()).isEmpty();
    }

    @Test
    @DisplayName("방문 목록에는 Place 정보와 방문 당시 categoryGroup을 반환한다")
    void returnsVisitsWithProgress() {
        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(filmRoll));
        when(visitRepository.findAllWithPlaceByFilmRollId(100L))
                .thenReturn(List.of(visit));
        when(visitRequirementService.getProgress(100L))
                .thenReturn(new VisitRequirementService.Progress(
                        3,
                        3,
                        true
                ));
        when(visit.getId()).thenReturn(300L);
        when(visit.getPlace()).thenReturn(place);
        when(place.getId()).thenReturn(200L);
        when(place.getTitle()).thenReturn("공산성");
        when(visit.getCategoryGroup())
                .thenReturn(PlaceCategoryGroup.TOURISM);
        when(visit.getVisitedAt())
                .thenReturn(LocalDateTime.of(2026, 8, 7, 15, 0));

        VisitListResponse response = service.getVisits(1L, 100L);

        assertThat(response.visitRequirementMet()).isTrue();
        assertThat(response.visits()).hasSize(1);
        assertThat(response.visits().get(0).categoryGroup())
                .isEqualTo("TOURISM");
    }

    @Test
    @DisplayName("소유하지 않은 FilmRoll의 방문 목록은 조회할 수 없다")
    void rejectsUnownedFilmRoll() {
        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVisits(1L, 100L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(FilmRollErrorCode.FILM_ROLL_NOT_FOUND)
                );
    }
}
