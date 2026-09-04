package com.chaerok.backend.user.dto;

import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.region.entity.Region;

import java.math.BigDecimal;
import java.util.List;

public record ReviewModeResponse(
        boolean enabled,
        ReviewRegionResponse region,
        List<ReviewTestPlaceResponse> testPlaces
) {

    public ReviewModeResponse {
        testPlaces = testPlaces == null
                ? List.of()
                : List.copyOf(testPlaces);
    }

    public static ReviewModeResponse disabled() {
        return new ReviewModeResponse(
                false,
                null,
                List.of()
        );
    }

    public static ReviewModeResponse enabled(
            Region region,
            List<Place> orderedPlaces
    ) {
        return new ReviewModeResponse(
                true,
                ReviewRegionResponse.from(region),
                orderedPlaces.stream()
                        .map(ReviewTestPlaceResponse::from)
                        .toList()
        );
    }

    public record ReviewRegionResponse(
            Long regionId,
            String provinceName,
            String cityCountyName
    ) {
        public static ReviewRegionResponse from(Region region) {
            return new ReviewRegionResponse(
                    region.getId(),
                    region.getProvinceName(),
                    region.getCityCountyName()
            );
        }
    }

    public record ReviewTestPlaceResponse(
            Long placeId,
            String title,
            String categoryGroup,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        public static ReviewTestPlaceResponse from(Place place) {
            return new ReviewTestPlaceResponse(
                    place.getId(),
                    place.getTitle(),
                    place.getCategoryGroup().name(),
                    place.getAddress(),
                    place.getLatitude(),
                    place.getLongitude()
            );
        }
    }
}
