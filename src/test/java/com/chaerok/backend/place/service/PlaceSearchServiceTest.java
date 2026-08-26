package com.chaerok.backend.place.service;

import com.chaerok.backend.place.dto.PlaceSearchResponse;
import com.chaerok.backend.place.external.KakaoLocalClient;
import com.chaerok.backend.place.external.KakaoPlaceItem;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceSearchServiceTest {

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

    private PlaceSearchService placeSearchService;

    @BeforeEach
    void setUp() {
        placeSearchService = new PlaceSearchService(
                regionRepository,
                tourApiPlaceClient,
                kakaoLocalClient,
                regionCenterProvider
        );
    }

    @Test
    @DisplayName("TourAPI 검색 결과가 5개 이상이면 Kakao 검색을 호출하지 않는다")
    void searchPlacesReturnsTourApiOnlyWhenEnoughResults() {
        // given
        Long regionId = 1L;
        String keyword = "공주";

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");

        List<TourApiPlaceItem> items = List.of(
                createTourApiItem("1001", "공산성"),
                createTourApiItem("1002", "무령왕릉"),
                createTourApiItem("1003", "마곡사"),
                createTourApiItem("1004", "제민천"),
                createTourApiItem("1005", "석장리박물관")
        );

        when(tourApiPlaceClient.searchPlacesByKeyword(
                keyword,
                "44",
                "150"
        )).thenReturn(items);

        // when
        List<PlaceSearchResponse> responses =
                placeSearchService.searchPlaces(regionId, keyword);

        // then
        assertThat(responses).hasSize(5);

        assertThat(responses)
                .extracting(PlaceSearchResponse::title)
                .containsExactly(
                        "공산성",
                        "무령왕릉",
                        "마곡사",
                        "제민천",
                        "석장리박물관"
                );

        verifyNoInteractions(kakaoLocalClient);
        verifyNoInteractions(regionCenterProvider);
    }

    @Test
    @DisplayName("지원하지 않는 TourAPI 분류 장소는 검색 결과에서 제외한다")
    void searchPlacesExcludesUnsupportedTourApiCategory() {
        // given
        Long regionId = 1L;
        String keyword = "공주";

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");

        TourApiPlaceItem supportedItem = createTourApiItem(
                "1001",
                "공산성"
        );

        TourApiPlaceItem unsupportedItem = new TourApiPlaceItem(
                "1002",
                "일반 쇼핑 매장",
                "충청남도 공주시",
                "36.4623",
                "127.1248",
                null,
                "44",
                "150",
                "SH",
                "SH01",
                "SH010100",
                null
        );

        when(tourApiPlaceClient.searchPlacesByKeyword(
                keyword,
                "44",
                "150"
        )).thenReturn(List.of(
                supportedItem,
                unsupportedItem
        ));

        RegionCenterProvider.RegionCenter center =
                new RegionCenterProvider.RegionCenter(
                        new BigDecimal("127.1190"),
                        new BigDecimal("36.4465")
                );

        when(regionCenterProvider.getCenter(region))
                .thenReturn(center);

        when(kakaoLocalClient.searchPlacesByKeyword(
                keyword,
                center.longitude(),
                center.latitude()
        )).thenReturn(List.of());

        // when
        List<PlaceSearchResponse> responses =
                placeSearchService.searchPlaces(regionId, keyword);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("공산성");
    }

    @Test
    @DisplayName("TourAPI 검색 결과가 부족하면 Kakao 검색 결과로 보완한다")
    void searchPlacesSupplementsWithKakao() {
        // given
        Long regionId = 1L;
        String keyword = "카페";

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");

        when(tourApiPlaceClient.searchPlacesByKeyword(
                keyword,
                "44",
                "150"
        )).thenReturn(List.of(
                createTourApiItem("1001", "공주 카페")
        ));

        RegionCenterProvider.RegionCenter center =
                new RegionCenterProvider.RegionCenter(
                        new BigDecimal("127.1190"),
                        new BigDecimal("36.4465")
                );

        when(regionCenterProvider.getCenter(region))
                .thenReturn(center);

        KakaoPlaceItem kakaoItem = new KakaoPlaceItem(
                "kakao-1",
                "제민천 카페",
                "음식점 > 카페",
                "CE7",
                "카페",
                "충청남도 공주시 중동",
                "충청남도 공주시 웅진로 10",
                "127.1200",
                "36.4500",
                "https://place.map.kakao.com/1"
        );

        when(kakaoLocalClient.searchPlacesByKeyword(
                keyword,
                center.longitude(),
                center.latitude()
        )).thenReturn(List.of(kakaoItem));

        // when
        List<PlaceSearchResponse> responses =
                placeSearchService.searchPlaces(regionId, keyword);

        // then
        assertThat(responses).hasSize(2);

        assertThat(responses)
                .extracting(PlaceSearchResponse::title)
                .containsExactly(
                        "공주 카페",
                        "제민천 카페"
                );
    }

    @Test
    @DisplayName("지원하지 않는 지역이면 Kakao 검색 없이 TourAPI 검색 결과만 반환한다")
    void searchPlacesSkipsKakaoWhenRegionCenterIsMissing() {
        // given
        Long regionId = 1L;
        String keyword = "공산성";

        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("999");

        when(tourApiPlaceClient.searchPlacesByKeyword(
                keyword,
                "44",
                "999"
        )).thenReturn(List.of(
                createTourApiItem("1001", "공산성")
        ));

        when(regionCenterProvider.getCenter(region))
                .thenReturn(null);

        // when
        List<PlaceSearchResponse> responses =
                placeSearchService.searchPlaces(regionId, keyword);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("공산성");

        verifyNoInteractions(kakaoLocalClient);
    }

    private TourApiPlaceItem createTourApiItem(
            String contentId,
            String title
    ) {
        return new TourApiPlaceItem(
                contentId,
                title,
                "충청남도 공주시",
                "36.4623",
                "127.1248",
                null,
                "44",
                "150",
                "HS",
                "HS01",
                "HS010100",
                null
        );
    }
}