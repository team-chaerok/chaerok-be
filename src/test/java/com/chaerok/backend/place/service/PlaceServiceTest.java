package com.chaerok.backend.place.service;

import com.chaerok.backend.place.dto.PlaceDetailResponse;
import com.chaerok.backend.place.dto.PlaceListResponse;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
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
    private PlaceSyncService placeSyncService;

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
                placeSyncService
        );
    }

    @Test
    @DisplayName("regionId 기준으로 TourAPI 장소를 동기화한 뒤 장소 목록을 반환한다")
    void getPlacesByRegion() {
        // given
        Long regionId = 1L;

        TourApiPlaceItem item = new TourApiPlaceItem(
                "1001",
                "공산성",
                "충청남도 공주시 웅진로 280",
                "36.4623",
                "127.1248",
                "https://example.com/image.jpg",
                "44",
                "150",
                "HS",
                "HS01",
                "HS010100"
        );

        when(regionRepository.findById(regionId)).thenReturn(Optional.of(region));
        when(region.getLdongRegnCd()).thenReturn("44");
        when(region.getLdongSignguCd()).thenReturn("150");
        when(tourApiPlaceClient.getPlacesByRegion("44", "150")).thenReturn(List.of(item));

        when(placeRepository.findByRegionId(regionId)).thenReturn(List.of(place));
        mockPlaceForListResponse();

        // when
        List<PlaceListResponse> responses = placeService.getPlacesByRegion(regionId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).title()).isEqualTo("공산성");
        assertThat(responses.get(0).categoryGroup()).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(responses.get(0).categoryDetail()).isEqualTo(PlaceCategoryDetail.HERITAGE);
        assertThat(responses.get(0).isRepresentative()).isFalse();

        verify(regionRepository).findById(regionId);
        verify(tourApiPlaceClient).getPlacesByRegion("44", "150");
        verify(placeSyncService).syncPlaces(region, List.of(item));
        verify(placeRepository).findByRegionId(regionId);
    }

    @Test
    @DisplayName("존재하지 않는 regionId로 장소 목록을 조회하면 예외가 발생한다")
    void getPlacesByRegionWithInvalidRegion() {
        // given
        Long regionId = 999L;

        when(regionRepository.findById(regionId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> placeService.getPlacesByRegion(regionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지역을 찾을 수 없습니다.");

        verify(regionRepository).findById(regionId);
        verifyNoInteractions(tourApiPlaceClient);
        verifyNoInteractions(placeSyncService);
        verifyNoInteractions(placeRepository);
    }

    @Test
    @DisplayName("placeId 기준으로 장소 상세 정보를 반환한다")
    void getPlace() {
        // given
        Long placeId = 1L;

        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
        mockPlaceForDetailResponse();

        // when
        PlaceDetailResponse response = placeService.getPlace(placeId);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.regionId()).isEqualTo(1L);
        assertThat(response.tourContentId()).isEqualTo("1001");
        assertThat(response.title()).isEqualTo("공산성");
        assertThat(response.categoryGroup()).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(response.categoryDetail()).isEqualTo(PlaceCategoryDetail.HERITAGE);
        assertThat(response.source()).isEqualTo(PlaceSource.TOUR_API);
        assertThat(response.isRepresentative()).isFalse();

        verify(placeRepository).findById(placeId);
    }

    @Test
    @DisplayName("존재하지 않는 placeId로 장소 상세를 조회하면 예외가 발생한다")
    void getPlaceWithInvalidPlaceId() {
        // given
        Long placeId = 999L;

        when(placeRepository.findById(placeId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> placeService.getPlace(placeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("장소를 찾을 수 없습니다.");

        verify(placeRepository).findById(placeId);
    }

    private void mockPlaceForListResponse() {
        when(place.getId()).thenReturn(1L);
        when(place.getTitle()).thenReturn("공산성");
        when(place.getAddress()).thenReturn("충청남도 공주시 웅진로 280");
        when(place.getLatitude()).thenReturn(new BigDecimal("36.4623000"));
        when(place.getLongitude()).thenReturn(new BigDecimal("127.1248000"));
        when(place.getFirstImageUrl()).thenReturn("https://example.com/image.jpg");
        when(place.getCategoryGroup()).thenReturn(PlaceCategoryGroup.TOURISM);
        when(place.getCategoryDetail()).thenReturn(PlaceCategoryDetail.HERITAGE);
        when(place.isRepresentative()).thenReturn(false);
    }

    private void mockPlaceForDetailResponse() {
        mockPlaceForListResponse();

        when(place.getRegion()).thenReturn(region);
        when(region.getId()).thenReturn(1L);

        when(place.getTourContentId()).thenReturn("1001");
        when(place.getLDongRegnCd()).thenReturn("44");
        when(place.getLDongSignguCd()).thenReturn("150");
        when(place.getLclsSystm1()).thenReturn("HS");
        when(place.getLclsSystm2()).thenReturn("HS01");
        when(place.getLclsSystm3()).thenReturn("HS010100");
        when(place.getSource()).thenReturn(PlaceSource.TOUR_API);
    }
}