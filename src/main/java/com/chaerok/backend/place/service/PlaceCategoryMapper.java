package com.chaerok.backend.place.service;

import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;

public final class PlaceCategoryMapper {

    private PlaceCategoryMapper() {
    }

    public static PlaceCategoryGroup toGroup(String lclsSystm3) {
        if (lclsSystm3 == null) {
            return PlaceCategoryGroup.TOURISM;
        }

        if (isFood(lclsSystm3)) {
            return PlaceCategoryGroup.FOOD;
        }

        if (isCafeDessert(lclsSystm3)) {
            return PlaceCategoryGroup.CAFE_DESSERT;
        }

        return PlaceCategoryGroup.TOURISM;
    }

    public static PlaceCategoryDetail toDetail(String lclsSystm3) {
        if (lclsSystm3 == null) {
            return null;
        }

        if (lclsSystm3.startsWith("FD05")) {
            return PlaceCategoryDetail.CAFE;
        }

        if (lclsSystm3.startsWith("FD")) {
            return PlaceCategoryDetail.RESTAURANT;
        }

        if (lclsSystm3.startsWith("HS")) {
            return PlaceCategoryDetail.HERITAGE;
        }

        if (lclsSystm3.startsWith("EV")) {
            return PlaceCategoryDetail.MUSEUM;
        }

        if (lclsSystm3.startsWith("NA")) {
            return PlaceCategoryDetail.NATURE;
        }

        if (lclsSystm3.startsWith("VE")) {
            return PlaceCategoryDetail.EXPERIENCE;
        }

        if (lclsSystm3.startsWith("LS")) {
            return PlaceCategoryDetail.WALK;
        }

        return PlaceCategoryDetail.EXPERIENCE;
    }

    private static boolean isFood(String lclsSystm3) {
        return lclsSystm3.startsWith("FD01")
                || lclsSystm3.startsWith("FD02")
                || lclsSystm3.startsWith("FD03")
                || lclsSystm3.startsWith("FD04");
    }

    private static boolean isCafeDessert(String lclsSystm3) {
        return lclsSystm3.startsWith("FD05");
    }
}