package com.chaerok.backend.visit.entity;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VisitTest {

    @Test
    @DisplayName("방문 생성 시 Place의 상위 유형을 스냅샷으로 저장한다")
    void snapshotsCategoryGroup() {
        FilmRoll filmRoll = mock(FilmRoll.class);
        Place place = mock(Place.class);

        when(place.getCategoryGroup())
                .thenReturn(PlaceCategoryGroup.FOOD);

        Visit visit = Visit.create(filmRoll, place);
        when(place.getCategoryGroup())
                .thenReturn(PlaceCategoryGroup.CAFE_DESSERT);

        assertThat(place.getCategoryGroup())
                .isEqualTo(PlaceCategoryGroup.CAFE_DESSERT);
        assertThat(visit.getFilmRoll()).isSameAs(filmRoll);
        assertThat(visit.getPlace()).isSameAs(place);
        assertThat(visit.getCategoryGroup())
                .isEqualTo(PlaceCategoryGroup.FOOD);
    }
}
