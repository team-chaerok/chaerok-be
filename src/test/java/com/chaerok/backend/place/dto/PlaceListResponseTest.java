package com.chaerok.backend.place.dto;

import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceListResponseTest {

    @Test
    void TourAPI_HS_분류와_궁남지_예외를_판별한다() {
        assertThat(PlaceListResponse.fromTourApi(item("100", "HS")).heritage()).isTrue();
        assertThat(PlaceListResponse.fromTourApi(item("125984", "NA")).heritage()).isTrue();
        assertThat(PlaceListResponse.fromTourApi(item("100", "NA")).heritage()).isFalse();
    }

    @Test
    void 지역_목록은_DB보다_조회된_TourAPI_분류를_우선한다() {
        Place place = mock(Place.class);
        when(place.getTourContentId()).thenReturn("100");
        when(place.getLclsSystm1()).thenReturn("HS");

        assertThat(PlaceListResponse.from(place, item("100", "NA")).heritage()).isFalse();
    }

    @Test
    void TourAPI_항목이_없으면_DB_분류와_궁남지_예외를_사용한다() {
        Place heritagePlace = mock(Place.class);
        when(heritagePlace.getTourContentId()).thenReturn("100");
        when(heritagePlace.getLclsSystm1()).thenReturn("HS");
        assertThat(PlaceListResponse.from(heritagePlace).heritage()).isTrue();

        Place gungnamji = mock(Place.class);
        when(gungnamji.getTourContentId()).thenReturn("125984");
        when(gungnamji.getLclsSystm1()).thenReturn("NA");
        assertThat(PlaceListResponse.from(gungnamji).heritage()).isTrue();
    }

    private TourApiPlaceItem item(String contentId, String lclsSystm1) {
        return new TourApiPlaceItem(
                contentId, null, "장소", "주소", null, null, null,
                null, null, lclsSystm1, null, null, null
        );
    }
}
