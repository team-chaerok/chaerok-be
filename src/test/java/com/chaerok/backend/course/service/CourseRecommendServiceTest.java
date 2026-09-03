package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CoursePlaceResponse;
import com.chaerok.backend.course.dto.CourseRecommendResponse;
import com.chaerok.backend.global.exception.BusinessException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        mockDefaultCafeSearch();

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

        mockDefaultCafeSearch();

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

        mockDefaultCafeSearch();

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
    @DisplayName("음식점 후보와 fallback이 모두 없으면 불완전한 코스를 추천하지 않는다")
    void recommendExcludesIncompleteCourseWhenFoodIsUnavailable() {
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

        mockDefaultCafeSearch();

        // when
        CourseRecommendResponse response =
                courseRecommendService.recommendCourses(
                        REGION_ID,
                        ANCHOR_ID
                );

        // then
        assertThat(response.courses()).isEmpty();
    }

    @Test
    @DisplayName("카카오 음식점 후보가 없으면 같은 유형의 DB 대표 장소를 fallback으로 사용한다")
    void recommendUsesFoodFallbackFromDatabase() {
        // given
        mockAnchor();

        Place foodFallback = mockPlace(
                20L,
                "공주 DB 식당",
                PlaceCategoryGroup.FOOD,
                PlaceCategoryDetail.RESTAURANT,
                "36.4510",
                "127.1210"
        );

        when(placeRepository.findByRegionIdAndRepresentativeTrue(REGION_ID))
                .thenReturn(List.of(anchor, foodFallback));

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                2000
        )).thenReturn(List.of());

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                5000
        )).thenReturn(List.of());

        mockDefaultCafeSearch();

        // when
        CourseRecommendResponse response =
                courseRecommendService.recommendCourses(
                        REGION_ID,
                        ANCHOR_ID
                );

        // then
        assertThat(response.courses())
                .hasSize(1);

        assertThat(response.courses().get(0).places())
                .extracting(CoursePlaceResponse::title)
                .contains("공주 DB 식당");
    }

    private Place mockPlace(
            Long id,
            String title,
            PlaceCategoryGroup categoryGroup,
            PlaceCategoryDetail categoryDetail,
            String latitude,
            String longitude
    ) {
        Place place = org.mockito.Mockito.mock(Place.class);

        when(place.getId()).thenReturn(id);
        when(place.getTitle()).thenReturn(title);
        when(place.getAddress()).thenReturn("충남 공주시");
        when(place.getLatitude()).thenReturn(new BigDecimal(latitude));
        when(place.getLongitude()).thenReturn(new BigDecimal(longitude));
        when(place.getCategoryGroup()).thenReturn(categoryGroup);
        when(place.getCategoryDetail()).thenReturn(categoryDetail);
        when(place.getSource()).thenReturn(PlaceSource.TOUR_API);

        return place;
    }

    @Test
    @DisplayName("카카오 카페 후보가 없으면 같은 유형의 DB 대표 장소를 fallback으로 사용한다")
    void recommendUsesCafeFallbackFromDatabase() {
        // given
        mockAnchor();

        Place cafeFallback = mockPlace(
                30L,
                "공주 DB 카페",
                PlaceCategoryGroup.CAFE_DESSERT,
                PlaceCategoryDetail.CAFE,
                "36.4520",
                "127.1220"
        );

        when(placeRepository.findByRegionIdAndRepresentativeTrue(REGION_ID))
                .thenReturn(List.of(anchor, cafeFallback));

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

        // when
        CourseRecommendResponse response =
                courseRecommendService.recommendCourses(
                        REGION_ID,
                        ANCHOR_ID
                );

        // then
        assertThat(response.courses()).hasSize(1);

        assertThat(response.courses().get(0).places())
                .extracting(CoursePlaceResponse::title)
                .contains("공주 DB 카페");
    }

    @Test
    @DisplayName("관광지가 아닌 대표 장소는 Anchor로 사용할 수 없다")
    void recommendRejectsNonTourismAnchor() {
        // given
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

        when(anchor.isRepresentative())
                .thenReturn(true);

        when(anchor.getCategoryGroup())
                .thenReturn(PlaceCategoryGroup.FOOD);

        // when & then
        assertThatThrownBy(() ->
                courseRecommendService.recommendCourses(
                        REGION_ID,
                        ANCHOR_ID
                )
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "추천 코스의 앵커 장소는 관광지 유형이어야 합니다."
                );
    }

    @Test
    @DisplayName("첫 번째 Anchor 코스가 제외되면 실제 첫 추천 코스의 Anchor ID를 반환한다")
    void recommendUsesFirstValidAnchorIdAfterFilteringIncompleteCourse() {
        // given
        Place firstAnchor = org.mockito.Mockito.mock(Place.class);
        Place secondAnchor = org.mockito.Mockito.mock(Place.class);

        mockRegionAnchor(
                firstAnchor,
                10L,
                "첫 번째 관광지",
                PlaceCategoryDetail.HERITAGE,
                "36.4500",
                "127.1200"
        );

        mockRegionAnchor(
                secondAnchor,
                20L,
                "두 번째 관광지",
                PlaceCategoryDetail.NATURE,
                "36.4600",
                "127.1300"
        );

        when(regionRepository.existsById(REGION_ID))
                .thenReturn(true);

        when(placeRepository.findByRegionIdAndRepresentativeTrue(REGION_ID))
                .thenReturn(List.of(firstAnchor, secondAnchor));

        // 첫 번째 Anchor: FOOD/CAFE 모두 실패 → 불완전 코스
        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                new BigDecimal("127.1200"),
                new BigDecimal("36.4500"),
                2000
        )).thenReturn(List.of());

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                new BigDecimal("127.1200"),
                new BigDecimal("36.4500"),
                5000
        )).thenReturn(List.of());

        when(kakaoLocalClient.searchPlacesByCategory(
                "CE7",
                new BigDecimal("127.1200"),
                new BigDecimal("36.4500"),
                2000
        )).thenReturn(List.of());

        when(kakaoLocalClient.searchPlacesByCategory(
                "CE7",
                new BigDecimal("127.1200"),
                new BigDecimal("36.4500"),
                5000
        )).thenReturn(List.of());

        // 두 번째 Anchor: 정상 FOOD / CAFE 후보 존재
        KakaoPlaceItem food = createKakaoPlace(
                "food-2",
                "두 번째 식당",
                "FD6",
                "충남 공주시 웅진로 30"
        );

        KakaoPlaceItem cafe = createKakaoPlace(
                "cafe-2",
                "두 번째 카페",
                "CE7",
                "충남 공주시 웅진로 40"
        );

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                new BigDecimal("127.1300"),
                new BigDecimal("36.4600"),
                2000
        )).thenReturn(List.of(food));

        when(kakaoLocalClient.searchPlacesByCategory(
                "CE7",
                new BigDecimal("127.1300"),
                new BigDecimal("36.4600"),
                2000
        )).thenReturn(List.of(cafe));

        // when
        CourseRecommendResponse response =
                courseRecommendService.recommendCourses(
                        REGION_ID,
                        null
                );

        // then
        assertThat(response.courses()).hasSize(1);
        assertThat(response.anchorPlaceId()).isEqualTo(20L);
        assertThat(response.courses().get(0).places())
                .extracting(CoursePlaceResponse::title)
                .contains(
                        "두 번째 관광지",
                        "두 번째 식당",
                        "두 번째 카페"
                );
    }

    private void mockRegionAnchor(
            Place place,
            Long id,
            String title,
            PlaceCategoryDetail categoryDetail,
            String latitude,
            String longitude
    ) {
        when(place.getId()).thenReturn(id);
        when(place.getRegion()).thenReturn(region);
        when(place.getLatitude()).thenReturn(new BigDecimal(latitude));
        when(place.getLongitude()).thenReturn(new BigDecimal(longitude));
        when(place.getTitle()).thenReturn(title);
        when(place.getAddress()).thenReturn("충남 공주시");
        when(place.getCategoryGroup()).thenReturn(PlaceCategoryGroup.TOURISM);
        when(place.getCategoryDetail()).thenReturn(categoryDetail);
        when(place.getSource()).thenReturn(PlaceSource.TOUR_API);

        when(region.getCityCountyName())
                .thenReturn("공주시");
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

    private void mockDefaultCafeSearch() {
        KakaoPlaceItem cafe = createKakaoPlace(
                "cafe-1",
                "공주 카페",
                "CE7",
                "충남 공주시 웅진로 20"
        );

        when(kakaoLocalClient.searchPlacesByCategory(
                "CE7",
                ANCHOR_LONGITUDE,
                ANCHOR_LATITUDE,
                2000
        )).thenReturn(List.of(cafe));
    }

    private KakaoPlaceItem createKakaoPlace(
            String id,
            String placeName,
            String categoryGroupCode,
            String address
    ) {
        boolean isCafe = "CE7".equals(categoryGroupCode);

        return new KakaoPlaceItem(
                id,
                placeName,
                isCafe ? "카페 > 커피전문점" : "음식점 > 한식",
                categoryGroupCode,
                isCafe ? "카페" : "음식점",
                address,
                address,
                "127.1200",
                "36.4500",
                "https://place.map.kakao.com/" + id
        );
    }
}