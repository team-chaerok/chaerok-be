package com.chaerok.backend.place.service;

import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;

public class PlaceCategoryMapper {

    private static final String CAFE_CODE = "FD05";

    private static final String KAKAO_FOOD_CODE = "FD6";
    private static final String KAKAO_CAFE_CODE = "CE7";

    private PlaceCategoryMapper() {
    }

    public static boolean isSupportedTourApiCategory(
            String lclsSystm1,
            String lclsSystm3
    ) {
        String code = resolveCode(lclsSystm1, lclsSystm3);

        if (code == null) {
            return false;
        }

        return isTourism(code)
                || isMarket(code)
                || isFood(code)
                || isCafe(code);
    }

    public static PlaceCategoryGroup toGroup(
            String lclsSystm1,
            String lclsSystm3
    ) {
        String code = resolveCode(lclsSystm1, lclsSystm3);

        if (code == null) {
            return PlaceCategoryGroup.TOURISM;
        }

        if (isCafe(code)) {
            return PlaceCategoryGroup.CAFE_DESSERT;
        }

        if (isFood(code)) {
            return PlaceCategoryGroup.FOOD;
        }

        return PlaceCategoryGroup.TOURISM;
    }

    public static PlaceCategoryDetail toDetail(
            String lclsSystm1,
            String lclsSystm3
    ) {
        String code = resolveCode(lclsSystm1, lclsSystm3);

        if (code == null) {
            return null;
        }

        if (isMarket(code)) {
            return PlaceCategoryDetail.MARKET;
        }

        if (isCafe(code)) {
            return PlaceCategoryDetail.CAFE;
        }

        if (isFood(code)) {
            return PlaceCategoryDetail.RESTAURANT;
        }

        if (code.startsWith("HS")) {
            return PlaceCategoryDetail.HERITAGE;
        }

        if (code.startsWith("NA")) {
            return PlaceCategoryDetail.NATURE;
        }

        if (code.startsWith("VE")) {
            return PlaceCategoryDetail.MUSEUM;
        }

        return PlaceCategoryDetail.EXPERIENCE;
    }

    public static PlaceCategoryGroup toGroupFromKakao(
            String categoryGroupCode,
            String categoryName
    ) {
        if (isKakaoCafe(categoryGroupCode, categoryName)) {
            return PlaceCategoryGroup.CAFE_DESSERT;
        }

        if (isKakaoFood(categoryGroupCode, categoryName)) {
            return PlaceCategoryGroup.FOOD;
        }

        return PlaceCategoryGroup.TOURISM;
    }

    public static PlaceCategoryDetail toDetailFromKakao(
            String categoryGroupCode,
            String categoryName
    ) {
        String name = normalize(categoryName);

        if (containsAny(name, "문구", "생활용품", "인테리어", "소품", "잡화", "공방")) {
            return PlaceCategoryDetail.SOUVENIR_SHOP;
        }

        if (containsAny(name, "베이커리", "제과", "빵", "도넛")) {
            return PlaceCategoryDetail.BAKERY;
        }

        if (containsAny(name, "분식", "김밥", "떡볶이")) {
            return PlaceCategoryDetail.SNACK_MEAL;
        }

        if (containsAny(name, "간식", "호떡")) {
            return PlaceCategoryDetail.SNACK;
        }

        if (containsAny(name, "전통찻집", "찻집", "다방")) {
            return PlaceCategoryDetail.TEA_HOUSE;
        }

        if (containsAny(name, "디저트", "떡", "모찌", "케이크", "마카롱", "아이스크림")) {
            return PlaceCategoryDetail.DESSERT;
        }

        if (isKakaoCafe(categoryGroupCode, categoryName)) {
            return PlaceCategoryDetail.CAFE;
        }

        if (containsAny(name, "한식", "백반", "생선구이", "불고기", "갈비", "국밥")) {
            return PlaceCategoryDetail.LOCAL_FOOD;
        }

        if (isKakaoFood(categoryGroupCode, categoryName)) {
            return PlaceCategoryDetail.RESTAURANT;
        }

        return PlaceCategoryDetail.EXPERIENCE;
    }

    private static String resolveCode(
            String lclsSystm1,
            String lclsSystm3
    ) {
        if (lclsSystm3 != null && !lclsSystm3.isBlank()) {
            return lclsSystm3;
        }

        if (lclsSystm1 != null && !lclsSystm1.isBlank()) {
            return lclsSystm1;
        }

        return null;
    }

    private static boolean isMarket(String code) {
        return code.startsWith("SH06");
    }

    private static boolean isTourism(String code) {
        return code.startsWith("EX")
                || code.startsWith("EV")
                || code.startsWith("HS")
                || code.startsWith("LS")
                || code.startsWith("NA")
                || code.startsWith("VE");
    }

    private static boolean isCafe(String code) {
        return code.startsWith(CAFE_CODE);
    }

    private static boolean isFood(String code) {
        return code.startsWith("FD01")
                || code.startsWith("FD02")
                || code.startsWith("FD03")
                || code.startsWith("FD04");
    }

    private static boolean isKakaoCafe(
            String categoryGroupCode,
            String categoryName
    ) {
        return KAKAO_CAFE_CODE.equals(categoryGroupCode)
                || containsAny(
                normalize(categoryName),
                "카페",
                "커피",
                "디저트",
                "베이커리",
                "찻집"
        );
    }

    private static boolean isKakaoFood(
            String categoryGroupCode,
            String categoryName
    ) {
        return KAKAO_FOOD_CODE.equals(categoryGroupCode)
                || containsAny(
                normalize(categoryName),
                "음식점",
                "한식",
                "중식",
                "일식",
                "양식",
                "분식",
                "식당"
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private static boolean containsAny(
            String value,
            String... keywords
    ) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}