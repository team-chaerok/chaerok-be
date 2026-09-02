package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CoursePlaceSaveRequest;
import com.chaerok.backend.course.dto.SelectedCourseResponse;
import com.chaerok.backend.course.entity.Course;
import com.chaerok.backend.course.entity.CoursePlace;
import com.chaerok.backend.course.entity.CourseStatus;
import com.chaerok.backend.course.exception.CourseErrorCode;
import com.chaerok.backend.course.repository.CoursePlaceRepository;
import com.chaerok.backend.course.repository.CourseRepository;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.exception.PlaceErrorCode;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.place.service.PlaceCategoryMapper;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CoursePersistenceService {

    private static final int MAX_COURSE_PLACE_COUNT = 3;

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final PlaceRepository placeRepository;

    @Transactional
    public SelectedCourseResponse createCourse(
            User user,
            Region region,
            String title,
            List<ResolvedCoursePlace> resolvedPlaces
    ) {
        inactiveActiveCourses(user.getId());

        Course course = courseRepository.save(
                Course.create(user, region, title)
        );

        addPlaces(course, region, resolvedPlaces);

        return getCourseResponse(course);
    }

    @Transactional
    public SelectedCourseResponse addPlacesToCourse(
            Course course,
            Region region,
            List<ResolvedCoursePlace> resolvedPlaces
    ) {
        int currentPlaceCount = coursePlaceRepository.countByCourseId(
                course.getId()
        );

        if (currentPlaceCount + resolvedPlaces.size()
                > MAX_COURSE_PLACE_COUNT) {
            throw new BusinessException(
                    CourseErrorCode.COURSE_PLACE_LIMIT_EXCEEDED
            );
        }

        addPlaces(course, region, resolvedPlaces);

        return getCourseResponse(course);
    }

    private void inactiveActiveCourses(Long userId) {
        List<Course> activeCourses = courseRepository
                .findAllByUserIdAndStatus(
                        userId,
                        CourseStatus.ACTIVE
                );

        activeCourses.forEach(Course::inactive);
    }

    private void addPlaces(
            Course course,
            Region region,
            List<ResolvedCoursePlace> resolvedPlaces
    ) {
        validateDuplicateCategoryInRequest(resolvedPlaces);

        int nextSequence = coursePlaceRepository.countByCourseId(
                course.getId()
        ) + 1;

        for (ResolvedCoursePlace resolvedPlace : resolvedPlaces) {
            Place place = resolvePlace(region, resolvedPlace);

            validateCoursePlace(course, place);

            CoursePlace coursePlace = CoursePlace.create(
                    course,
                    place,
                    nextSequence++
            );

            coursePlaceRepository.save(coursePlace);
        }
    }

    private Place resolvePlace(
            Region region,
            ResolvedCoursePlace resolvedPlace
    ) {
        CoursePlaceSaveRequest request = resolvedPlace.request();

        if (request.placeId() != null) {
            Place place = placeRepository.findById(request.placeId())
                    .orElseThrow(() ->
                            new BusinessException(
                                    PlaceErrorCode.PLACE_NOT_FOUND
                            )
                    );

            validatePlaceRegion(region, place);

            return place;
        }

        if (resolvedPlace.hasTourApiPlace()) {
            Place place = findOrCreateTourApiPlace(
                    region,
                    resolvedPlace.tourApiPlace()
            );

            validatePlaceRegion(region, place);

            return place;
        }

        Place place = findOrCreateKakaoPlace(region, request);
        validatePlaceRegion(region, place);

        return place;
    }

    private Place findOrCreateTourApiPlace(
            Region region,
            TourApiPlaceItem item
    ) {
        return placeRepository.findByTourContentId(item.contentId())
                .orElseGet(() -> createTourApiPlace(region, item));
    }

    private Place createTourApiPlace(
            Region region,
            TourApiPlaceItem item
    ) {
        validateTourApiItemRegion(region, item);

        PlaceCategoryGroup categoryGroup =
                PlaceCategoryMapper.toGroup(
                        item.lclsSystm1(),
                        item.lclsSystm3()
                );

        PlaceCategoryDetail categoryDetail =
                PlaceCategoryMapper.toDetail(
                        item.lclsSystm1(),
                        item.lclsSystm3()
                );

        Place place = Place.create(
                region,
                item.contentId(),
                null,
                item.title(),
                item.address(),
                toBigDecimal(item.latitude()),
                toBigDecimal(item.longitude()),
                item.firstImageUrl(),
                item.lDongRegnCd(),
                item.lDongSignguCd(),
                item.lclsSystm1(),
                item.lclsSystm2(),
                item.lclsSystm3(),
                categoryGroup,
                categoryDetail,
                false,
                PlaceSource.TOUR_API
        );

        return placeRepository.save(place);
    }

    private void validateTourApiItemRegion(
            Region region,
            TourApiPlaceItem item
    ) {
        if (!region.getLdongRegnCd().equals(item.lDongRegnCd())
                || !region.getLdongSignguCd().equals(item.lDongSignguCd())) {
            throw new BusinessException(
                    CourseErrorCode.PLACE_REGION_MISMATCH
            );
        }
    }

    private Place findOrCreateKakaoPlace(
            Region region,
            CoursePlaceSaveRequest request
    ) {
        if (!hasText(request.externalPlaceId())) {
            throw new BusinessException(
                    CourseErrorCode.EXTERNAL_PLACE_ID_REQUIRED
            );
        }

        return placeRepository.findByKakaoPlaceId(
                        request.externalPlaceId()
                )
                .orElseGet(() ->
                        createKakaoPlace(region, request)
                );
    }

    private Place createKakaoPlace(
            Region region,
            CoursePlaceSaveRequest request
    ) {
        validateKakaoPlaceRequest(region, request);

        PlaceCategoryGroup categoryGroup =
                toCategoryGroup(request.categoryGroup());

        PlaceCategoryDetail categoryDetail =
                toCategoryDetail(
                        request.categoryDetail(),
                        categoryGroup
                );

        Place place = Place.create(
                region,
                null,
                request.externalPlaceId(),
                request.title(),
                request.address(),
                request.latitude(),
                request.longitude(),
                null,
                region.getLdongRegnCd(),
                region.getLdongSignguCd(),
                null,
                null,
                null,
                categoryGroup,
                categoryDetail,
                false,
                PlaceSource.KAKAO_LOCAL
        );

        return placeRepository.save(place);
    }

    private void validateKakaoPlaceRequest(
            Region region,
            CoursePlaceSaveRequest request
    ) {
        if (!hasText(request.title())) {
            throw new BusinessException(
                    CourseErrorCode.INVALID_EXTERNAL_PLACE_DATA
            );
        }

        if (!hasText(request.address())) {
            throw new BusinessException(
                    CourseErrorCode.INVALID_EXTERNAL_PLACE_DATA
            );
        }

        if (request.latitude() == null
                || request.longitude() == null) {
            throw new BusinessException(
                    CourseErrorCode.INVALID_EXTERNAL_PLACE_DATA
            );
        }

        if (!isValidLatitude(request.latitude())
                || !isValidLongitude(request.longitude())) {
            throw new BusinessException(
                    CourseErrorCode.INVALID_EXTERNAL_PLACE_DATA
            );
        }

        if (!isAddressInRegion(request.address(), region)) {
            throw new BusinessException(
                    CourseErrorCode.PLACE_REGION_MISMATCH
            );
        }

        validateKakaoCategory(request);
    }

    private boolean isValidLatitude(BigDecimal latitude) {
        return latitude.compareTo(new BigDecimal("-90")) >= 0
                && latitude.compareTo(new BigDecimal("90")) <= 0;
    }

    private boolean isValidLongitude(BigDecimal longitude) {
        return longitude.compareTo(new BigDecimal("-180")) >= 0
                && longitude.compareTo(new BigDecimal("180")) <= 0;
    }

    private boolean isAddressInRegion(
            String address,
            Region region
    ) {
        if (!hasText(address)
                || !hasText(region.getCityCountyName())) {
            return false;
        }

        return address.contains(region.getCityCountyName());
    }

    private void validateKakaoCategory(
            CoursePlaceSaveRequest request
    ) {
        PlaceCategoryGroup categoryGroup =
                toCategoryGroup(request.categoryGroup());

        if (!hasText(request.categoryDetail())) {
            return;
        }

        PlaceCategoryDetail categoryDetail;

        try {
            categoryDetail = PlaceCategoryDetail.valueOf(
                    request.categoryDetail()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    CourseErrorCode.UNSUPPORTED_PLACE_CATEGORY
            );
        }

        boolean valid = switch (categoryGroup) {
            case TOURISM ->
                    categoryDetail == PlaceCategoryDetail.EXPERIENCE
                            || categoryDetail == PlaceCategoryDetail.HERITAGE
                            || categoryDetail == PlaceCategoryDetail.NATURE
                            || categoryDetail == PlaceCategoryDetail.MUSEUM
                            || categoryDetail == PlaceCategoryDetail.MARKET
                            || categoryDetail == PlaceCategoryDetail.SOUVENIR_SHOP;

            case FOOD ->
                    categoryDetail == PlaceCategoryDetail.RESTAURANT
                            || categoryDetail == PlaceCategoryDetail.LOCAL_FOOD
                            || categoryDetail == PlaceCategoryDetail.SNACK_MEAL
                            || categoryDetail == PlaceCategoryDetail.SNACK;

            case CAFE_DESSERT ->
                    categoryDetail == PlaceCategoryDetail.CAFE
                            || categoryDetail == PlaceCategoryDetail.DESSERT
                            || categoryDetail == PlaceCategoryDetail.BAKERY
                            || categoryDetail == PlaceCategoryDetail.TEA_HOUSE;
        };

        if (!valid) {
            throw new BusinessException(
                    CourseErrorCode.PLACE_CATEGORY_MISMATCH
            );
        }
    }

    private void validateCoursePlace(
            Course course,
            Place place
    ) {
        if (coursePlaceRepository.existsByCourseIdAndPlaceId(
                course.getId(),
                place.getId()
        )) {
            throw new BusinessException(
                    CourseErrorCode.DUPLICATE_PLACE
            );
        }

        if (coursePlaceRepository
                .existsByCourseIdAndCategoryGroup(
                        course.getId(),
                        place.getCategoryGroup()
                )) {
            throw new BusinessException(
                    CourseErrorCode.DUPLICATE_PLACE_CATEGORY
            );
        }
    }

    private void validateDuplicateCategoryInRequest(
            List<ResolvedCoursePlace> resolvedPlaces
    ) {
        Set<PlaceCategoryGroup> categoryGroups = new HashSet<>();

        for (ResolvedCoursePlace resolvedPlace : resolvedPlaces) {
            CoursePlaceSaveRequest request =
                    resolvedPlace.request();

            PlaceCategoryGroup categoryGroup =
                    toCategoryGroup(request.categoryGroup());

            if (!categoryGroups.add(categoryGroup)) {
                throw new BusinessException(
                        CourseErrorCode.DUPLICATE_REQUEST_PLACE_CATEGORY
                );
            }
        }
    }

    private void validatePlaceRegion(
            Region region,
            Place place
    ) {
        if (!place.getRegion().getId().equals(region.getId())) {
            throw new BusinessException(
                    CourseErrorCode.PLACE_REGION_MISMATCH
            );
        }
    }

    private SelectedCourseResponse getCourseResponse(
            Course course
    ) {
        List<CoursePlace> coursePlaces =
                coursePlaceRepository
                        .findByCourseIdOrderBySequenceAsc(
                                course.getId()
                        );

        return SelectedCourseResponse.of(
                course,
                coursePlaces
        );
    }

    private PlaceCategoryGroup toCategoryGroup(String value) {
        try {
            return PlaceCategoryGroup.valueOf(value);
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    CourseErrorCode.UNSUPPORTED_PLACE_CATEGORY
            );
        }
    }

    private PlaceCategoryDetail toCategoryDetail(
            String value,
            PlaceCategoryGroup categoryGroup
    ) {
        if (!hasText(value)) {
            return getDefaultCategoryDetail(categoryGroup);
        }

        try {
            return PlaceCategoryDetail.valueOf(value);
        } catch (RuntimeException exception) {
            return getDefaultCategoryDetail(categoryGroup);
        }
    }

    private PlaceCategoryDetail getDefaultCategoryDetail(
            PlaceCategoryGroup categoryGroup
    ) {
        return switch (categoryGroup) {
            case TOURISM -> PlaceCategoryDetail.EXPERIENCE;
            case FOOD -> PlaceCategoryDetail.RESTAURANT;
            case CAFE_DESSERT -> PlaceCategoryDetail.CAFE;
        };
    }

    private BigDecimal toBigDecimal(String value) {
        if (!hasText(value)) {
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}