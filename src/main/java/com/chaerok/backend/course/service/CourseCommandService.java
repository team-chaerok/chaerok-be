package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CourseAddPlacesRequest;
import com.chaerok.backend.course.dto.CourseCreateRequest;
import com.chaerok.backend.course.dto.CoursePlaceSaveRequest;
import com.chaerok.backend.course.dto.SelectedCourseResponse;
import com.chaerok.backend.course.entity.Course;
import com.chaerok.backend.course.entity.CourseStatus;
import com.chaerok.backend.course.repository.CoursePlaceRepository;
import com.chaerok.backend.course.repository.CourseRepository;
import com.chaerok.backend.global.exception.RegionNotFoundException;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseCommandService {

    private static final int MAX_COURSE_PLACE_COUNT = 3;

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final CourseExternalPlaceResolver externalPlaceResolver;
    private final CoursePersistenceService persistenceService;

    public SelectedCourseResponse createCourse(
            Long userId,
            CourseCreateRequest request
    ) {
        validatePlaceRequestSize(request.places());

        User user = findUser(userId);
        Region region = findRegion(request.regionId());

        List<ResolvedCoursePlace> resolvedPlaces =
                resolvePlaces(region, request.places());

        return persistenceService.createCourse(
                user,
                region,
                request.title(),
                resolvedPlaces
        );
    }

    public SelectedCourseResponse addPlacesToActiveCourse(
            Long userId,
            CourseAddPlacesRequest request
    ) {
        validatePlaceRequestSize(request.places());

        Course course = findActiveCourse(userId);
        Region region = course.getRegion();

        List<ResolvedCoursePlace> resolvedPlaces =
                resolvePlaces(region, request.places());

        return persistenceService.addPlacesToCourse(
                course,
                region,
                resolvedPlaces
        );
    }

    @Transactional(readOnly = true)
    public SelectedCourseResponse getActiveCourse(Long userId) {
        Course course = findActiveCourse(userId);
        return getCourseResponse(course);
    }

    private List<ResolvedCoursePlace> resolvePlaces(
            Region region,
            List<CoursePlaceSaveRequest> requests
    ) {
        return requests.stream()
                .map(request -> {
                    if (request.placeId() != null) {
                        return ResolvedCoursePlace.of(
                                request,
                                null
                        );
                    }

                    return ResolvedCoursePlace.of(
                            request,
                            externalPlaceResolver
                                    .resolveTourApiPlace(
                                            region,
                                            request
                                    )
                                    .orElse(null)
                    );
                })
                .toList();
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

    private SelectedCourseResponse getCourseResponse(
            Course course
    ) {
        return SelectedCourseResponse.of(
                course,
                coursePlaceRepository
                        .findByCourseIdOrderBySequenceAsc(
                                course.getId()
                        )
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );
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
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "ACTIVE 코스가 없습니다."
                        )
                );
    }
}