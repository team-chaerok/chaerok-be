package com.chaerok.backend.place.dto;

import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.TourApiPlaceItem;

import java.math.BigDecimal;

import static com.chaerok.backend.place.service.TourApiValueMapper.toBigDecimalOrFallback;
import static com.chaerok.backend.place.service.TourApiValueMapper.valueOrFallback;

public record PlaceDetailResponse(
        Long id,
        Long regionId,
        String tourContentId,
        String title,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String firstImageUrl,
        String overview,
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
                null,
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

    public static PlaceDetailResponse from(Place place, TourApiPlaceItem item) {
        return new PlaceDetailResponse(
                place.getId(),
                place.getRegion().getId(),
                place.getTourContentId(),
                valueOrFallback(item.title(), place.getTitle()),
                valueOrFallback(item.address(), place.getAddress()),
                toBigDecimalOrFallback(item.latitude(), place.getLatitude()),
                toBigDecimalOrFallback(item.longitude(), place.getLongitude()),
                valueOrFallback(item.firstImageUrl(), place.getFirstImageUrl()),
                item.overview(),
                valueOrFallback(item.lDongRegnCd(), place.getLDongRegnCd()),
                valueOrFallback(item.lDongSignguCd(), place.getLDongSignguCd()),
                valueOrFallback(item.lclsSystm1(), place.getLclsSystm1()),
                valueOrFallback(item.lclsSystm2(), place.getLclsSystm2()),
                valueOrFallback(item.lclsSystm3(), place.getLclsSystm3()),
                place.getCategoryGroup(),
                place.getCategoryDetail(),
                place.isRepresentative(),
                place.getSource()
        );
    }
}