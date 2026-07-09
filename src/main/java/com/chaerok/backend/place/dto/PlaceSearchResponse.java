package com.chaerok.backend.place.dto;

import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.KakaoPlaceItem;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.service.PlaceCategoryMapper;

import java.math.BigDecimal;

import static com.chaerok.backend.place.service.TourApiValueMapper.toBigDecimal;
import static com.chaerok.backend.place.service.TourApiValueMapper.valueOrFallback;

public record PlaceSearchResponse(
        Long id,
        String tourContentId,
        String kakaoPlaceId,
        String title,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String firstImageUrl,
        PlaceCategoryGroup categoryGroup,
        PlaceCategoryDetail categoryDetail,
        PlaceSource source
) {

    public static PlaceSearchResponse fromTourApi(TourApiPlaceItem item) {
        return new PlaceSearchResponse(
                null,
                item.contentId(),
                null,
                item.title(),
                item.address(),
                toBigDecimal(item.latitude()),
                toBigDecimal(item.longitude()),
                item.firstImageUrl(),
                PlaceCategoryMapper.toGroup(item.lclsSystm1(), item.lclsSystm3()),
                PlaceCategoryMapper.toDetail(item.lclsSystm1(), item.lclsSystm3()),
                PlaceSource.TOUR_API
        );
    }

    public static PlaceSearchResponse fromKakao(KakaoPlaceItem item) {
        return new PlaceSearchResponse(
                null,
                null,
                item.id(),
                item.placeName(),
                valueOrFallback(item.roadAddressName(), item.addressName()),
                toBigDecimal(item.y()),
                toBigDecimal(item.x()),
                null,
                PlaceCategoryMapper.toGroupFromKakao(item.categoryGroupCode(), item.categoryName()),
                PlaceCategoryMapper.toDetailFromKakao(item.categoryGroupCode(), item.categoryName()),
                PlaceSource.KAKAO_LOCAL
        );
    }
}