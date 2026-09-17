package com.chaerok.backend.place.external;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourApiPlaceIntroItemTest {

    @Test
    @DisplayName("관광지의 이용시간과 전화번호를 반환한다")
    void mapsTourismFields() {
        TourApiPlaceIntroItem item = new TourApiPlaceIntroItem(
                "09:00~18:00",
                "041-000-0000",
                null, null, null, null, null, null, null,
                null, null, null
        );

        assertThat(item.openingHours()).isEqualTo("09:00~18:00");
        assertThat(item.phone()).isEqualTo("041-000-0000");
    }

    @Test
    @DisplayName("음식점의 영업시간과 전화번호를 반환한다")
    void mapsFoodFields() {
        TourApiPlaceIntroItem item = new TourApiPlaceIntroItem(
                null, null,
                "10:00~21:00",
                "041-111-1111",
                null, null, null, null, null,
                null, null, null
        );

        assertThat(item.openingHours()).isEqualTo("10:00~21:00");
        assertThat(item.phone()).isEqualTo("041-111-1111");
    }

    @Test
    @DisplayName("문화시설의 이용시간과 전화번호를 반환한다")
    void mapsCultureFields() {
        TourApiPlaceIntroItem item = new TourApiPlaceIntroItem(
                null, null, null, null,
                "09:00~17:00",
                "041-222-2222",
                null, null, null,
                null, null, null
        );

        assertThat(item.openingHours()).isEqualTo("09:00~17:00");
        assertThat(item.phone()).isEqualTo("041-222-2222");
    }

    @Test
    @DisplayName("레포츠 시설의 이용시간과 전화번호를 반환한다")
    void mapsLeportsFields() {
        TourApiPlaceIntroItem item = new TourApiPlaceIntroItem(
                null, null, null, null, null, null,
                "08:00~20:00",
                "041-333-3333",
                null,
                null, null, null
        );

        assertThat(item.openingHours()).isEqualTo("08:00~20:00");
        assertThat(item.phone()).isEqualTo("041-333-3333");
    }

    @Test
    @DisplayName("쇼핑 장소의 영업시간과 전화번호를 반환한다")
    void mapsShoppingFields() {
        TourApiPlaceIntroItem item = new TourApiPlaceIntroItem(
                null,
                "041-444-4444",
                null, null, null, null, null, null,
                "10:00~22:00",
                null, null, null
        );

        assertThat(item.openingHours()).isEqualTo("10:00~22:00");
        assertThat(item.phone()).isEqualTo("041-444-4444");
    }

    @Test
    @DisplayName("이용시간과 전화번호 정보가 없으면 null을 반환한다")
    void returnsNullWhenFieldsAreMissing() {
        TourApiPlaceIntroItem item = new TourApiPlaceIntroItem(
                null, null, null, null, null, null, null, null, null,
                null, null, null
        );

        assertThat(item.openingHours()).isNull();
        assertThat(item.phone()).isNull();
    }

    @Test
    @DisplayName("축제의 행사 시간과 주최 측 전화번호를 반환한다")
    void mapsFestivalFields() {
        TourApiPlaceIntroItem item = new TourApiPlaceIntroItem(
                null, null, null, null,
                null, null, null, null,
                null,
                "18:00 ~ 21:30",
                "041-730-2971,3",
                null
        );

        assertThat(item.openingHours()).isEqualTo("18:00 ~ 21:30");
        assertThat(item.phone()).isEqualTo("041-730-2971,3");
    }
}