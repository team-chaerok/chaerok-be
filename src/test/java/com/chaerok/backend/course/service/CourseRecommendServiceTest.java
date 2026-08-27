package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CoursePlaceResponse;
import com.chaerok.backend.course.dto.CourseRecommendResponse;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.KakaoLocalClient;
import com.chaerok.backend.place.external.KakaoPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRecommendServiceTest {

    private static final Long REGION_ID = 1L;
    private static final Long ANCHOR_ID = 10L;

    private static final BigDecimal ANCHOR_LATITUDE =
            new BigDecimal("36.4500");

    private static final BigDecimal ANCHOR_LONGITUDE =
            new BigDecimal("127.1200");

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private KakaoLocalClient kakaoLocalClient;

    @Mock
    private Place anchor;

    @Mock
    private Region region;

    private CourseRecommendService courseRecommendService;

    @BeforeEach
    void setUp() {
        courseRecommendService = new CourseRecommendService(
                regionRepository,
                placeRepository,
                kakaoLocalClient
        );
    }

    @Test
    @DisplayName("기본 반경에 같은 시군 음식점이 있으면 해당 후보를 사용한다")
    void recommendUsesSameRegionCandidateInDefaultRadius() {
        // given
        mockAnchor();

        KakaoPlaceItem food = createKakaoPlace(
                "food-1",
                "공주 식당",
                "FD6",
                "충남 공주시 웅진로 10"
        );

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                2000
        )).thenReturn(List.of(food));

        mockEmptyCafeSearch();

        // when
        CourseRecommendResponse response =
                courseRecommendService.recommendCourses(
                        REGION_ID,
                        ANCHOR_ID
                );

        // then
        List<CoursePlaceResponse> places =
                response.courses().get(0).places();

        assertThat(places)
                .extracting(CoursePlaceResponse::title)
                .contains("공주 식당");

        verify(kakaoLocalClient, never())
                .searchPlacesByCategory(
                        "FD6",
                        ANCHOR_LONGITUDE,
                        ANCHOR_LATITUDE,
                        5000
                );
    }

    @Test
    @DisplayName("기본 반경에 다른 시군 음식점만 있으면 확장 반경을 조회한다")
    void recommendExpandsRadiusWhenDefaultCandidatesAreOutsideRegion() {
        // given
        mockAnchor();

        KakaoPlaceItem outsideFood = createKakaoPlace(
                "food-outside",
                "논산 식당",
                "FD6",
                "충남 논산시 중앙로 10"
        );

        KakaoPlaceItem expandedFood = createKakaoPlace(
                "food-expanded",
                "공주 확장 식당",
                "FD6",
                "충남 공주시 금성동 20"
        );

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                2000
        )).thenReturn(List.of(outsideFood));

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                5000
        )).thenReturn(List.of(expandedFood));

        mockEmptyCafeSearch();

        // when
        CourseRecommendResponse response =
                courseRecommendService.recommendCourses(
                        REGION_ID,
                        ANCHOR_ID
                );

        // then
        List<CoursePlaceResponse> places =
                response.courses().get(0).places();

        assertThat(places)
                .extracting(CoursePlaceResponse::title)
                .contains("공주 확장 식당")
                .doesNotContain("논산 식당");

        verify(kakaoLocalClient)
                .searchPlacesByCategory(
                        "FD6",
                        ANCHOR_LONGITUDE,
                        ANCHOR_LATITUDE,
                        5000
                );
    }

    @Test
    @DisplayName("확장 반경에서도 같은 시군 후보만 추천한다")
    void recommendFiltersExpandedCandidatesByRegion() {
        // given
        mockAnchor();

        KakaoPlaceItem outsideDefault = createKakaoPlace(
                "food-default-outside",
                "논산 기본 식당",
                "FD6",
                "충남 논산시 중앙로 10"
        );

        KakaoPlaceItem outsideExpanded = createKakaoPlace(
                "food-expanded-outside",
                "논산 확장 식당",
                "FD6",
                "충남 논산시 시민로 20"
        );

        KakaoPlaceItem sameRegionExpanded = createKakaoPlace(
                "food-expanded-same",
                "공주 확장 식당",
                "FD6",
                "충남 공주시 웅진로 30"
        );

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                2000
        )).thenReturn(List.of(outsideDefault));

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                5000
        )).thenReturn(List.of(
                outsideExpanded,
                sameRegionExpanded
        ));

        mockEmptyCafeSearch();

        // when
        CourseRecommendResponse response =
                courseRecommendService.recommendCourses(
                        REGION_ID,
                        ANCHOR_ID
                );

        // then
        List<CoursePlaceResponse> places =
                response.courses().get(0).places();

        assertThat(places)
                .extracting(CoursePlaceResponse::title)
                .contains("공주 확장 식당")
                .doesNotContain(
                        "논산 기본 식당",
                        "논산 확장 식당"
                );
    }

    @Test
    @DisplayName("기본 및 확장 반경에 다른 시군 음식점만 있으면 음식점을 추천하지 않는다")
    void recommendDoesNotUseOutsideRegionCandidate() {
        // given
        mockAnchor();

        KakaoPlaceItem outsideDefault = createKakaoPlace(
                "food-outside-1",
                "논산 식당 1",
                "FD6",
                "충남 논산시 중앙로 10"
        );

        KakaoPlaceItem outsideExpanded = createKakaoPlace(
                "food-outside-2",
                "논산 식당 2",
                "FD6",
                "충남 논산시 시민로 20"
        );

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                2000
        )).thenReturn(List.of(outsideDefault));

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                5000
        )).thenReturn(List.of(outsideExpanded));

        mockEmptyCafeSearch();

        // when
        CourseRecommendResponse response =
                courseRecommendService.recommendCourses(
                        REGION_ID,
                        ANCHOR_ID
                );

        // then
        List<CoursePlaceResponse> places =
                response.courses().get(0).places();

        assertThat(places)
                .noneMatch(place ->
                        PlaceCategoryGroup.FOOD.name()
                                .equals(place.categoryGroup())
                );

        assertThat(places)
                .extracting(CoursePlaceResponse::title)
                .doesNotContain(
                        "논산 식당 1",
                        "논산 식당 2"
                );
    }

    private void mockAnchor() {
        when(regionRepository.existsById(REGION_ID))
                .thenReturn(true);

        when(placeRepository.findByRegionIdAndRepresentativeTrue(REGION_ID))
                .thenReturn(List.of(anchor));

        when(placeRepository.findById(ANCHOR_ID))
                .thenReturn(Optional.of(anchor));

        when(anchor.getRegion())
                .thenReturn(region);

        when(region.getId())
                .thenReturn(REGION_ID);

        when(region.getCityCountyName())
                .thenReturn("공주시");

        when(anchor.getId())
                .thenReturn(ANCHOR_ID);

        when(anchor.isRepresentative())
                .thenReturn(true);

        when(anchor.getLatitude())
                .thenReturn(ANCHOR_LATITUDE);

        when(anchor.getLongitude())
                .thenReturn(ANCHOR_LONGITUDE);

        when(anchor.getTitle())
                .thenReturn("공산성");

        when(anchor.getAddress())
                .thenReturn("충남 공주시 금성동");

        when(anchor.getCategoryGroup())
                .thenReturn(PlaceCategoryGroup.TOURISM);

        when(anchor.getCategoryDetail())
                .thenReturn(PlaceCategoryDetail.HERITAGE);

        when(anchor.getSource())
                .thenReturn(PlaceSource.TOUR_API);

        when(anchor.getTourContentId())
                .thenReturn("126204");
    }

    private void mockEmptyCafeSearch() {
        when(kakaoLocalClient.searchPlacesByCategory(
                "CE7",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                2000
        )).thenReturn(List.of());

        when(kakaoLocalClient.searchPlacesByCategory(
                "CE7",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                5000
        )).thenReturn(List.of());
    }

    private KakaoPlaceItem createKakaoPlace(
            String id,
            String placeName,
            String categoryGroupCode,
            String address
    ) {
        return new KakaoPlaceItem(
                id,
                placeName,
                "음식점 > 한식",
                categoryGroupCode,
                "음식점",
                address,
                address,
                "127.1200",
                "36.4500",
                "https://place.map.kakao.com/" + id
        );
    }
}