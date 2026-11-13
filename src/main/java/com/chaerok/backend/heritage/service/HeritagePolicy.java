package com.chaerok.backend.heritage.service;

import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;

public final class HeritagePolicy {

    private static final String GUNGNAMJI_CONTENT_ID = "125984";

    private HeritagePolicy() {
    }

    public static boolean isHeritage(String lclsSystm1, String contentId) {
        return "HS".equals(lclsSystm1)
                || GUNGNAMJI_CONTENT_ID.equals(contentId);
    }

    public static boolean isHeritage(Place place) {
        if (place.getTourContentId() == null || place.getTourContentId().isBlank()) {
            return false;
        }

        if (isHeritage(place.getLclsSystm1(), place.getTourContentId())) {
            return true;
        }

        return (place.getLclsSystm1() == null || place.getLclsSystm1().isBlank())
                && place.getCategoryDetail() == PlaceCategoryDetail.HERITAGE;
    }
}
