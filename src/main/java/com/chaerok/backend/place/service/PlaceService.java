package com.chaerok.backend.place.service;

import com.chaerok.backend.global.exception.PlaceNotFoundException;
import com.chaerok.backend.global.exception.RegionNotFoundException;
import com.chaerok.backend.place.dto.PlaceDetailResponse;
import com.chaerok.backend.place.dto.PlaceListResponse;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.external.KakaoLocalClient;
import com.chaerok.backend.place.external.KakaoPlaceItem;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private static final int TOURISM_LIMIT = 15;
    private static final int FOOD_LIMIT = 15;
    private static final int CAFE_DESSERT_LIMIT = 15;

    private static final String KAKAO_FOOD_CATEGORY = "FD6";
    private static final String KAKAO_CAFE_CATEGORY = "CE7";
    private static final List<String> EXCLUDED_TOURISM_KEYWORDS = List.of("모텔", "호텔", "리조트", "체육센터");

    private static final int KAKAO_SEARCH_RADIUS = 20_000;
    private static final double DUPLICATE_DISTANCE_METERS = 30.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final PlaceRepository placeRepository;
    private final RegionRepository regionRepository;
    private final TourApiPlaceClient tourApiPlaceClient;
    private final KakaoLocalClient kakaoLocalClient;
    private final RegionCenterProvider regionCenterProvider;

    public List<PlaceListResponse> getPlacesByRegion(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(RegionNotFoundException::new);

        List<Place> representativePlaces =
                placeRepository.findByRegionIdAndRepresentativeTrue(regionId);

        Set<String> targetContentIds = representativePlaces.stream()
                .map(Place::getTourContentId)
                .filter(contentId -> contentId != null && !contentId.isBlank())
                .collect(Collectors.toSet());

        Map<String, TourApiPlaceItem> tourApiPlaces =
                tourApiPlaceClient.getPlacesByContentIds(
                        region.getLdongRegnCd(),
                        region.getLdongSignguCd(),
                        targetContentIds
                );

        return representativePlaces.stream()
                .map(place -> toPlaceListResponse(
                        place,
                        tourApiPlaces.get(place.getTourContentId())
                ))
                .toList();
    }

    public List<PlaceListResponse> getExternalPlaces(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(RegionNotFoundException::new);

        List<TourApiPlaceItem> tourApiItems =
                tourApiPlaceClient.getPlacesByRegion(
                        region.getLdongRegnCd(),
                        region.getLdongSignguCd()
                );

        List<PlaceListResponse> results = new ArrayList<>();

        addPlacesByCategory(
                results,
                tourApiItems,
                PlaceCategoryGroup.TOURISM,
                TOURISM_LIMIT
        );

        int foodCount = addPlacesByCategory(
                results,
                tourApiItems,
                PlaceCategoryGroup.FOOD,
                FOOD_LIMIT
        );

        int cafeCount = addPlacesByCategory(
                results,
                tourApiItems,
                PlaceCategoryGroup.CAFE_DESSERT,
                CAFE_DESSERT_LIMIT
        );

        int foodShortage = FOOD_LIMIT - foodCount;
        int cafeShortage = CAFE_DESSERT_LIMIT - cafeCount;

        if (foodShortage <= 0 && cafeShortage <= 0) {
            return results;
        }

        RegionCenterProvider.RegionCenter center =
                regionCenterProvider.getCenter(region);

        if (foodShortage > 0) {
            List<KakaoPlaceItem> kakaoFoods =
                    kakaoLocalClient.searchPlacesByCategory(
                            KAKAO_FOOD_CATEGORY,
                            center.longitude(),
                            center.latitude(),
                            KAKAO_SEARCH_RADIUS
                    );

            addKakaoPlaces(
                    results,
                    kakaoFoods,
                    region,
                    foodShortage
            );
        }

        if (cafeShortage > 0) {
            List<KakaoPlaceItem> kakaoCafes =
                    kakaoLocalClient.searchPlacesByCategory(
                            KAKAO_CAFE_CATEGORY,
                            center.longitude(),
                            center.latitude(),
                            KAKAO_SEARCH_RADIUS
                    );

            addKakaoPlaces(
                    results,
                    kakaoCafes,
                    region,
                    cafeShortage
            );
        }

        return results;
    }

    private int addPlacesByCategory(
            List<PlaceListResponse> results,
            List<TourApiPlaceItem> items,
            PlaceCategoryGroup targetCategory,
            int limit
    ) {
        List<PlaceListResponse> places = items.stream()
                .filter(item -> PlaceCategoryMapper.isSupportedTourApiCategory(
                        item.lclsSystm1(),
                        item.lclsSystm3()
                ))
                .filter(item -> PlaceCategoryMapper.toGroup(
                        item.lclsSystm1(),
                        item.lclsSystm3()
                ) == targetCategory)
                .filter(item ->
                        targetCategory != PlaceCategoryGroup.TOURISM
                                || !isExcludedTourismPlace(item.title())
                )
                .limit(limit)
                .map(PlaceListResponse::fromTourApi)
                .toList();

        results.addAll(places);

        return places.size();
    }

    private void addKakaoPlaces(
            List<PlaceListResponse> results,
            List<KakaoPlaceItem> kakaoItems,
            Region region,
            int shortage
    ) {
        if (shortage <= 0) {
            return;
        }

        int addedCount = 0;

        for (KakaoPlaceItem item : kakaoItems) {
            if (addedCount >= shortage) {
                break;
            }

            if (!isInRegion(item, region)) {
                continue;
            }

            PlaceListResponse candidate =
                    PlaceListResponse.fromKakao(item);

            if (isDuplicatePlace(results, candidate)) {
                continue;
            }

            results.add(candidate);
            addedCount++;
        }
    }

    private boolean isInRegion(
            KakaoPlaceItem item,
            Region region
    ) {
        String address = item.roadAddressName();

        if (address == null || address.isBlank()) {
            address = item.addressName();
        }

        if (address == null || address.isBlank()) {
            return false;
        }

        return address.contains(region.getCityCountyName());
    }

    private boolean isDuplicatePlace(
            List<PlaceListResponse> existingPlaces,
            PlaceListResponse candidate
    ) {
        return existingPlaces.stream()
                .anyMatch(existing ->
                        isSamePlace(existing, candidate)
                );
    }

    private boolean isSamePlace(
            PlaceListResponse first,
            PlaceListResponse second
    ) {
        String firstTitle = normalizeTitle(first.title());
        String secondTitle = normalizeTitle(second.title());

        String firstAddress = normalizeAddress(first.address());
        String secondAddress = normalizeAddress(second.address());

        if (firstTitle.isBlank() || secondTitle.isBlank()) {
            return false;
        }

        // 이름과 주소가 동일한 경우
        if (firstTitle.equals(secondTitle)
                && !firstAddress.isBlank()
                && firstAddress.equals(secondAddress)) {
            return true;
        }

        // 이름이 서로 관련되지 않으면 다른 장소로 판단
        if (!isSimilarTitle(firstTitle, secondTitle)) {
            return false;
        }

        // 이름이 유사하고 좌표가 30m 이내이면 동일 장소로 판단
        return calculateDistanceMeters(
                first.latitude(),
                first.longitude(),
                second.latitude(),
                second.longitude()
        ) <= DUPLICATE_DISTANCE_METERS;
    }

    private boolean isSimilarTitle(
            String first,
            String second
    ) {
        return first.equals(second)
                || first.contains(second)
                || second.contains(first);
    }

    private String normalizeTitle(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replaceAll("[^가-힣a-zA-Z0-9]", "")
                .toLowerCase();
    }

    private String normalizeAddress(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("충청남도", "충남")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }

    private double calculateDistanceMeters(
            BigDecimal latitude1,
            BigDecimal longitude1,
            BigDecimal latitude2,
            BigDecimal longitude2
    ) {
        if (latitude1 == null
                || longitude1 == null
                || latitude2 == null
                || longitude2 == null) {
            return Double.MAX_VALUE;
        }

        double lat1 = Math.toRadians(latitude1.doubleValue());
        double lat2 = Math.toRadians(latitude2.doubleValue());

        double deltaLat = Math.toRadians(
                latitude2.doubleValue() - latitude1.doubleValue()
        );

        double deltaLon = Math.toRadians(
                longitude2.doubleValue() - longitude1.doubleValue()
        );

        double a =
                Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                        + Math.cos(lat1)
                        * Math.cos(lat2)
                        * Math.sin(deltaLon / 2)
                        * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return EARTH_RADIUS_METERS * c;
    }

    private boolean isExcludedTourismPlace(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }

        return EXCLUDED_TOURISM_KEYWORDS.stream()
                .anyMatch(title::contains);
    }

    private PlaceListResponse toPlaceListResponse(
            Place place,
            TourApiPlaceItem tourApiItem
    ) {
        if (tourApiItem == null) {
            return PlaceListResponse.from(place);
        }

        return PlaceListResponse.from(place, tourApiItem);
    }

    public PlaceDetailResponse getPlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(PlaceNotFoundException::new);

        TourApiPlaceItem tourApiItem =
                tourApiPlaceClient.getPlaceDetail(
                        place.getTourContentId()
                );

        if (tourApiItem == null) {
            return PlaceDetailResponse.from(place);
        }

        return PlaceDetailResponse.from(place, tourApiItem);
    }
}