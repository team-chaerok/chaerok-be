package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CourseCreateRequest;
import com.chaerok.backend.course.dto.CoursePlaceSaveRequest;
import com.chaerok.backend.course.entity.Course;
import com.chaerok.backend.course.entity.CourseStatus;
import com.chaerok.backend.course.repository.CoursePlaceRepository;
import com.chaerok.backend.course.repository.CourseRepository;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.repository.PlaceRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private PlaceRepository placeRepository;

    @Mock
    private TourApiPlaceClient tourApiPlaceClient;

    @Mock
    private User user;

    @Mock
    private Region region;

    private CourseCommandService courseCommandService;

    @BeforeEach
    void setUp() {
        courseCommandService = new CourseCommandService(
                courseRepository,
                coursePlaceRepository,
                userRepository,
                regionRepository,
                placeRepository,
                tourApiPlaceClient
        );
    }

    @Test
    @DisplayName("유효한 Kakao 외부 장소는 신규 Place로 저장한다")
    void createCourseSavesValidKakaoPlace() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createKakaoRequest(
                        "kakao-1",
                        "공주 카페",
                        "CAFE_DESSERT",
                        "CAFE",
                        "충남 공주시 웅진로 10",
                        new BigDecimal("36.4500"),
                        new BigDecimal("127.1200")
                );

        CourseCreateRequest request =
                new CourseCreateRequest(
                        1L,
                        "공주 여행",
                        List.of(placeRequest)
                );

        mockCourseCreationBase(placeRequest);

        when(region.getId()).thenReturn(1L);
        when(region.getCityCountyName()).thenReturn("공주시");

        when(placeRepository.findByKakaoPlaceId("kakao-1"))
                .thenReturn(Optional.empty());

        when(placeRepository.save(any(Place.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        courseCommandService.createCourse(1L, request);

        // then
        ArgumentCaptor<Place> placeCaptor =
                ArgumentCaptor.forClass(Place.class);

        verify(placeRepository).save(placeCaptor.capture());

        Place savedPlace = placeCaptor.getValue();

        assertThat(savedPlace.getKakaoPlaceId())
                .isEqualTo("kakao-1");
        assertThat(savedPlace.getTitle())
                .isEqualTo("공주 카페");
        assertThat(savedPlace.getAddress())
                .isEqualTo("충남 공주시 웅진로 10");
        assertThat(savedPlace.getCategoryGroup())
                .isEqualTo(PlaceCategoryGroup.CAFE_DESSERT);
        assertThat(savedPlace.getCategoryDetail())
                .isEqualTo(PlaceCategoryDetail.CAFE);
        assertThat(savedPlace.getSource())
                .isEqualTo(PlaceSource.KAKAO_LOCAL);
    }

    @Test
    @DisplayName("다른 시군의 Kakao 외부 장소는 저장하지 않는다")
    void createCourseRejectsKakaoPlaceOutsideRegion() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createKakaoRequest(
                        "kakao-2",
                        "논산 카페",
                        "CAFE_DESSERT",
                        "CAFE",
                        "충남 논산시 시민로 10",
                        new BigDecimal("36.2000"),
                        new BigDecimal("127.0000")
                );

        CourseCreateRequest request =
                new CourseCreateRequest(
                        1L,
                        "공주 여행",
                        List.of(placeRequest)
                );

        mockCourseCreationBase(placeRequest);
        when(region.getCityCountyName()).thenReturn("공주시");

        when(placeRepository.findByKakaoPlaceId("kakao-2"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                courseCommandService.createCourse(1L, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kakao 장소가 코스 지역과 일치하지 않습니다.");

        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("좌표가 없는 Kakao 외부 장소는 저장하지 않는다")
    void createCourseRejectsKakaoPlaceWithoutCoordinates() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createKakaoRequest(
                        "kakao-3",
                        "공주 카페",
                        "CAFE_DESSERT",
                        "CAFE",
                        "충남 공주시 웅진로 10",
                        null,
                        null
                );

        CourseCreateRequest request =
                new CourseCreateRequest(
                        1L,
                        "공주 여행",
                        List.of(placeRequest)
                );

        mockCourseCreationBase(placeRequest);

        when(placeRepository.findByKakaoPlaceId("kakao-3"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                courseCommandService.createCourse(1L, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kakao 장소 좌표는 필수입니다.");

        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("유효 범위를 벗어난 Kakao 외부 장소 좌표는 저장하지 않는다")
    void createCourseRejectsKakaoPlaceWithInvalidCoordinates() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createKakaoRequest(
                        "kakao-4",
                        "공주 카페",
                        "CAFE_DESSERT",
                        "CAFE",
                        "충남 공주시 웅진로 10",
                        new BigDecimal("91.0000"),
                        new BigDecimal("127.1200")
                );

        CourseCreateRequest request =
                new CourseCreateRequest(
                        1L,
                        "공주 여행",
                        List.of(placeRequest)
                );

        mockCourseCreationBase(placeRequest);

        when(placeRepository.findByKakaoPlaceId("kakao-4"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                courseCommandService.createCourse(1L, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kakao 장소 좌표가 올바르지 않습니다.");

        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("지원하지 않는 Kakao 장소 세부 유형은 저장하지 않는다")
    void createCourseRejectsUnsupportedKakaoCategoryDetail() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createKakaoRequest(
                        "kakao-5",
                        "공주 카페",
                        "CAFE_DESSERT",
                        "UNKNOWN",
                        "충남 공주시 웅진로 10",
                        new BigDecimal("36.4500"),
                        new BigDecimal("127.1200")
                );

        CourseCreateRequest request =
                new CourseCreateRequest(
                        1L,
                        "공주 여행",
                        List.of(placeRequest)
                );

        mockCourseCreationBase(placeRequest);
        when(region.getCityCountyName()).thenReturn("공주시");

        when(placeRepository.findByKakaoPlaceId("kakao-5"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                courseCommandService.createCourse(1L, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 장소 세부 유형입니다.");

        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("장소 유형과 세부 유형이 일치하지 않으면 Kakao 장소를 저장하지 않는다")
    void createCourseRejectsMismatchedKakaoCategory() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createKakaoRequest(
                        "kakao-6",
                        "공주 카페",
                        "FOOD",
                        "CAFE",
                        "충남 공주시 웅진로 10",
                        new BigDecimal("36.4500"),
                        new BigDecimal("127.1200")
                );

        CourseCreateRequest request =
                new CourseCreateRequest(
                        1L,
                        "공주 여행",
                        List.of(placeRequest)
                );

        mockCourseCreationBase(placeRequest);
        when(region.getCityCountyName()).thenReturn("공주시");

        when(placeRepository.findByKakaoPlaceId("kakao-6"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                courseCommandService.createCourse(1L, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("장소 유형과 세부 유형이 일치하지 않습니다.");

        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("이미 저장된 kakaoPlaceId가 있으면 기존 Place를 재사용한다")
    void createCourseReusesExistingKakaoPlace() {
        // given
        CoursePlaceSaveRequest placeRequest =
                createKakaoRequest(
                        "kakao-7",
                        "공주 카페",
                        "CAFE_DESSERT",
                        "CAFE",
                        "충남 공주시 웅진로 10",
                        new BigDecimal("36.4500"),
                        new BigDecimal("127.1200")
                );

        CourseCreateRequest request =
                new CourseCreateRequest(
                        1L,
                        "공주 여행",
                        List.of(placeRequest)
                );

        mockCourseCreationBase(placeRequest);

        when(region.getId()).thenReturn(1L);

        Place existingPlace = Place.create(
                region,
                null,
                "kakao-7",
                "기존 공주 카페",
                "충남 공주시 웅진로 10",
                new BigDecimal("36.4500"),
                new BigDecimal("127.1200"),
                null,
                "44",
                "150",
                null,
                null,
                null,
                PlaceCategoryGroup.CAFE_DESSERT,
                PlaceCategoryDetail.CAFE,
                false,
                PlaceSource.KAKAO_LOCAL
        );

        when(placeRepository.findByKakaoPlaceId("kakao-7"))
                .thenReturn(Optional.of(existingPlace));

        // when
        courseCommandService.createCourse(1L, request);

        // then
        verify(placeRepository, never())
                .save(any(Place.class));

        verify(coursePlaceRepository)
                .save(any());
    }

    private void mockCourseCreationBase(
            CoursePlaceSaveRequest placeRequest
    ) {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(regionRepository.findById(1L))
                .thenReturn(Optional.of(region));

        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");

        when(courseRepository.findAllByUserIdAndStatus(
                1L,
                CourseStatus.ACTIVE
        )).thenReturn(List.of());

        when(courseRepository.save(any(Course.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(coursePlaceRepository.countByCourseId(null))
                .thenReturn(0);

        when(tourApiPlaceClient.searchPlacesByKeyword(
                placeRequest.title(),
                "44",
                "150"
        )).thenReturn(List.of());
    }

    private CoursePlaceSaveRequest createKakaoRequest(
            String externalPlaceId,
            String title,
            String categoryGroup,
            String categoryDetail,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        return new CoursePlaceSaveRequest(
                null,
                externalPlaceId,
                PlaceSource.KAKAO_LOCAL.name(),
                title,
                categoryGroup,
                categoryDetail,
                address,
                latitude,
                longitude,
                null
        );
    }
}