package com.chaerok.backend.place.dto;

import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.service.PlaceCategoryMapper;

import java.math.BigDecimal;

public record PlaceListResponse(
        Long id,
        String tourContentId,
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
                place.getTourContentId(),
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

    public static PlaceListResponse from(Place place, TourApiPlaceItem item) {
        return new PlaceListResponse(
                place.getId(),
                place.getTourContentId(),
                valueOrFallback(item.title(), place.getTitle()),
                valueOrFallback(item.address(), place.getAddress()),
                toBigDecimalOrFallback(item.latitude(), place.getLatitude()),
                toBigDecimalOrFallback(item.longitude(), place.getLongitude()),
                valueOrFallback(item.firstImageUrl(), place.getFirstImageUrl()),
                place.getCategoryGroup(),
                place.getCategoryDetail(),
                place.isRepresentative()
        );
    }

    public static PlaceListResponse fromTourApi(TourApiPlaceItem item) {
        return new PlaceListResponse(
                null,
                item.contentId(),
                item.title(),
                item.address(),
                toBigDecimal(item.latitude()),
                toBigDecimal(item.longitude()),
                item.firstImageUrl(),
                PlaceCategoryMapper.toGroup(item.lclsSystm1(), item.lclsSystm3()),
                PlaceCategoryMapper.toDetail(item.lclsSystm1(), item.lclsSystm3()),
                false
        );
    }

    private static String valueOrFallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    private static BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return new BigDecimal(value);
    }

    private static BigDecimal toBigDecimalOrFallback(String value, BigDecimal fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return new BigDecimal(value);
    }
}