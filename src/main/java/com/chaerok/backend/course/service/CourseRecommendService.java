package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CoursePlaceResponse;
import com.chaerok.backend.course.dto.CourseRecommendResponse;
import com.chaerok.backend.course.dto.CourseResponse;
import com.chaerok.backend.course.exception.CourseErrorCode;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.exception.PlaceErrorCode;
import com.chaerok.backend.place.external.KakaoLocalClient;
import com.chaerok.backend.place.external.KakaoPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.exception.RegionErrorCode;
import com.chaerok.backend.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseRecommendService {

    private static final String RECOMMENDATION_TYPE_REGION = "REGION";
    private static final String RECOMMENDATION_TYPE_ANCHOR = "ANCHOR";

    private static final String KAKAO_CATEGORY_FOOD = "FD6";
    private static final String KAKAO_CATEGORY_CAFE = "CE7";

    private static final int DEFAULT_SEARCH_RADIUS = 2000;
    private static final int EXTENDED_SEARCH_RADIUS = 5000;
    private static final int MAX_REGION_ANCHOR_COUNT = 3;
    private static final int MAX_COURSE_PLACE_COUNT = 3;

    private final RegionRepository regionRepository;
    private final PlaceRepository placeRepository;
    private final KakaoLocalClient kakaoLocalClient;

    public CourseRecommendResponse recommendCourses(
            Long regionId,
            Long anchorPlaceId
    ) {
        validateRegion(regionId);

        List<Place> representativePlaces =
                placeRepository.findByRegionIdAndRepresentativeTrue(regionId);

        if (anchorPlaceId != null) {
            Place anchor = findAnchor(regionId, anchorPlaceId);

            CourseResponse course = createCourse(anchor, representativePlaces);

            return new CourseRecommendResponse(
                    regionId,
                    RECOMMENDATION_TYPE_ANCHOR,
                    anchor.getId(),
                    isCompleteCourse(course) ? List.of(course) : List.of()
            );
        }

        List<Place> anchors = findRegionAnchors(representativePlaces);

        List<AnchorCourse> validCourses = anchors.stream()
                .map(anchor -> new AnchorCourse(
                        anchor,
                        createCourse(anchor, representativePlaces)
                ))
                .filter(anchorCourse -> isCompleteCourse(anchorCourse.course()))
                .toList();

        List<CourseResponse> courses = validCourses.stream()
                .map(AnchorCourse::course)
                .toList();

        Long firstAnchorPlaceId = validCourses.isEmpty()
                ? null
                : validCourses.get(0).anchor().getId();

        return new CourseRecommendResponse(
                regionId,
                RECOMMENDATION_TYPE_REGION,
                firstAnchorPlaceId,
                courses
        );
    }

    private boolean isCompleteCourse(CourseResponse course) {
        return course.places().size() == MAX_COURSE_PLACE_COUNT
                && containsCategoryGroup(course.places(), PlaceCategoryGroup.TOURISM)
                && containsCategoryGroup(course.places(), PlaceCategoryGroup.FOOD)
                && containsCategoryGroup(course.places(), PlaceCategoryGroup.CAFE_DESSERT);
    }

    private void validateRegion(Long regionId) {
        if (!regionRepository.existsById(regionId)) {
            throw new BusinessException(
                    RegionErrorCode.REGION_NOT_FOUND
            );
        }
    }

    private Place findAnchor(
            Long regionId,
            Long anchorPlaceId
    ) {
        Place anchor = placeRepository.findById(anchorPlaceId)
                .orElseThrow(() ->
                        new BusinessException(
                                PlaceErrorCode.PLACE_NOT_FOUND
                        )
                );

        if (!anchor.getRegion().getId().equals(regionId)) {
            throw new BusinessException(
                    CourseErrorCode.INVALID_ANCHOR_REGION
            );
        }

        if (!anchor.isRepresentative()) {
            throw new BusinessException(
                    CourseErrorCode.NON_REPRESENTATIVE_ANCHOR
            );
        }

        if (anchor.getCategoryGroup() != PlaceCategoryGroup.TOURISM) {
            throw new BusinessException(
                    CourseErrorCode.INVALID_ANCHOR_CATEGORY
            );
        }

        if (!hasCoordinate(anchor)) {
            throw new BusinessException(
                    CourseErrorCode.ANCHOR_COORDINATE_MISSING
            );
        }

        return anchor;
    }

    private List<Place> findRegionAnchors(List<Place> representativePlaces) {
        return representativePlaces.stream()
                .filter(this::hasCoordinate)
                .filter(place -> place.getCategoryGroup() == PlaceCategoryGroup.TOURISM)
                .sorted(Comparator
                        .comparingInt(this::getAnchorPriority)
                        .thenComparing(Place::getId))
                .limit(MAX_REGION_ANCHOR_COUNT)
                .toList();
    }

    private CourseResponse createCourse(
            Place anchor,
            List<Place> representativePlaces
    ) {
        List<CoursePlaceResponse> places = new ArrayList<>();

        places.add(CoursePlaceResponse.fromPlace(anchor));

        findFirstFoodCandidate(anchor)
                .map(CoursePlaceResponse::fromKakaoFood)
                .or(() -> findFallbackPlace(
                        anchor,
                        representativePlaces,
                        PlaceCategoryGroup.FOOD
                ).map(CoursePlaceResponse::fromPlace))
                .ifPresent(places::add);

        findFirstCafeCandidate(anchor)
                .map(CoursePlaceResponse::fromKakaoCafe)
                .or(() -> findFallbackPlace(
                        anchor,
                        representativePlaces,
                        PlaceCategoryGroup.CAFE_DESSERT
                ).map(CoursePlaceResponse::fromPlace))
                .ifPresent(places::add);

        return new CourseResponse(
                createCourseTitle(anchor),
                calculateScore(anchor, places),
                places
        );
    }

    private java.util.Optional<KakaoPlaceItem> findFirstFoodCandidate(Place anchor) {
        return searchKakaoCandidates(anchor, KAKAO_CATEGORY_FOOD).stream()
                .findFirst();
    }

    private java.util.Optional<KakaoPlaceItem> findFirstCafeCandidate(Place anchor) {
        return searchKakaoCandidates(anchor, KAKAO_CATEGORY_CAFE).stream()
                .findFirst();
    }

    private List<KakaoPlaceItem> searchKakaoCandidates(
            Place anchor,
            String categoryGroupCode
    ) {
        List<KakaoPlaceItem> candidates = filterCandidatesByRegion(
                kakaoLocalClient.searchPlacesByCategory(
                        categoryGroupCode,
                        anchor.getLongitude(),
                        anchor.getLatitude(),
                        DEFAULT_SEARCH_RADIUS
                ),
                anchor
        );

        if (!candidates.isEmpty()) {
            return candidates;
        }

        return filterCandidatesByRegion(
                kakaoLocalClient.searchPlacesByCategory(
                        categoryGroupCode,
                        anchor.getLongitude(),
                        anchor.getLatitude(),
                        EXTENDED_SEARCH_RADIUS
                ),
                anchor
        );
    }

    private List<KakaoPlaceItem> filterCandidatesByRegion(
            List<KakaoPlaceItem> candidates,
            Place anchor
    ) {
        String cityCountyName = anchor.getRegion().getCityCountyName();

        if (!StringUtils.hasText(cityCountyName)) {
            return List.of();
        }

        return candidates.stream()
                .filter(candidate -> isSameRegion(candidate, cityCountyName))
                .toList();
    }

    private boolean isSameRegion(
            KakaoPlaceItem candidate,
            String cityCountyName
    ) {
        return StringUtils.hasText(candidate.addressName())
                && candidate.addressName().contains(cityCountyName);
    }

    private java.util.Optional<Place> findFallbackPlace(
            Place anchor,
            List<Place> representativePlaces,
            PlaceCategoryGroup categoryGroup
    ) {
        return representativePlaces.stream()
                .filter(this::hasCoordinate)
                .filter(place -> !place.getId().equals(anchor.getId()))
                .filter(place -> place.getCategoryGroup() == categoryGroup)
                .min(Comparator.comparingDouble(
                        place -> calculateDistanceMeters(anchor, place)
                ));
    }

    private String createCourseTitle(Place anchor) {
        return anchor.getTitle() + " 중심 소도시 탐방 코스";
    }

    private double calculateScore(
            Place anchor,
            List<CoursePlaceResponse> places
    ) {
        double score = 60.0;

        if (places.size() >= MAX_COURSE_PLACE_COUNT) {
            score += 10.0;
        }

        if (containsCategoryGroup(places, PlaceCategoryGroup.FOOD)) {
            score += 10.0;
        }

        if (containsCategoryGroup(places, PlaceCategoryGroup.CAFE_DESSERT)) {
            score += 10.0;
        }

        if (isHeritageAnchor(anchor)) {
            score += 10.0;
        }

        return Math.min(score, 100.0);
    }

    private boolean containsCategoryGroup(
            List<CoursePlaceResponse> places,
            PlaceCategoryGroup categoryGroup
    ) {
        return places.stream()
                .anyMatch(place -> categoryGroup.name().equals(place.categoryGroup()));
    }

    private boolean isHeritageAnchor(Place anchor) {
        return anchor.getCategoryDetail() == PlaceCategoryDetail.HERITAGE;
    }

    private int getAnchorPriority(Place place) {
        PlaceCategoryDetail categoryDetail = place.getCategoryDetail();

        if (categoryDetail == PlaceCategoryDetail.HERITAGE) {
            return 1;
        }

        if (categoryDetail == PlaceCategoryDetail.NATURE) {
            return 2;
        }

        if (categoryDetail == PlaceCategoryDetail.WALK) {
            return 3;
        }

        if (categoryDetail == PlaceCategoryDetail.MARKET) {
            return 4;
        }

        return 5;
    }

    private boolean hasCoordinate(Place place) {
        return place.getLatitude() != null && place.getLongitude() != null;
    }

    private double calculateDistanceMeters(
            Place from,
            Place to
    ) {
        return calculateDistanceMeters(
                from.getLatitude(),
                from.getLongitude(),
                to.getLatitude(),
                to.getLongitude()
        );
    }

    private double calculateDistanceMeters(
            BigDecimal latitude1,
            BigDecimal longitude1,
            BigDecimal latitude2,
            BigDecimal longitude2
    ) {
        double earthRadiusMeters = 6371000.0;

        double lat1 = Math.toRadians(latitude1.doubleValue());
        double lat2 = Math.toRadians(latitude2.doubleValue());
        double deltaLat = Math.toRadians(latitude2.subtract(latitude1).doubleValue());
        double deltaLon = Math.toRadians(longitude2.subtract(longitude1).doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusMeters * c;
    }

    private record AnchorCourse(
            Place anchor,
            CourseResponse course
    ) {
    }
}