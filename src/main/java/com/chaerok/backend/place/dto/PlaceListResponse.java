package com.chaerok.backend.place.dto;

import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;

import java.math.BigDecimal;

public record PlaceListResponse(
        Long id,
        String title,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String firstImageUrl,
        PlaceCategoryGroup categoryGroup,
        PlaceCategoryDetail categoryDetail,
        boolean isRepresentative
) {

    public static PlaceListResponse from(Place place) {
        return new PlaceListResponse(
                place.getId(),
                place.getTitle(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getFirstImageUrl(),
                place.getCategoryGroup(),
                place.getCategoryDetail(),
                place.isRepresentative()
        );
    }
}