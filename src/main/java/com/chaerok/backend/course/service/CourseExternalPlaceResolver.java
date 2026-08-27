package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CoursePlaceSaveRequest;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.region.entity.Region;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseExternalPlaceResolver {

    private final TourApiPlaceClient tourApiPlaceClient;

    public Optional<TourApiPlaceItem> resolveTourApiPlace(
            Region region,
            CoursePlaceSaveRequest request
    ) {
        if (request.placeId() != null) {
            return Optional.empty();
        }

        if (isTourApiRequest(request)
                && hasText(request.externalPlaceId())) {
            TourApiPlaceItem item = tourApiPlaceClient.getPlaceDetail(
                    request.externalPlaceId()
            );

            if (item == null) {
                throw new IllegalArgumentException(
                        "TourAPI 장소 정보를 찾을 수 없습니다."
                );
            }

            validateRegion(region, item);

            return Optional.of(item);
        }

        List<TourApiPlaceItem> items =
                tourApiPlaceClient.searchPlacesByKeyword(
                        request.title(),
                        region.getLdongRegnCd(),
                        region.getLdongSignguCd()
                );

        return items.stream()
                .filter(item -> isSamePlace(item, request))
                .findFirst();
    }

    private void validateRegion(
            Region region,
            TourApiPlaceItem item
    ) {
        if (!region.getLdongRegnCd().equals(item.lDongRegnCd())
                || !region.getLdongSignguCd().equals(item.lDongSignguCd())) {
            throw new IllegalArgumentException(
                    "TourAPI 장소가 코스 지역과 일치하지 않습니다."
            );
        }
    }

    private boolean isTourApiRequest(
            CoursePlaceSaveRequest request
    ) {
        return PlaceSource.TOUR_API.name().equals(request.source());
    }

    private boolean isSamePlace(
            TourApiPlaceItem item,
            CoursePlaceSaveRequest request
    ) {
        String itemTitle = normalize(item.title());
        String requestTitle = normalize(request.title());

        if (!itemTitle.equals(requestTitle)) {
            return false;
        }

        if (!hasText(request.address())) {
            return true;
        }

        String itemAddress = normalize(item.address());
        String requestAddress = normalize(request.address());

        return itemAddress.contains(requestAddress)
                || requestAddress.contains(itemAddress);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\s+", "")
                .toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}