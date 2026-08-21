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
        assertThat(PlaceCategoryMapper.toGroup("EX", "EX010100"))
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toGroup("EV", "EV010100"))
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toGroup("HS", "HS010100"))
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toGroup("LS", "LS010100"))
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toGroup("NA", "NA010100"))
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toGroup("VE", "VE010100"))
                .isEqualTo(PlaceCategoryGroup.TOURISM);
    }

    @Test
    @DisplayName("음식점 분류 코드는 FOOD로 매핑된다")
    void foodCodesToGroup() {
        assertThat(PlaceCategoryMapper.toGroup("FD", "FD010100"))
                .isEqualTo(PlaceCategoryGroup.FOOD);
        assertThat(PlaceCategoryMapper.toGroup("FD", "FD020100"))
                .isEqualTo(PlaceCategoryGroup.FOOD);
        assertThat(PlaceCategoryMapper.toGroup("FD", "FD030100"))
                .isEqualTo(PlaceCategoryGroup.FOOD);
        assertThat(PlaceCategoryMapper.toGroup("FD", "FD040100"))
                .isEqualTo(PlaceCategoryGroup.FOOD);
    }

    @Test
    @DisplayName("카페/찻집 분류 코드는 CAFE_DESSERT로 매핑된다")
    void cafeCodeToGroup() {
        assertThat(PlaceCategoryMapper.toGroup("FD", "FD050100"))
                .isEqualTo(PlaceCategoryGroup.CAFE_DESSERT);
    }

    @Test
    @DisplayName("시장 분류 코드는 TOURISM으로 매핑된다")
    void marketCodeToGroup() {
        assertThat(PlaceCategoryMapper.toGroup("SH", "SH060100"))
                .isEqualTo(PlaceCategoryGroup.TOURISM);
    }

    @Test
    @DisplayName("관광지 분류 코드는 세부 유형으로 매핑된다")
    void tourismCodesToDetail() {
        assertThat(PlaceCategoryMapper.toDetail("EX", "EX010100"))
                .isEqualTo(PlaceCategoryDetail.EXPERIENCE);
        assertThat(PlaceCategoryMapper.toDetail("EV", "EV010100"))
                .isEqualTo(PlaceCategoryDetail.EXPERIENCE);
        assertThat(PlaceCategoryMapper.toDetail("HS", "HS010100"))
                .isEqualTo(PlaceCategoryDetail.HERITAGE);
        assertThat(PlaceCategoryMapper.toDetail("LS", "LS010100"))
                .isEqualTo(PlaceCategoryDetail.EXPERIENCE);
        assertThat(PlaceCategoryMapper.toDetail("NA", "NA010100"))
                .isEqualTo(PlaceCategoryDetail.NATURE);
        assertThat(PlaceCategoryMapper.toDetail("VE", "VE010100"))
                .isEqualTo(PlaceCategoryDetail.MUSEUM);
    }

    @Test
    @DisplayName("음식점 분류 코드는 RESTAURANT로 매핑된다")
    void foodCodesToDetail() {
        assertThat(PlaceCategoryMapper.toDetail("FD", "FD010100"))
                .isEqualTo(PlaceCategoryDetail.RESTAURANT);
        assertThat(PlaceCategoryMapper.toDetail("FD", "FD020100"))
                .isEqualTo(PlaceCategoryDetail.RESTAURANT);
        assertThat(PlaceCategoryMapper.toDetail("FD", "FD030100"))
                .isEqualTo(PlaceCategoryDetail.RESTAURANT);
        assertThat(PlaceCategoryMapper.toDetail("FD", "FD040100"))
                .isEqualTo(PlaceCategoryDetail.RESTAURANT);
    }

    @Test
    @DisplayName("카페/찻집 분류 코드는 CAFE로 매핑된다")
    void cafeCodeToDetail() {
        assertThat(PlaceCategoryMapper.toDetail("FD", "FD050100"))
                .isEqualTo(PlaceCategoryDetail.CAFE);
    }

    @Test
    @DisplayName("시장 분류 코드는 MARKET으로 매핑된다")
    void marketCodeToDetail() {
        assertThat(PlaceCategoryMapper.toDetail("SH", "SH060100"))
                .isEqualTo(PlaceCategoryDetail.MARKET);
    }

    @Test
    @DisplayName("채록에서 사용하는 TourAPI 분류 코드는 지원 대상으로 판단한다")
    void supportedTourApiCategories() {
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("EX", "EX010100"))
                .isTrue();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("EV", "EV010100"))
                .isTrue();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("HS", "HS010100"))
                .isTrue();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("LS", "LS010100"))
                .isTrue();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("NA", "NA010100"))
                .isTrue();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("VE", "VE010100"))
                .isTrue();

        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("SH", "SH060100"))
                .isTrue();

        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("FD", "FD010100"))
                .isTrue();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("FD", "FD020100"))
                .isTrue();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("FD", "FD030100"))
                .isTrue();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("FD", "FD040100"))
                .isTrue();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("FD", "FD050100"))
                .isTrue();
    }

    @Test
    @DisplayName("채록에서 사용하지 않는 TourAPI 분류 코드는 지원 대상에서 제외한다")
    void unsupportedTourApiCategories() {
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("SH", "SH010100"))
                .isFalse();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("AC", "AC010100"))
                .isFalse();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory("FD", "FD060100"))
                .isFalse();
        assertThat(PlaceCategoryMapper.isSupportedTourApiCategory(null, null))
                .isFalse();
    }

    @Test
    @DisplayName("lclsSystm3가 없으면 lclsSystm1 기준으로 매핑된다")
    void fallbackToLclsSystm1() {
        assertThat(PlaceCategoryMapper.toGroup("HS", null))
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toDetail("HS", null))
                .isEqualTo(PlaceCategoryDetail.HERITAGE);
    }

    @Test
    @DisplayName("분류 코드가 없으면 TOURISM과 null 세부 유형을 반환한다")
    void nullCode() {
        assertThat(PlaceCategoryMapper.toGroup(null, null))
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(PlaceCategoryMapper.toDetail(null, null))
                .isNull();
    }

    @Test
    @DisplayName("Kakao 카페 카테고리를 CAFE_DESSERT/CAFE로 매핑한다")
    void mapKakaoCafe() {
        PlaceCategoryGroup group =
                PlaceCategoryMapper.toGroupFromKakao(
                        "CE7",
                        "음식점 > 카페"
                );

        PlaceCategoryDetail detail =
                PlaceCategoryMapper.toDetailFromKakao(
                        "CE7",
                        "음식점 > 카페"
                );

        assertThat(group)
                .isEqualTo(PlaceCategoryGroup.CAFE_DESSERT);
        assertThat(detail)
                .isEqualTo(PlaceCategoryDetail.CAFE);
    }

    @Test
    @DisplayName("Kakao 음식점 카테고리를 FOOD/RESTAURANT로 매핑한다")
    void mapKakaoRestaurant() {
        PlaceCategoryGroup group =
                PlaceCategoryMapper.toGroupFromKakao(
                        "FD6",
                        "음식점 > 한식"
                );

        PlaceCategoryDetail detail =
                PlaceCategoryMapper.toDetailFromKakao(
                        "FD6",
                        "음식점 > 한식"
                );

        assertThat(group)
                .isEqualTo(PlaceCategoryGroup.FOOD);
        assertThat(detail)
                .isEqualTo(PlaceCategoryDetail.LOCAL_FOOD);
    }

    @Test
    @DisplayName("Kakao 베이커리 카테고리를 CAFE_DESSERT/BAKERY로 매핑한다")
    void mapKakaoBakery() {
        PlaceCategoryGroup group =
                PlaceCategoryMapper.toGroupFromKakao(
                        "CE7",
                        "음식점 > 카페 > 베이커리"
                );

        PlaceCategoryDetail detail =
                PlaceCategoryMapper.toDetailFromKakao(
                        "CE7",
                        "음식점 > 카페 > 베이커리"
                );

        assertThat(group)
                .isEqualTo(PlaceCategoryGroup.CAFE_DESSERT);
        assertThat(detail)
                .isEqualTo(PlaceCategoryDetail.BAKERY);
    }

    @Test
    @DisplayName("Kakao 분식 카테고리를 FOOD/SNACK_MEAL로 매핑한다")
    void mapKakaoSnackMeal() {
        PlaceCategoryGroup group =
                PlaceCategoryMapper.toGroupFromKakao(
                        "FD6",
                        "음식점 > 분식"
                );

        PlaceCategoryDetail detail =
                PlaceCategoryMapper.toDetailFromKakao(
                        "FD6",
                        "음식점 > 분식"
                );

        assertThat(group)
                .isEqualTo(PlaceCategoryGroup.FOOD);
        assertThat(detail)
                .isEqualTo(PlaceCategoryDetail.SNACK_MEAL);
    }

    @Test
    @DisplayName("Kakao 소품 관련 카테고리를 TOURISM/SOUVENIR_SHOP으로 매핑한다")
    void mapKakaoSouvenirShop() {
        PlaceCategoryGroup group =
                PlaceCategoryMapper.toGroupFromKakao(
                        "",
                        "쇼핑 > 생활용품 > 소품"
                );

        PlaceCategoryDetail detail =
                PlaceCategoryMapper.toDetailFromKakao(
                        "",
                        "쇼핑 > 생활용품 > 소품"
                );

        assertThat(group)
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(detail)
                .isEqualTo(PlaceCategoryDetail.SOUVENIR_SHOP);
    }
}