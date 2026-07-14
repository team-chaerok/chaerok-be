package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CourseAddPlacesRequest;
import com.chaerok.backend.course.dto.CourseCreateRequest;
import com.chaerok.backend.course.dto.CoursePlaceSaveRequest;
import com.chaerok.backend.course.dto.SelectedCourseResponse;
import com.chaerok.backend.course.entity.Course;
import com.chaerok.backend.course.entity.CoursePlace;
import com.chaerok.backend.course.entity.CourseStatus;
import com.chaerok.backend.course.repository.CoursePlaceRepository;
import com.chaerok.backend.course.repository.CourseRepository;
import com.chaerok.backend.global.exception.PlaceNotFoundException;
import com.chaerok.backend.global.exception.RegionNotFoundException;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.place.service.PlaceCategoryMapper;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseCommandService {

    private static final int MAX_COURSE_PLACE_COUNT = 3;

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final PlaceRepository placeRepository;
    private final TourApiPlaceClient tourApiPlaceClient;

    public SelectedCourseResponse createCourse(
            Long userId,
            CourseCreateRequest request
    ) {
        validatePlaceRequestSize(request.places());

        User user = findUser(userId);
        Region region = findRegion(request.regionId());

        inactiveActiveCourses(userId);

        Course course = courseRepository.save(
                Course.create(user, region, request.title())
        );

        addPlaces(course, region, request.places());

        return getCourseResponse(course);
    }

    public SelectedCourseResponse addPlacesToActiveCourse(
            Long userId,
            CourseAddPlacesRequest request
    ) {
        validatePlaceRequestSize(request.places());

        Course course = findActiveCourse(userId);
        Region region = course.getRegion();

        int currentPlaceCount = coursePlaceRepository.countByCourseId(
                course.getId()
        );

        if (currentPlaceCount + request.places().size()
                > MAX_COURSE_PLACE_COUNT) {
            throw new IllegalArgumentException(
                    "ACTIVE 코스에는 최대 3개 장소까지만 저장할 수 있습니다."
            );
        }

        addPlaces(course, region, request.places());

        return getCourseResponse(course);
    }

    @Transactional(readOnly = true)
    public SelectedCourseResponse getActiveCourse(Long userId) {
        Course course = findActiveCourse(userId);
        return getCourseResponse(course);
    }

    private void inactiveActiveCourses(Long userId) {
        List<Course> activeCourses = courseRepository
                .findAllByUserIdAndStatus(userId, CourseStatus.ACTIVE);

        activeCourses.forEach(Course::inactive);
    }

    private void addPlaces(
            Course course,
            Region region,
            List<CoursePlaceSaveRequest> requests
    ) {
        validateDuplicateCategoryInRequest(requests);

        int nextSequence = coursePlaceRepository.countByCourseId(
                course.getId()
        ) + 1;

        for (CoursePlaceSaveRequest request : requests) {
            Place place = resolvePlace(region, request);

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
            CoursePlaceSaveRequest request
    ) {
        if (request.placeId() != null) {
            Place place = placeRepository.findById(request.placeId())
                    .orElseThrow(PlaceNotFoundException::new);

            validatePlaceRegion(region, place);

            return place;
        }

        return resolveExternalPlace(region, request);
    }

    private Place resolveExternalPlace(
            Region region,
            CoursePlaceSaveRequest request
    ) {
        Place tourApiPlace = findOrCreateTourApiPlace(region, request);

        if (tourApiPlace != null) {
            validatePlaceRegion(region, tourApiPlace);
            return tourApiPlace;
        }

        Place kakaoPlace = findOrCreateKakaoPlace(region, request);
        validatePlaceRegion(region, kakaoPlace);

        return kakaoPlace;
    }

    private Place findOrCreateTourApiPlace(
            Region region,
            CoursePlaceSaveRequest request
    ) {
        if (isTourApiRequest(request)
                && hasText(request.externalPlaceId())) {
            return placeRepository.findByTourContentId(
                            request.externalPlaceId()
                    )
                    .orElseGet(() -> createTourApiPlaceByContentIdOrThrow(
                            region,
                            request.externalPlaceId()
                    ));
        }

        List<TourApiPlaceItem> items =
                tourApiPlaceClient.searchPlacesByKeyword(
                        request.title(),
                        region.getLdongRegnCd(),
                        region.getLdongSignguCd()
                );

        return items.stream()
                .filter(item -> isSamePlace(item, request))
                .findFirst()
                .map(item -> placeRepository.findByTourContentId(
                                item.contentId()
                        )
                        .orElseGet(() -> createTourApiPlace(region, item)))
                .orElse(null);
    }

    private Place createTourApiPlaceByContentIdOrThrow(
            Region region,
            String contentId
    ) {
        TourApiPlaceItem item = tourApiPlaceClient.getPlaceDetail(contentId);

        if (item == null) {
            throw new IllegalArgumentException(
                    "TourAPI 장소 정보를 찾을 수 없습니다."
            );
        }

        validateTourApiItemRegion(region, item);

        return createTourApiPlace(region, item);
    }

    private Place createTourApiPlace(
            Region region,
            TourApiPlaceItem item
    ) {
        validateTourApiItemRegion(region, item);

        PlaceCategoryGroup categoryGroup = PlaceCategoryMapper.toGroup(
                item.lclsSystm1(),
                item.lclsSystm3()
        );

        PlaceCategoryDetail categoryDetail = PlaceCategoryMapper.toDetail(
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
            throw new IllegalArgumentException(
                    "TourAPI 장소가 코스 지역과 일치하지 않습니다."
            );
        }
    }

    private Place findOrCreateKakaoPlace(
            Region region,
            CoursePlaceSaveRequest request
    ) {
        if (!hasText(request.externalPlaceId())) {
            throw new IllegalArgumentException(
                    "외부 후보 장소 저장 시 externalPlaceId는 필수입니다."
            );
        }

        return placeRepository.findByKakaoPlaceId(request.externalPlaceId())
                .orElseGet(() -> createKakaoPlace(region, request));
    }

    private Place createKakaoPlace(
            Region region,
            CoursePlaceSaveRequest request
    ) {
        PlaceCategoryGroup categoryGroup = toCategoryGroup(
                request.categoryGroup()
        );

        PlaceCategoryDetail categoryDetail = toCategoryDetail(
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

    private void validateCoursePlace(
            Course course,
            Place place
    ) {
        if (coursePlaceRepository.existsByCourseIdAndPlaceId(
                course.getId(),
                place.getId()
        )) {
            throw new IllegalArgumentException(
                    "이미 ACTIVE 코스에 저장된 장소입니다."
            );
        }

        if (coursePlaceRepository.existsByCourseIdAndCategoryGroup(
                course.getId(),
                place.getCategoryGroup()
        )) {
            throw new IllegalArgumentException(
                    "이미 ACTIVE 코스에 저장된 장소 유형입니다."
            );
        }
    }

    private void validateDuplicateCategoryInRequest(
            List<CoursePlaceSaveRequest> requests
    ) {
        Set<PlaceCategoryGroup> categoryGroups = new HashSet<>();

        for (CoursePlaceSaveRequest request : requests) {
            PlaceCategoryGroup categoryGroup = toCategoryGroup(
                    request.categoryGroup()
            );

            if (!categoryGroups.add(categoryGroup)) {
                throw new IllegalArgumentException(
                        "한 번의 요청에 동일한 장소 유형을 중복 저장할 수 없습니다."
                );
            }
        }
    }

    private void validatePlaceRequestSize(
            List<CoursePlaceSaveRequest> places
    ) {
        if (places == null || places.isEmpty()) {
            throw new IllegalArgumentException(
                    "코스 장소는 1개 이상 선택해야 합니다."
            );
        }

        if (places.size() > MAX_COURSE_PLACE_COUNT) {
            throw new IllegalArgumentException(
                    "코스 장소는 최대 3개까지 선택할 수 있습니다."
            );
        }
    }

    private void validatePlaceRegion(
            Region region,
            Place place
    ) {
        if (!place.getRegion().getId().equals(region.getId())) {
            throw new IllegalArgumentException(
                    "선택한 장소가 코스 지역과 일치하지 않습니다."
            );
        }
    }

    private SelectedCourseResponse getCourseResponse(Course course) {
        List<CoursePlace> coursePlaces = coursePlaceRepository
                .findByCourseIdOrderBySequenceAsc(course.getId());

        return SelectedCourseResponse.of(course, coursePlaces);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다."
                ));
    }

    private Region findRegion(Long regionId) {
        return regionRepository.findById(regionId)
                .orElseThrow(RegionNotFoundException::new);
    }

    private Course findActiveCourse(Long userId) {
        return courseRepository.findByUserIdAndStatus(
                        userId,
                        CourseStatus.ACTIVE
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "ACTIVE 코스가 없습니다."
                ));
    }

    private boolean isTourApiRequest(CoursePlaceSaveRequest request) {
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

    private PlaceCategoryGroup toCategoryGroup(String value) {
        try {
            return PlaceCategoryGroup.valueOf(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "지원하지 않는 장소 유형입니다."
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