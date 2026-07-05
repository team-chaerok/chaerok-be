package com.chaerok.backend.place.service;

import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceCategoryMapperTest {

    @Test
    @DisplayName("관광지 분류 코드는 TOURISM으로 매핑된다")
    void tourismCodesToGroup() {
        assertThat(PlaceCategoryMapper.toGroup("EX", "EX010100")).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toGroup("EV", "EV010100")).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toGroup("HS", "HS010100")).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toGroup("LS", "LS010100")).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toGroup("NA", "NA010100")).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toGroup("VE", "VE010100")).isEqualTo(PlaceCategoryGroup.TOURISM);
    }

    @Test
    @DisplayName("음식점 분류 코드는 FOOD로 매핑된다")
    void foodCodesToGroup() {
        assertThat(PlaceCategoryMapper.toGroup("FD", "FD010100")).isEqualTo(PlaceCategoryGroup.FOOD);
        assertThat(PlaceCategoryMapper.toGroup("FD", "FD020100")).isEqualTo(PlaceCategoryGroup.FOOD);
        assertThat(PlaceCategoryMapper.toGroup("FD", "FD030100")).isEqualTo(PlaceCategoryGroup.FOOD);
        assertThat(PlaceCategoryMapper.toGroup("FD", "FD040100")).isEqualTo(PlaceCategoryGroup.FOOD);
    }

    @Test
    @DisplayName("카페/찻집 분류 코드는 CAFE_DESSERT로 매핑된다")
    void cafeCodeToGroup() {
        assertThat(PlaceCategoryMapper.toGroup("FD", "FD050100")).isEqualTo(PlaceCategoryGroup.CAFE_DESSERT);
    }

    @Test
    @DisplayName("시장 분류 코드는 TOURISM으로 매핑된다")
    void marketCodeToGroup() {
        assertThat(PlaceCategoryMapper.toGroup("SH", "SH060100")).isEqualTo(PlaceCategoryGroup.TOURISM);
    }

    @Test
    @DisplayName("관광지 분류 코드는 세부 유형으로 매핑된다")
    void tourismCodesToDetail() {
        assertThat(PlaceCategoryMapper.toDetail("EX", "EX010100")).isEqualTo(PlaceCategoryDetail.EXPERIENCE);
        assertThat(PlaceCategoryMapper.toDetail("EV", "EV010100")).isEqualTo(PlaceCategoryDetail.EXPERIENCE);
        assertThat(PlaceCategoryMapper.toDetail("HS", "HS010100")).isEqualTo(PlaceCategoryDetail.HERITAGE);
        assertThat(PlaceCategoryMapper.toDetail("LS", "LS010100")).isEqualTo(PlaceCategoryDetail.EXPERIENCE);
        assertThat(PlaceCategoryMapper.toDetail("NA", "NA010100")).isEqualTo(PlaceCategoryDetail.NATURE);
        assertThat(PlaceCategoryMapper.toDetail("VE", "VE010100")).isEqualTo(PlaceCategoryDetail.MUSEUM);
    }

    @Test
    @DisplayName("음식점 분류 코드는 RESTAURANT로 매핑된다")
    void foodCodesToDetail() {
        assertThat(PlaceCategoryMapper.toDetail("FD", "FD010100")).isEqualTo(PlaceCategoryDetail.RESTAURANT);
        assertThat(PlaceCategoryMapper.toDetail("FD", "FD020100")).isEqualTo(PlaceCategoryDetail.RESTAURANT);
        assertThat(PlaceCategoryMapper.toDetail("FD", "FD030100")).isEqualTo(PlaceCategoryDetail.RESTAURANT);
        assertThat(PlaceCategoryMapper.toDetail("FD", "FD040100")).isEqualTo(PlaceCategoryDetail.RESTAURANT);
    }

    @Test
    @DisplayName("카페/찻집 분류 코드는 CAFE로 매핑된다")
    void cafeCodeToDetail() {
        assertThat(PlaceCategoryMapper.toDetail("FD", "FD050100")).isEqualTo(PlaceCategoryDetail.CAFE);
    }

    @Test
    @DisplayName("시장 분류 코드는 MARKET으로 매핑된다")
    void marketCodeToDetail() {
        assertThat(PlaceCategoryMapper.toDetail("SH", "SH060100")).isEqualTo(PlaceCategoryDetail.MARKET);
    }

    @Test
    @DisplayName("lclsSystm3가 없으면 lclsSystm1 기준으로 매핑된다")
    void fallbackToLclsSystm1() {
        assertThat(PlaceCategoryMapper.toGroup("HS", null)).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toDetail("HS", null)).isEqualTo(PlaceCategoryDetail.HERITAGE);
    }

    @Test
    @DisplayName("분류 코드가 없으면 TOURISM과 null 세부 유형을 반환한다")
    void nullCode() {
        assertThat(PlaceCategoryMapper.toGroup(null, null)).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toDetail(null, null)).isNull();
    }
}