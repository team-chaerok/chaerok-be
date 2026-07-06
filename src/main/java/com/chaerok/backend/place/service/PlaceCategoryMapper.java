package com.chaerok.backend.place.service;

import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;

public class PlaceCategoryMapper {

    private static final String CAFE_CODE = "FD05";
    private static final String MARKET_CODE = "SH06";

    private PlaceCategoryMapper() {
    }

    public static PlaceCategoryGroup toGroup(String lclsSystm1, String lclsSystm3) {
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

    public static PlaceCategoryDetail toDetail(String lclsSystm1, String lclsSystm3) {
        String code = resolveCode(lclsSystm1, lclsSystm3);

        if (code == null) {
            return null;
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

        if (isMarket(code)) {
            return PlaceCategoryDetail.MARKET;
        }

        if (code.startsWith("VE")) {
            return PlaceCategoryDetail.MUSEUM;
        }

        return PlaceCategoryDetail.EXPERIENCE;
    }

    private static String resolveCode(String lclsSystm1, String lclsSystm3) {
        if (lclsSystm3 != null && !lclsSystm3.isBlank()) {
            return lclsSystm3;
        }

        if (lclsSystm1 != null && !lclsSystm1.isBlank()) {
            return lclsSystm1;
        }

        return null;
    }

    private static boolean isCafe(String code) {
        return code.startsWith(CAFE_CODE);
    }

    private static boolean isFood(String code) {
        return code.startsWith("FD") && !isCafe(code);
    }

    private static boolean isMarket(String code) {
        return code.startsWith(MARKET_CODE);
    }
}