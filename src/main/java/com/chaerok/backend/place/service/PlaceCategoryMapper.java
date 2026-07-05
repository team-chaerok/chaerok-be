package com.chaerok.backend.place.service;

import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;

public final class PlaceCategoryMapper {

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

        if (isTourism(code)) {
            return PlaceCategoryGroup.TOURISM;
        }

        return PlaceCategoryGroup.TOURISM;
    }

    public static PlaceCategoryDetail toDetail(String lclsSystm1, String lclsSystm3) {
        String code = resolveCode(lclsSystm1, lclsSystm3);

        if (code == null) {
            return null;
        }

        if (code.startsWith("FD05")) {
            return PlaceCategoryDetail.CAFE;
        }

        if (code.startsWith("FD")) {
            return PlaceCategoryDetail.RESTAURANT;
        }

        if (code.startsWith("HS")) {
            return PlaceCategoryDetail.HERITAGE;
        }

        if (code.startsWith("EV")) {
            return PlaceCategoryDetail.EXPERIENCE;
        }

        if (code.startsWith("NA")) {
            return PlaceCategoryDetail.NATURE;
        }

        if (code.startsWith("VE")) {
            return PlaceCategoryDetail.MUSEUM;
        }

        if (code.startsWith("LS")) {
            return PlaceCategoryDetail.EXPERIENCE;
        }

        if (code.startsWith("EX")) {
            return PlaceCategoryDetail.EXPERIENCE;
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

    private static boolean isTourism(String code) {
        return code.startsWith("EX")
                || code.startsWith("EV")
                || code.startsWith("HS")
                || code.startsWith("LS")
                || code.startsWith("NA")
                || code.startsWith("VE");
    }

    private static boolean isFood(String code) {
        return code.startsWith("FD01")
                || code.startsWith("FD02")
                || code.startsWith("FD03")
                || code.startsWith("FD04");
    }

    private static boolean isCafe(String code) {
        return code.startsWith("FD05");
    }
}