package com.chaerok.backend.place.dto;

import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;

import java.math.BigDecimal;

public record PlaceDetailResponse(
        Long id,
        Long regionId,
        String tourContentId,
        String title,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String firstImageUrl,
        String lDongRegnCd,
        String lDongSignguCd,
        String lclsSystm1,
        String lclsSystm2,
        String lclsSystm3,
        PlaceCategoryGroup categoryGroup,
        PlaceCategoryDetail categoryDetail,
        boolean isRepresentative,
        PlaceSource source
) {

    public static PlaceDetailResponse from(Place place) {
        return new PlaceDetailResponse(
                place.getId(),
                place.getRegion().getId(),
                place.getTourContentId(),
                place.getTitle(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getFirstImageUrl(),
                place.getLDongRegnCd(),
                place.getLDongSignguCd(),
                place.getLclsSystm1(),
                place.getLclsSystm2(),
                place.getLclsSystm3(),
                place.getCategoryGroup(),
                place.getCategoryDetail(),
                place.isRepresentative(),
                place.getSource()
        );
    }
}