package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CourseAddPlacesRequest;
import com.chaerok.backend.course.dto.CourseCreateRequest;
import com.chaerok.backend.course.dto.CoursePlaceSaveRequest;
import com.chaerok.backend.course.dto.SelectedCourseResponse;
import com.chaerok.backend.course.entity.Course;
import com.chaerok.backend.course.entity.CourseStatus;
import com.chaerok.backend.course.repository.CoursePlaceRepository;
import com.chaerok.backend.course.repository.CourseRepository;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseCommandServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CoursePlaceRepository coursePlaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private CourseExternalPlaceResolver externalPlaceResolver;

    @Mock
    private CoursePersistenceService persistenceService;

    @Mock
    private User user;

    @Mock
    private Region region;

    @Mock
    private Course course;

    @Mock
    private SelectedCourseResponse response;

    private CourseCommandService courseCommandService;

    @BeforeEach
    void setUp() {
        courseCommandService = new CourseCommandService(
                courseRepository,
                coursePlaceRepository,
                userRepository,
                regionRepository,
                externalPlaceResolver,
                persistenceService
        );
    }

    @Test
    @DisplayName("코스 생성 시 외부 장소를 먼저 조회한 뒤 저장 서비스에 위임한다")
    void createCourseResolvesExternalPlaceBeforePersistence() {
        // given
        CoursePlaceSaveRequest placeRequest = createTourApiRequest();

        CourseCreateRequest request = new CourseCreateRequest(
                1L,
                "공주 여행",
                List.of(placeRequest)
        );

        TourApiPlaceItem tourApiPlace = createTourApiPlaceItem();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(regionRepository.findById(1L))
                .thenReturn(Optional.of(region));

        when(externalPlaceResolver.resolveTourApiPlace(
                region,
                placeRequest
        )).thenReturn(Optional.of(tourApiPlace));

        when(persistenceService.createCourse(
                any(User.class),
                any(Region.class),
                any(String.class),
                any()
        )).thenReturn(response);

        // when
        SelectedCourseResponse result =
                courseCommandService.createCourse(1L, request);

        // then
        ArgumentCaptor<List<ResolvedCoursePlace>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(externalPlaceResolver)
                .resolveTourApiPlace(region, placeRequest);

        verify(persistenceService)
                .createCourse(
                        eq(user),
                        eq(region),
                        eq("공주 여행"),
                        captor.capture()
                );

        List<ResolvedCoursePlace> resolvedPlaces =
                captor.getValue();

        assertThat(resolvedPlaces).hasSize(1);
        assertThat(resolvedPlaces.get(0).request())
                .isEqualTo(placeRequest);
        assertThat(resolvedPlaces.get(0).tourApiPlace())
                .isEqualTo(tourApiPlace);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("DB 장소 요청은 외부 API 조회 없이 저장 서비스에 전달한다")
    void createCourseDoesNotResolveExistingDbPlaceExternally() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createDbPlaceRequest();

        CourseCreateRequest request = new CourseCreateRequest(
                1L,
                "공주 여행",
                List.of(placeRequest)
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(regionRepository.findById(1L))
                .thenReturn(Optional.of(region));

        when(persistenceService.createCourse(
                any(User.class),
                any(Region.class),
                any(String.class),
                any()
        )).thenReturn(response);

        // when
        courseCommandService.createCourse(1L, request);

        // then
        ArgumentCaptor<List<ResolvedCoursePlace>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(externalPlaceResolver, never())
                .resolveTourApiPlace(any(), any());

        verify(persistenceService)
                .createCourse(
                        eq(user),
                        eq(region),
                        eq("공주 여행"),
                        captor.capture()
                );

        ResolvedCoursePlace resolvedPlace =
                captor.getValue().get(0);

        assertThat(resolvedPlace.request())
                .isEqualTo(placeRequest);
        assertThat(resolvedPlace.tourApiPlace())
                .isNull();
    }

    @Test
    @DisplayName("TourAPI에서 매칭되지 않은 외부 장소는 Kakao 저장 대상으로 전달한다")
    void createCoursePassesUnresolvedExternalPlaceToPersistence() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createKakaoRequest();

        CourseCreateRequest request = new CourseCreateRequest(
                1L,
                "공주 여행",
                List.of(placeRequest)
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(regionRepository.findById(1L))
                .thenReturn(Optional.of(region));

        when(externalPlaceResolver.resolveTourApiPlace(
                region,
                placeRequest
        )).thenReturn(Optional.empty());

        when(persistenceService.createCourse(
                any(User.class),
                any(Region.class),
                any(String.class),
                any()
        )).thenReturn(response);

        // when
        courseCommandService.createCourse(1L, request);

        // then
        ArgumentCaptor<List<ResolvedCoursePlace>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(persistenceService)
                .createCourse(
                        eq(user),
                        eq(region),
                        eq("공주 여행"),
                        captor.capture()
                );

        ResolvedCoursePlace resolvedPlace =
                captor.getValue().get(0);

        assertThat(resolvedPlace.request())
                .isEqualTo(placeRequest);
        assertThat(resolvedPlace.tourApiPlace())
                .isNull();
    }

    @Test
    @DisplayName("ACTIVE 코스 장소 추가 시 외부 장소 조회 후 저장 서비스에 위임한다")
    void addPlacesResolvesExternalPlaceBeforePersistence() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createKakaoRequest();

        CourseAddPlacesRequest request =
                new CourseAddPlacesRequest(
                        List.of(placeRequest)
                );

        when(courseRepository.findByUserIdAndStatus(
                1L,
                CourseStatus.ACTIVE
        )).thenReturn(Optional.of(course));

        when(course.getRegion())
                .thenReturn(region);

        when(externalPlaceResolver.resolveTourApiPlace(
                region,
                placeRequest
        )).thenReturn(Optional.empty());

        when(persistenceService.addPlacesToCourse(
                any(Course.class),
                any(Region.class),
                any()
        )).thenReturn(response);

        // when
        SelectedCourseResponse result =
                courseCommandService.addPlacesToActiveCourse(
                        1L,
                        request
                );

        // then
        verify(externalPlaceResolver)
                .resolveTourApiPlace(region, placeRequest);

        verify(persistenceService)
                .addPlacesToCourse(
                        course,
                        region,
                        List.of(
                                ResolvedCoursePlace.of(
                                        placeRequest,
                                        null
                                )
                        )
                );

        assertThat(result).isSameAs(response);
    }

    private CoursePlaceSaveRequest createTourApiRequest() {
        return new CoursePlaceSaveRequest(
                null,
                "126204",
                "TOUR_API",
                "공산성",
                "TOURISM",
                "HERITAGE",
                "충남 공주시 금성동",
                new BigDecimal("36.4650"),
                new BigDecimal("127.1270"),
                null
        );
    }

    private CoursePlaceSaveRequest createKakaoRequest() {
        return new CoursePlaceSaveRequest(
                null,
                "kakao-1",
                "KAKAO_LOCAL",
                "공주 카페",
                "CAFE_DESSERT",
                "CAFE",
                "충남 공주시 웅진로 10",
                new BigDecimal("36.4500"),
                new BigDecimal("127.1200"),
                null
        );
    }

    private CoursePlaceSaveRequest createDbPlaceRequest() {
        return new CoursePlaceSaveRequest(
                100L,
                null,
                "TOUR_API",
                "공산성",
                "TOURISM",
                "HERITAGE",
                "충남 공주시 금성동",
                new BigDecimal("36.4650"),
                new BigDecimal("127.1270"),
                null
        );
    }

    private TourApiPlaceItem createTourApiPlaceItem() {
        return new TourApiPlaceItem(
                "126204",
                "공산성",
                "충남 공주시 금성동",
                "36.4650",
                "127.1270",
                null,
                "44",
                "150",
                "HS",
                null,
                "HS01",
                null
        );
    }
}