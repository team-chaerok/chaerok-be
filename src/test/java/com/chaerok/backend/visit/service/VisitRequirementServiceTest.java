package com.chaerok.backend.visit.service;

import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.visit.exception.VisitRequirementNotMetException;
import com.chaerok.backend.visit.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitRequirementServiceTest {

    @Mock
    private VisitRepository visitRepository;

    private VisitRequirementService service;

    @BeforeEach
    void setUp() {
        service = new VisitRequirementService(visitRepository);
    }

    @Test
    @DisplayName("방문이 없으면 0개 유형이며 조건을 충족하지 않는다")
    void noVisits() {
        when(visitRepository
                .findDistinctCategoryGroupsByFilmRollId(100L))
                .thenReturn(List.of());

        VisitRequirementService.Progress progress =
                service.getProgress(100L);

        assertThat(progress.visitedCategoryCount()).isZero();
        assertThat(progress.requiredCategoryCount()).isEqualTo(3);
        assertThat(progress.satisfied()).isFalse();
    }

    @Test
    @DisplayName("같은 유형을 여러 번 받아도 한 유형으로 계산한다")
    void duplicateCategoryCountsOnce() {
        when(visitRepository
                .findDistinctCategoryGroupsByFilmRollId(100L))
                .thenReturn(List.of(
                        PlaceCategoryGroup.TOURISM,
                        PlaceCategoryGroup.TOURISM
                ));

        VisitRequirementService.Progress progress =
                service.getProgress(100L);

        assertThat(progress.visitedCategoryCount()).isEqualTo(1);
        assertThat(progress.satisfied()).isFalse();
    }

    @Test
    @DisplayName("서로 다른 두 유형 방문은 현상 조건을 충족하지 않는다")
    void twoCategoriesAreNotEnough() {
        when(visitRepository
                .findDistinctCategoryGroupsByFilmRollId(100L))
                .thenReturn(List.of(
                        PlaceCategoryGroup.TOURISM,
                        PlaceCategoryGroup.FOOD
                ));

        VisitRequirementService.Progress progress =
                service.getProgress(100L);

        assertThat(progress.visitedCategoryCount()).isEqualTo(2);
        assertThat(progress.satisfied()).isFalse();
    }

    @Test
    @DisplayName("관광지 식당 카페를 각각 방문하면 현상 조건을 충족한다")
    void allThreeRequiredCategoriesSatisfyRequirement() {
        when(visitRepository
                .findDistinctCategoryGroupsByFilmRollId(100L))
                .thenReturn(List.of(
                        PlaceCategoryGroup.TOURISM,
                        PlaceCategoryGroup.FOOD,
                        PlaceCategoryGroup.CAFE_DESSERT
                ));

        VisitRequirementService.Progress progress =
                service.getProgress(100L);

        assertThat(progress.visitedCategoryCount()).isEqualTo(3);
        assertThat(progress.requiredCategoryCount()).isEqualTo(3);
        assertThat(progress.satisfied()).isTrue();
        assertThat(service.isSatisfied(100L)).isTrue();
    }

    @Test
    @DisplayName("세 유형이 모두 없으면 현상 조건 검증에서 거부한다")
    void requireSatisfiedRejectsIncompleteProgress() {
        when(visitRepository
                .findDistinctCategoryGroupsByFilmRollId(100L))
                .thenReturn(List.of(
                        PlaceCategoryGroup.TOURISM,
                        PlaceCategoryGroup.CAFE_DESSERT
                ));

        assertThatThrownBy(() -> service.requireSatisfied(100L))
                .isInstanceOf(VisitRequirementNotMetException.class)
                .hasMessageContaining("관광지")
                .hasMessageContaining("식당")
                .hasMessageContaining("카페");
    }
}
