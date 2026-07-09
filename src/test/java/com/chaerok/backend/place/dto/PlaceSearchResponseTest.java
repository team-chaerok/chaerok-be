package com.chaerok.backend.place.dto;

import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.KakaoPlaceItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceSearchResponseTest {

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
        assertThat(response.source()).isEqualTo(PlaceSource.KAKAO_LOCAL);
    }
}