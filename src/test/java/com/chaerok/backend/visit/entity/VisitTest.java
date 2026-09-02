package com.chaerok.backend.visit.entity;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VisitTest {

    @Test
    @DisplayName("방문 생성 시 Place 유형과 인증 Photo를 함께 저장한다")
    void snapshotsCategoryGroupAndKeepsPhoto() {
        FilmRoll filmRoll = mock(FilmRoll.class);
        Place place = mock(Place.class);
        Photo photo = mock(Photo.class);

        when(place.getCategoryGroup())
                .thenReturn(PlaceCategoryGroup.FOOD);

        Visit visit = Visit.create(
                filmRoll,
                place,
                photo
        );

        when(place.getCategoryGroup())
                .thenReturn(PlaceCategoryGroup.CAFE_DESSERT);

        assertThat(place.getCategoryGroup())
                .isEqualTo(PlaceCategoryGroup.CAFE_DESSERT);
        assertThat(visit.getFilmRoll()).isSameAs(filmRoll);
        assertThat(visit.getPlace()).isSameAs(place);
        assertThat(visit.getPhoto()).isSameAs(photo);
        assertThat(visit.getCategoryGroup())
                .isEqualTo(PlaceCategoryGroup.FOOD);
    }

    @Test
    @DisplayName("신규 방문은 인증 Photo 없이 생성할 수 없다")
    void rejectsMissingPhoto() {
        FilmRoll filmRoll = mock(FilmRoll.class);
        Place place = mock(Place.class);

        when(place.getCategoryGroup())
                .thenReturn(PlaceCategoryGroup.TOURISM);

        assertThatThrownBy(() ->
                Visit.create(filmRoll, place, null)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("방문 인증 사진");
    }
}