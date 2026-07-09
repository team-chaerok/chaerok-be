package com.chaerok.backend.place.dto;

import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.KakaoPlaceItem;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceSearchResponseTest {

    @Test
    @DisplayName("TourAPI 장소 응답을 장소 검색 응답으로 변환한다")
    void fromTourApi() {
        TourApiPlaceItem item = new TourApiPlaceItem(
                "1001",
                "공산성",
                "충청남도 공주시 웅진로 280",
                "36.4623000",
                "127.1248000",
                "https://example.com/image.jpg",
                "44",
                "150",
                "HS",
                "HS01",
                "HS010100",
                "공산성은 백제 시대의 대표적인 역사 유적지입니다."
        );

        PlaceSearchResponse response = PlaceSearchResponse.fromTourApi(item);

        assertThat(response.id()).isNull();
        assertThat(response.tourContentId()).isEqualTo("1001");
        assertThat(response.kakaoPlaceId()).isNull();
        assertThat(response.title()).isEqualTo("공산성");
        assertThat(response.address()).isEqualTo("충청남도 공주시 웅진로 280");
        assertThat(response.latitude()).isEqualByComparingTo(new BigDecimal("36.4623000"));
        assertThat(response.longitude()).isEqualByComparingTo(new BigDecimal("127.1248000"));
        assertThat(response.firstImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(response.categoryGroup()).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(response.categoryDetail()).isEqualTo(PlaceCategoryDetail.HERITAGE);
        assertThat(response.source()).isEqualTo(PlaceSource.TOUR_API);
    }

    @Test
    @DisplayName("Kakao 장소 응답을 장소 검색 응답으로 변환한다")
    void fromKakao() {
        KakaoPlaceItem item = new KakaoPlaceItem(
                "12345",
                "해미호떡",
                "음식점 > 간식",
                "FD6",
                "음식점",
                "충남 서산시 해미면 읍내리 1",
                "충남 서산시 해미면 남문2로 1",
                "126.5441234",
                "36.7135678",
                "https://place.map.kakao.com/12345"
        );

        PlaceSearchResponse response = PlaceSearchResponse.fromKakao(item);

        assertThat(response.id()).isNull();
        assertThat(response.tourContentId()).isNull();
        assertThat(response.kakaoPlaceId()).isEqualTo("12345");
        assertThat(response.title()).isEqualTo("해미호떡");
        assertThat(response.address()).isEqualTo("충남 서산시 해미면 남문2로 1");
        assertThat(response.latitude()).isEqualByComparingTo(new BigDecimal("36.7135678"));
        assertThat(response.longitude()).isEqualByComparingTo(new BigDecimal("126.5441234"));
        assertThat(response.firstImageUrl()).isNull();
        assertThat(response.categoryGroup()).isEqualTo(PlaceCategoryGroup.FOOD);
        assertThat(response.categoryDetail()).isEqualTo(PlaceCategoryDetail.SNACK);
        assertThat(response.source()).isEqualTo(PlaceSource.KAKAO_LOCAL);
    }
}