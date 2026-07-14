package com.chaerok.backend.course.dto;

import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.KakaoPlaceItem;

import java.math.BigDecimal;

public record CoursePlaceResponse(
        Long placeId,
        String externalPlaceId,
        String source,
        String title,
        String categoryGroup,
        String categoryDetail,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String placeUrl
) {

    public static CoursePlaceResponse fromPlace(Place place) {
        return new CoursePlaceResponse(
                place.getId(),
                place.getKakaoPlaceId(),
                place.getSource().name(),
                place.getTitle(),
                place.getCategoryGroup().name(),
                place.getCategoryDetail() == null
                        ? null
                        : place.getCategoryDetail().name(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                null
        );
    }

    public static CoursePlaceResponse fromKakaoFood(KakaoPlaceItem item) {
        return fromKakao(
                item,
                PlaceCategoryGroup.FOOD,
                PlaceCategoryDetail.RESTAURANT
        );
    }

    public static CoursePlaceResponse fromKakaoCafe(KakaoPlaceItem item) {
        return fromKakao(
                item,
                PlaceCategoryGroup.CAFE_DESSERT,
                PlaceCategoryDetail.CAFE
        );
    }

    private static CoursePlaceResponse fromKakao(
            KakaoPlaceItem item,
            PlaceCategoryGroup categoryGroup,
            PlaceCategoryDetail categoryDetail
    ) {
        return new CoursePlaceResponse(
                null,
                item.id(),
                PlaceSource.KAKAO_LOCAL.name(),
                item.placeName(),
                categoryGroup.name(),
                categoryDetail.name(),
                getAddress(item),
                toBigDecimal(item.y()),
                toBigDecimal(item.x()),
                item.placeUrl()
        );
    }

    private static String getAddress(KakaoPlaceItem item) {
        if (item.roadAddressName() != null
                && !item.roadAddressName().isBlank()) {
            return item.roadAddressName();
        }

        return item.addressName();
    }

    private static BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}