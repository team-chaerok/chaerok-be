package com.chaerok.backend.place.service;

import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.place.dto.PlaceDetailResponse;
import com.chaerok.backend.place.dto.PlaceListResponse;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.exception.PlaceErrorCode;
import com.chaerok.backend.place.external.KakaoLocalClient;
import com.chaerok.backend.place.external.KakaoPlaceItem;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.exception.RegionErrorCode;
import com.chaerok.backend.region.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private TourApiPlaceClient tourApiPlaceClient;

    @Mock
    private KakaoLocalClient kakaoLocalClient;

    @Mock
    private RegionCenterProvider regionCenterProvider;

    @Mock
    private Region region;

    @Mock
    private Place place;

    private PlaceService placeService;

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(
                placeRepository,
                regionRepository,
                tourApiPlaceClient,
                kakaoLocalClient,
                regionCenterProvider
        );
    }

    @Test
    @DisplayName("regionId 기준으로 대표 장소 목록을 반환한다")
    void getPlacesByRegion() {
        // given
        Long regionId = 1L;

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");

        when(placeRepository.findByRegionIdAndRepresentativeTrue(regionId))
                .thenReturn(List.of(place));

        mockPlaceForListResponse();

        TourApiPlaceItem tourApiItem = createTourApiPlaceItem();

        when(tourApiPlaceClient.getPlacesByContentIds(
                "44",
                "150",
                Set.of("1001")
        )).thenReturn(Map.of(
                "1001",
                tourApiItem
        ));

        // when
        List<PlaceListResponse> responses =
                placeService.getPlacesByRegion(regionId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).tourContentId()).isEqualTo("1001");
        assertThat(responses.get(0).title()).isEqualTo("TourAPI 공산성");
        assertThat(responses.get(0).address())
                .isEqualTo("TourAPI 충청남도 공주시 웅진로 280");
        assertThat(responses.get(0).latitude())
                .isEqualByComparingTo(new BigDecimal("36.4623000"));
        assertThat(responses.get(0).longitude())
                .isEqualByComparingTo(new BigDecimal("127.1248000"));
        assertThat(responses.get(0).firstImageUrl())
                .isEqualTo("https://tour-api.example.com/image.jpg");
        assertThat(responses.get(0).categoryGroup())
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(responses.get(0).categoryDetail())
                .isEqualTo(PlaceCategoryDetail.HERITAGE);

        verify(regionRepository).findById(regionId);
        verify(placeRepository)
                .findByRegionIdAndRepresentativeTrue(regionId);

        verify(tourApiPlaceClient).getPlacesByContentIds(
                "44",
                "150",
                Set.of("1001")
        );
    }

    @Test
    @DisplayName("TourAPI 매칭 결과가 없으면 DB 장소 정보로 대표 장소 목록을 반환한다")
    void getPlacesByRegionWithTourApiFallback() {
        // given
        Long regionId = 1L;

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");

        when(placeRepository.findByRegionIdAndRepresentativeTrue(regionId))
                .thenReturn(List.of(place));

        mockPlaceForListResponse();

        when(tourApiPlaceClient.getPlacesByContentIds(
                "44",
                "150",
                Set.of("1001")
        )).thenReturn(Map.of());

        // when
        List<PlaceListResponse> responses =
                placeService.getPlacesByRegion(regionId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).tourContentId()).isEqualTo("1001");
        assertThat(responses.get(0).title()).isEqualTo("공산성");
        assertThat(responses.get(0).address())
                .isEqualTo("충청남도 공주시 웅진로 280");
        assertThat(responses.get(0).firstImageUrl())
                .isEqualTo("https://example.com/image.jpg");
        assertThat(responses.get(0).categoryGroup())
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(responses.get(0).categoryDetail())
                .isEqualTo(PlaceCategoryDetail.HERITAGE);

        verify(regionRepository).findById(regionId);
        verify(placeRepository)
                .findByRegionIdAndRepresentativeTrue(regionId);

        verify(tourApiPlaceClient).getPlacesByContentIds(
                "44",
                "150",
                Set.of("1001")
        );
    }

    @Test
    @DisplayName("존재하지 않는 regionId로 장소 목록을 조회하면 예외가 발생한다")
    void getPlacesByRegionWithInvalidRegion() {
        // given
        Long regionId = 999L;

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> placeService.getPlacesByRegion(regionId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(RegionErrorCode.REGION_NOT_FOUND)
                );

        verify(regionRepository).findById(regionId);
        verifyNoInteractions(placeRepository);
        verifyNoInteractions(tourApiPlaceClient);
        verifyNoInteractions(kakaoLocalClient);
        verifyNoInteractions(regionCenterProvider);
    }

    @Test
    @DisplayName("추가 장소 조회 시 TourAPI 결과에서 카테고리별 장소를 반환한다")
    void getExternalPlaces() {
        // given
        Long regionId = 1L;

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");

        List<TourApiPlaceItem> items = List.of(
                createTourismItem(),
                createFoodItem(),
                createCafeItem()
        );

        when(tourApiPlaceClient.getPlacesByRegion("44", "150"))
                .thenReturn(items);

        RegionCenterProvider.RegionCenter center =
                new RegionCenterProvider.RegionCenter(
                        new BigDecimal("127.1190"),
                        new BigDecimal("36.4465")
                );

        when(regionCenterProvider.getCenter(region))
                .thenReturn(center);

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                center.longitude(),
                center.latitude(),
                20000
        )).thenReturn(List.of());

        when(kakaoLocalClient.searchPlacesByCategory(
                "CE7",
                center.longitude(),
                center.latitude(),
                20000
        )).thenReturn(List.of());

        // when
        List<PlaceListResponse> responses =
                placeService.getExternalPlaces(regionId);

        // then
        assertThat(responses).hasSize(3);

        assertThat(responses)
                .extracting(PlaceListResponse::categoryGroup)
                .containsExactly(
                        PlaceCategoryGroup.TOURISM,
                        PlaceCategoryGroup.FOOD,
                        PlaceCategoryGroup.CAFE_DESSERT
                );

        verify(tourApiPlaceClient)
                .getPlacesByRegion("44", "150");
    }

    @Test
    @DisplayName("TourAPI 음식점과 카페가 부족하면 Kakao Local 결과로 보완한다")
    void getExternalPlacesWithKakaoFallback() {
        // given
        Long regionId = 1L;

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");
        when(region.getCityCountyName()).thenReturn("공주시");

        when(tourApiPlaceClient.getPlacesByRegion("44", "150"))
                .thenReturn(List.of(createTourismItem()));

        RegionCenterProvider.RegionCenter center =
                new RegionCenterProvider.RegionCenter(
                        new BigDecimal("127.1190"),
                        new BigDecimal("36.4465")
                );

        when(regionCenterProvider.getCenter(region))
                .thenReturn(center);

        KakaoPlaceItem food = new KakaoPlaceItem(
                "kakao-food-1",
                "공주 맛집",
                "음식점 > 한식",
                "FD6",
                "음식점",
                "충청남도 공주시 중동 1",
                "충청남도 공주시 웅진로 1",
                "127.1200",
                "36.4500",
                "https://place.map.kakao.com/1"
        );

        KakaoPlaceItem cafe = new KakaoPlaceItem(
                "kakao-cafe-1",
                "공주 카페",
                "음식점 > 카페",
                "CE7",
                "카페",
                "충청남도 공주시 중동 2",
                "충청남도 공주시 웅진로 2",
                "127.1210",
                "36.4510",
                "https://place.map.kakao.com/2"
        );

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                center.longitude(),
                center.latitude(),
                20000
        )).thenReturn(List.of(food));

        when(kakaoLocalClient.searchPlacesByCategory(
                "CE7",
                center.longitude(),
                center.latitude(),
                20000
        )).thenReturn(List.of(cafe));

        // when
        List<PlaceListResponse> responses =
                placeService.getExternalPlaces(regionId);

        // then
        assertThat(responses).hasSize(3);

        assertThat(responses)
                .extracting(PlaceListResponse::source)
                .contains(
                        PlaceSource.TOUR_API,
                        PlaceSource.KAKAO_LOCAL
                );

        assertThat(responses)
                .extracting(PlaceListResponse::title)
                .contains(
                        "공산성",
                        "공주 맛집",
                        "공주 카페"
                );
    }

    @Test
    @DisplayName("다른 시군의 Kakao 장소는 추가 장소 결과에서 제외한다")
    void getExternalPlacesExcludesKakaoPlacesOutsideRegion() {
        // given
        Long regionId = 1L;

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");
        when(region.getCityCountyName()).thenReturn("공주시");

        when(tourApiPlaceClient.getPlacesByRegion("44", "150"))
                .thenReturn(List.of());

        RegionCenterProvider.RegionCenter center =
                new RegionCenterProvider.RegionCenter(
                        new BigDecimal("127.1190"),
                        new BigDecimal("36.4465")
                );

        when(regionCenterProvider.getCenter(region))
                .thenReturn(center);

        KakaoPlaceItem outsideFood = new KakaoPlaceItem(
                "kakao-food-outside",
                "논산 식당",
                "음식점 > 한식",
                "FD6",
                "음식점",
                "충청남도 논산시 취암동",
                "충청남도 논산시 시민로 1",
                "127.0000",
                "36.2000",
                "https://place.map.kakao.com/3"
        );

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                center.longitude(),
                center.latitude(),
                20000
        )).thenReturn(List.of(outsideFood));

        when(kakaoLocalClient.searchPlacesByCategory(
                "CE7",
                center.longitude(),
                center.latitude(),
                20000
        )).thenReturn(List.of());

        // when
        List<PlaceListResponse> responses =
                placeService.getExternalPlaces(regionId);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("지원하지 않는 지역이면 Kakao 보완 없이 TourAPI 결과만 반환한다")
    void getExternalPlacesSkipsKakaoWhenRegionCenterIsMissing() {
        // given
        Long regionId = 1L;

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("999");

        when(tourApiPlaceClient.getPlacesByRegion("44", "999"))
                .thenReturn(List.of(createTourismItem()));

        when(regionCenterProvider.getCenter(region))
                .thenReturn(null);

        // when
        List<PlaceListResponse> responses =
                placeService.getExternalPlaces(regionId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("공산성");

        verifyNoInteractions(kakaoLocalClient);
    }

    @Test
    @DisplayName("지원하지 않는 TourAPI 분류 장소는 추가 장소 결과에서 제외한다")
    void getExternalPlacesExcludesUnsupportedTourApiCategory() {
        // given
        Long regionId = 1L;

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");

        TourApiPlaceItem unsupportedItem = new TourApiPlaceItem(
                "3001",
                "일반 쇼핑 매장",
                "충청남도 공주시",
                "36.4500000",
                "127.1200000",
                null,
                "44",
                "150",
                "SH",
                "SH01",
                "SH010100",
                null
        );

        when(tourApiPlaceClient.getPlacesByRegion("44", "150"))
                .thenReturn(List.of(unsupportedItem));

        RegionCenterProvider.RegionCenter center =
                new RegionCenterProvider.RegionCenter(
                        new BigDecimal("127.1190"),
                        new BigDecimal("36.4465")
                );

        when(regionCenterProvider.getCenter(region))
                .thenReturn(center);

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                center.longitude(),
                center.latitude(),
                20000
        )).thenReturn(List.of());

        when(kakaoLocalClient.searchPlacesByCategory(
                "CE7",
                center.longitude(),
                center.latitude(),
                20000
        )).thenReturn(List.of());

        // when
        List<PlaceListResponse> responses =
                placeService.getExternalPlaces(regionId);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("관광지 제외 키워드가 포함된 장소는 추가 장소 결과에서 제외한다")
    void getExternalPlacesExcludesTourismKeyword() {
        // given
        Long regionId = 1L;

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");

        TourApiPlaceItem resort = new TourApiPlaceItem(
                "3002",
                "공주리조트",
                "충청남도 공주시",
                "36.4500000",
                "127.1200000",
                null,
                "44",
                "150",
                "VE",
                "VE01",
                "VE010100",
                null
        );

        when(tourApiPlaceClient.getPlacesByRegion("44", "150"))
                .thenReturn(List.of(resort));

        RegionCenterProvider.RegionCenter center =
                new RegionCenterProvider.RegionCenter(
                        new BigDecimal("127.1190"),
                        new BigDecimal("36.4465")
                );

        when(regionCenterProvider.getCenter(region))
                .thenReturn(center);

        when(kakaoLocalClient.searchPlacesByCategory(
                "FD6",
                center.longitude(),
                center.latitude(),
                20000
        )).thenReturn(List.of());

        when(kakaoLocalClient.searchPlacesByCategory(
                "CE7",
                center.longitude(),
                center.latitude(),
                20000
        )).thenReturn(List.of());

        // when
        List<PlaceListResponse> responses =
                placeService.getExternalPlaces(regionId);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("placeId 기준으로 TourAPI 상세 정보를 우선 반영한 장소 상세 정보를 반환한다")
    void getPlace() {
        // given
        Long placeId = 1L;

        when(placeRepository.findById(placeId))
                .thenReturn(Optional.of(place));

        mockPlaceForDetailResponse();

        TourApiPlaceItem tourApiItem = createTourApiPlaceItem();

        when(tourApiPlaceClient.getPlaceDetail("1001"))
                .thenReturn(tourApiItem);

        // when
        PlaceDetailResponse response =
                placeService.getPlace(placeId);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.regionId()).isEqualTo(1L);
        assertThat(response.tourContentId()).isEqualTo("1001");
        assertThat(response.title()).isEqualTo("TourAPI 공산성");
        assertThat(response.address())
                .isEqualTo("TourAPI 충청남도 공주시 웅진로 280");
        assertThat(response.firstImageUrl())
                .isEqualTo("https://tour-api.example.com/image.jpg");
        assertThat(response.overview())
                .isEqualTo("TourAPI 공산성 소개 문구입니다.");
        assertThat(response.categoryGroup())
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(response.categoryDetail())
                .isEqualTo(PlaceCategoryDetail.HERITAGE);
        assertThat(response.source())
                .isEqualTo(PlaceSource.TOUR_API);
        assertThat(response.isRepresentative()).isFalse();

        verify(placeRepository).findById(placeId);
        verify(tourApiPlaceClient).getPlaceDetail("1001");
    }

    @Test
    @DisplayName("TourAPI 상세 조회 결과가 없으면 DB 장소 정보로 장소 상세 정보를 반환한다")
    void getPlaceWithTourApiFallback() {
        // given
        Long placeId = 1L;

        when(placeRepository.findById(placeId))
                .thenReturn(Optional.of(place));

        mockPlaceForDetailResponse();

        when(tourApiPlaceClient.getPlaceDetail("1001"))
                .thenReturn(null);

        // when
        PlaceDetailResponse response =
                placeService.getPlace(placeId);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.regionId()).isEqualTo(1L);
        assertThat(response.tourContentId()).isEqualTo("1001");
        assertThat(response.title()).isEqualTo("공산성");
        assertThat(response.address())
                .isEqualTo("충청남도 공주시 웅진로 280");
        assertThat(response.overview()).isNull();
        assertThat(response.categoryGroup())
                .isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(response.categoryDetail())
                .isEqualTo(PlaceCategoryDetail.HERITAGE);
        assertThat(response.source())
                .isEqualTo(PlaceSource.TOUR_API);
        assertThat(response.isRepresentative()).isFalse();

        verify(placeRepository).findById(placeId);
        verify(tourApiPlaceClient).getPlaceDetail("1001");
    }

    @Test
    @DisplayName("존재하지 않는 placeId로 장소 상세를 조회하면 예외가 발생한다")
    void getPlaceWithInvalidPlaceId() {
        // given
        Long placeId = 999L;

        when(placeRepository.findById(placeId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> placeService.getPlace(placeId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND)
                );

        verify(placeRepository).findById(placeId);
        verifyNoInteractions(tourApiPlaceClient);
    }

    @Test
    @DisplayName("존재하지 않는 regionId로 추가 장소를 조회하면 예외가 발생한다")
    void getExternalPlacesWithInvalidRegion() {
        // given
        Long regionId = 999L;

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> placeService.getExternalPlaces(regionId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(RegionErrorCode.REGION_NOT_FOUND)
                );

        verify(regionRepository).findById(regionId);
        verifyNoInteractions(tourApiPlaceClient);
        verifyNoInteractions(kakaoLocalClient);
        verifyNoInteractions(regionCenterProvider);
    }

    private void mockPlaceForListResponse() {
        when(place.getId()).thenReturn(1L);
        when(place.getTourContentId()).thenReturn("1001");
        when(place.getTitle()).thenReturn("공산성");
        when(place.getAddress())
                .thenReturn("충청남도 공주시 웅진로 280");
        when(place.getLatitude())
                .thenReturn(new BigDecimal("36.4623000"));
        when(place.getLongitude())
                .thenReturn(new BigDecimal("127.1248000"));
        when(place.getFirstImageUrl())
                .thenReturn("https://example.com/image.jpg");
        when(place.getCategoryGroup())
                .thenReturn(PlaceCategoryGroup.TOURISM);
        when(place.getCategoryDetail())
                .thenReturn(PlaceCategoryDetail.HERITAGE);
        when(place.isRepresentative()).thenReturn(false);
    }

    private void mockPlaceForDetailResponse() {
        mockPlaceForListResponse();

        when(place.getRegion()).thenReturn(region);
        when(region.getId()).thenReturn(1L);

        when(place.getLDongRegnCd()).thenReturn("44");
        when(place.getLDongSignguCd()).thenReturn("150");
        when(place.getLclsSystm1()).thenReturn("HS");
        when(place.getLclsSystm2()).thenReturn("HS01");
        when(place.getLclsSystm3()).thenReturn("HS010100");
        when(place.getSource()).thenReturn(PlaceSource.TOUR_API);
    }

    private TourApiPlaceItem createTourApiPlaceItem() {
        return new TourApiPlaceItem(
                "1001",
                "TourAPI 공산성",
                "TourAPI 충청남도 공주시 웅진로 280",
                "36.4623000",
                "127.1248000",
                "https://tour-api.example.com/image.jpg",
                "44",
                "150",
                "HS",
                "HS01",
                "HS010100",
                "TourAPI 공산성 소개 문구입니다."
        );
    }

    private TourApiPlaceItem createTourismItem() {
        return new TourApiPlaceItem(
                "2001",
                "공산성",
                "충청남도 공주시 웅진로 280",
                "36.4623000",
                "127.1248000",
                "https://example.com/tourism.jpg",
                "44",
                "150",
                "HS",
                "HS01",
                "HS010100",
                null
        );
    }

    private TourApiPlaceItem createFoodItem() {
        return new TourApiPlaceItem(
                "2002",
                "공주 음식점",
                "충청남도 공주시 중동",
                "36.4500000",
                "127.1200000",
                null,
                "44",
                "150",
                "FD",
                "FD01",
                "FD010100",
                null
        );
    }

    private TourApiPlaceItem createCafeItem() {
        return new TourApiPlaceItem(
                "2003",
                "공주 카페",
                "충청남도 공주시 중동",
                "36.4510000",
                "127.1210000",
                null,
                "44",
                "150",
                "FD",
                "FD05",
                "FD050100",
                null
        );
    }
}