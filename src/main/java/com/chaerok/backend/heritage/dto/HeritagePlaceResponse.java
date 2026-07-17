package com.chaerok.backend.heritage.dto;

import com.chaerok.backend.place.external.TourApiPlaceItem;

public record HeritagePlaceResponse(
        Long placeId,
        String contentId,
        String title,
        String address,
        String regionName,
        boolean heritage,
        String lclsSystm1,
        String lclsSystm2,
        String lclsSystm3,
        String overview,
        String imageUrl
) {

    public static HeritagePlaceResponse of(
            Long placeId,
            String regionName,
            boolean heritage,
            TourApiPlaceItem item
    ) {
        return new HeritagePlaceResponse(
                placeId,
                item.contentId(),
                item.title(),
                item.address(),
                regionName,
                heritage,
                item.lclsSystm1(),
                item.lclsSystm2(),
                item.lclsSystm3(),
                item.overview(),
                item.firstImageUrl()
        );
    }
}