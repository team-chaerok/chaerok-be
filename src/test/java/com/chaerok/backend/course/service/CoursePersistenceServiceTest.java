package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CoursePlaceSaveRequest;
import com.chaerok.backend.course.entity.Course;
import com.chaerok.backend.course.entity.CoursePlace;
import com.chaerok.backend.course.entity.CourseStatus;
import com.chaerok.backend.course.repository.CoursePlaceRepository;
import com.chaerok.backend.course.repository.CourseRepository;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.entity.Region;
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
class CoursePersistenceServiceTest {

    private static final Long COURSE_ID = 10L;
    private static final Long REGION_ID = 1L;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CoursePlaceRepository coursePlaceRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private Course course;

    @Mock
    private Region region;

    private CoursePersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new CoursePersistenceService(
                courseRepository,
                coursePlaceRepository,
                placeRepository
        );
    }

    @Test
    @DisplayName("유효한 Kakao 외부 장소는 신규 Place로 저장한다")
    void addPlacesSavesValidKakaoPlace() {
        // given
        mockSuccessfulCourse();

        when(region.getLdongRegnCd())
                .thenReturn("44");
        when(region.getLdongSignguCd())
                .thenReturn("150");
        when(region.getCityCountyName())
                .thenReturn("공주시");

        CoursePlaceSaveRequest request = createKakaoRequest(
                "kakao-1",
                "공주 카페",
                "CAFE_DESSERT",
                "CAFE",
                "충남 공주시 웅진로 10",
                new BigDecimal("36.4500"),
                new BigDecimal("127.1200")
        );

        when(placeRepository.findByKakaoPlaceId("kakao-1"))
                .thenReturn(Optional.empty());

        when(placeRepository.save(any(Place.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        persistenceService.addPlacesToCourse(
                course,
                region,
                List.of(
                        ResolvedCoursePlace.of(request, null)
                )
        );

        // then
        ArgumentCaptor<Place> captor =
                ArgumentCaptor.forClass(Place.class);

        verify(placeRepository).save(captor.capture());

        Place savedPlace = captor.getValue();

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

        verify(coursePlaceRepository)
                .save(any(CoursePlace.class));
    }

    @Test
    @DisplayName("다른 시군의 Kakao 외부 장소는 저장하지 않는다")
    void addPlacesRejectsKakaoPlaceOutsideRegion() {
        // given
        when(course.getId()).thenReturn(COURSE_ID);
        when(region.getCityCountyName())
                .thenReturn("공주시");

        CoursePlaceSaveRequest request = createKakaoRequest(
                "kakao-2",
                "논산 카페",
                "CAFE_DESSERT",
                "CAFE",
                "충남 논산시 중앙로 10",
                new BigDecimal("36.2000"),
                new BigDecimal("127.0000")
        );

        when(placeRepository.findByKakaoPlaceId("kakao-2"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                persistenceService.addPlacesToCourse(
                        course,
                        region,
                        List.of(
                                ResolvedCoursePlace.of(request, null)
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Kakao 장소가 코스 지역과 일치하지 않습니다."
                );

        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("좌표가 없는 Kakao 외부 장소는 저장하지 않는다")
    void addPlacesRejectsKakaoPlaceWithoutCoordinates() {
        // given
        when(course.getId()).thenReturn(COURSE_ID);

        CoursePlaceSaveRequest request = createKakaoRequest(
                "kakao-3",
                "공주 카페",
                "CAFE_DESSERT",
                "CAFE",
                "충남 공주시 웅진로 10",
                null,
                null
        );

        when(placeRepository.findByKakaoPlaceId("kakao-3"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                persistenceService.addPlacesToCourse(
                        course,
                        region,
                        List.of(
                                ResolvedCoursePlace.of(request, null)
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kakao 장소 좌표는 필수입니다.");

        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("유효 범위를 벗어난 Kakao 외부 장소 좌표는 저장하지 않는다")
    void addPlacesRejectsInvalidKakaoCoordinates() {
        // given
        when(course.getId()).thenReturn(COURSE_ID);

        CoursePlaceSaveRequest request = createKakaoRequest(
                "kakao-4",
                "공주 카페",
                "CAFE_DESSERT",
                "CAFE",
                "충남 공주시 웅진로 10",
                new BigDecimal("91"),
                new BigDecimal("127.1200")
        );

        when(placeRepository.findByKakaoPlaceId("kakao-4"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                persistenceService.addPlacesToCourse(
                        course,
                        region,
                        List.of(
                                ResolvedCoursePlace.of(request, null)
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Kakao 장소 좌표가 올바르지 않습니다."
                );

        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("지원하지 않는 Kakao 장소 세부 유형은 저장하지 않는다")
    void addPlacesRejectsUnsupportedKakaoCategoryDetail() {
        // given
        when(course.getId()).thenReturn(COURSE_ID);
        when(region.getCityCountyName())
                .thenReturn("공주시");

        CoursePlaceSaveRequest request = createKakaoRequest(
                "kakao-5",
                "공주 카페",
                "CAFE_DESSERT",
                "UNKNOWN",
                "충남 공주시 웅진로 10",
                new BigDecimal("36.4500"),
                new BigDecimal("127.1200")
        );

        when(placeRepository.findByKakaoPlaceId("kakao-5"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                persistenceService.addPlacesToCourse(
                        course,
                        region,
                        List.of(
                                ResolvedCoursePlace.of(request, null)
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "지원하지 않는 장소 세부 유형입니다."
                );

        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("장소 유형과 세부 유형이 일치하지 않으면 Kakao 장소를 저장하지 않는다")
    void addPlacesRejectsMismatchedKakaoCategory() {
        // given
        when(course.getId()).thenReturn(COURSE_ID);
        when(region.getCityCountyName())
                .thenReturn("공주시");

        CoursePlaceSaveRequest request = createKakaoRequest(
                "kakao-6",
                "공주 카페",
                "FOOD",
                "CAFE",
                "충남 공주시 웅진로 10",
                new BigDecimal("36.4500"),
                new BigDecimal("127.1200")
        );

        when(placeRepository.findByKakaoPlaceId("kakao-6"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                persistenceService.addPlacesToCourse(
                        course,
                        region,
                        List.of(
                                ResolvedCoursePlace.of(request, null)
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "장소 유형과 세부 유형이 일치하지 않습니다."
                );

        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("이미 저장된 kakaoPlaceId가 있으면 기존 Place를 재사용한다")
    void addPlacesReusesExistingKakaoPlace() {
        // given
        mockSuccessfulCourse();

        CoursePlaceSaveRequest request = createKakaoRequest(
                "kakao-7",
                "공주 카페",
                "CAFE_DESSERT",
                "CAFE",
                "충남 공주시 웅진로 10",
                new BigDecimal("36.4500"),
                new BigDecimal("127.1200")
        );

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
        persistenceService.addPlacesToCourse(
                course,
                region,
                List.of(
                        ResolvedCoursePlace.of(request, null)
                )
        );

        // then
        verify(placeRepository, never())
                .save(any(Place.class));

        verify(coursePlaceRepository)
                .save(any(CoursePlace.class));
    }

    @Test
    @DisplayName("이미 저장된 TourAPI 장소가 있으면 tourContentId 기준으로 재사용한다")
    void addPlacesReusesExistingTourApiPlace() {
        // given
        mockSuccessfulCourse();

        CoursePlaceSaveRequest request =
                createTourApiRequest();

        TourApiPlaceItem item =
                createTourApiPlaceItem();

        Place existingPlace = Place.create(
                region,
                "126204",
                null,
                "공산성",
                "충남 공주시 금성동",
                new BigDecimal("36.4650"),
                new BigDecimal("127.1270"),
                null,
                "44",
                "150",
                "HS",
                null,
                "HS01",
                PlaceCategoryGroup.TOURISM,
                PlaceCategoryDetail.HERITAGE,
                true,
                PlaceSource.TOUR_API
        );

        when(placeRepository.findByTourContentId("126204"))
                .thenReturn(Optional.of(existingPlace));

        // when
        persistenceService.addPlacesToCourse(
                course,
                region,
                List.of(
                        ResolvedCoursePlace.of(request, item)
                )
        );

        // then
        verify(placeRepository, never())
                .save(any(Place.class));

        verify(coursePlaceRepository)
                .save(any(CoursePlace.class));
    }

    @Test
    @DisplayName("신규 TourAPI 장소는 외부 조회 결과를 사용해 Place로 저장한다")
    void addPlacesSavesNewTourApiPlace() {
        // given
        mockSuccessfulCourse();

        CoursePlaceSaveRequest request =
                createTourApiRequest();

        TourApiPlaceItem item =
                createTourApiPlaceItem();

        when(region.getLdongRegnCd())
                .thenReturn("44");
        when(region.getLdongSignguCd())
                .thenReturn("150");

        when(placeRepository.findByTourContentId("126204"))
                .thenReturn(Optional.empty());

        when(placeRepository.save(any(Place.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        persistenceService.addPlacesToCourse(
                course,
                region,
                List.of(
                        ResolvedCoursePlace.of(request, item)
                )
        );

        // then
        ArgumentCaptor<Place> captor =
                ArgumentCaptor.forClass(Place.class);

        verify(placeRepository).save(captor.capture());

        Place savedPlace = captor.getValue();

        assertThat(savedPlace.getTourContentId())
                .isEqualTo("126204");
        assertThat(savedPlace.getTitle())
                .isEqualTo("공산성");
        assertThat(savedPlace.getCategoryGroup())
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(savedPlace.getCategoryDetail())
                .isEqualTo(PlaceCategoryDetail.HERITAGE);
        assertThat(savedPlace.getSource())
                .isEqualTo(PlaceSource.TOUR_API);
    }

    private void mockSuccessfulCourse() {
        when(course.getId())
                .thenReturn(COURSE_ID);

        when(course.getRegion())
                .thenReturn(region);

        when(course.getTitle())
                .thenReturn("공주 여행");

        when(course.getStatus())
                .thenReturn(CourseStatus.ACTIVE);

        when(region.getId())
                .thenReturn(REGION_ID);

        when(coursePlaceRepository
                .findByCourseIdOrderBySequenceAsc(COURSE_ID))
                .thenReturn(List.of());
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

    private CoursePlaceSaveRequest createTourApiRequest() {
        return new CoursePlaceSaveRequest(
                null,
                "126204",
                PlaceSource.TOUR_API.name(),
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