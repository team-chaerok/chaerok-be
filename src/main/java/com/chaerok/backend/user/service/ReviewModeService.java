package com.chaerok.backend.user.service;

import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import com.chaerok.backend.user.config.ReviewModeProperties;
import com.chaerok.backend.user.dto.ReviewModeResponse;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewModeService {

    private static final List<PlaceCategoryGroup> REQUIRED_CATEGORIES =
            List.of(
                    PlaceCategoryGroup.TOURISM,
                    PlaceCategoryGroup.FOOD,
                    PlaceCategoryGroup.CAFE_DESSERT
            );

    private final UserService userService;
    private final RegionRepository regionRepository;
    private final PlaceRepository placeRepository;
    private final ReviewModeProperties properties;

    public ReviewModeResponse getReviewMode(Long userId) {
        User user = userService.findById(userId);

        if (!user.isReviewMode()) {
            return ReviewModeResponse.disabled();
        }

        Region region = loadReviewRegion();
        List<Place> orderedPlaces = loadAndValidateReviewPlaces(region);

        return ReviewModeResponse.enabled(
                region,
                orderedPlaces
        );
    }

    private Region loadReviewRegion() {
        return regionRepository
                .findByProvinceNameAndCityCountyNameAndServiceEnabledTrue(
                        properties.getProvinceName(),
                        properties.getCityCountyName()
                )
                .orElseThrow(() -> invalidConfiguration(
                        "service-enabled review region was not found: "
                                + properties.getProvinceName()
                                + " "
                                + properties.getCityCountyName()
                ));
    }

    private List<Place> loadAndValidateReviewPlaces(Region region) {
        Map<PlaceCategoryGroup, Place> placesByCategory =
                new EnumMap<>(PlaceCategoryGroup.class);

        for (String tourContentId
                : properties.getTestPlaceTourContentIds()) {
            Place place = placeRepository
                    .findByTourContentId(tourContentId)
                    .orElseThrow(() -> invalidConfiguration(
                            "review place was not found: tourContentId="
                                    + tourContentId
                    ));

            validateReviewPlace(region, place);

            Place previous = placesByCategory.put(
                    place.getCategoryGroup(),
                    place
            );
            if (previous != null) {
                throw invalidConfiguration(
                        "review place category is duplicated: category="
                                + place.getCategoryGroup()
                );
            }
        }

        if (!placesByCategory.keySet().containsAll(REQUIRED_CATEGORIES)
                || placesByCategory.size()
                != REQUIRED_CATEGORIES.size()) {
            throw invalidConfiguration(
                    "review place categories must be exactly "
                            + REQUIRED_CATEGORIES
                            + ", actual="
                            + placesByCategory.keySet()
            );
        }

        return REQUIRED_CATEGORIES.stream()
                .map(placesByCategory::get)
                .toList();
    }

    private void validateReviewPlace(
            Region region,
            Place place
    ) {
        if (place.getRegion() == null
                || !region.getId().equals(place.getRegion().getId())) {
            throw invalidConfiguration(
                    "review place belongs to another region: placeId="
                            + place.getId()
            );
        }

        if (!REQUIRED_CATEGORIES.contains(place.getCategoryGroup())) {
            throw invalidConfiguration(
                    "unsupported review place category: placeId="
                            + place.getId()
                            + ", category="
                            + place.getCategoryGroup()
            );
        }

        if (place.getLatitude() == null
                || place.getLongitude() == null) {
            throw invalidConfiguration(
                    "review place coordinates are missing: placeId="
                            + place.getId()
            );
        }
    }

    private BusinessException invalidConfiguration(String detail) {
        log.error("Review mode configuration invalid. {}", detail);

        return new BusinessException(
                UserErrorCode.REVIEW_MODE_CONFIGURATION_INVALID,
                detail
        );
    }
}
