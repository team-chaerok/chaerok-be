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
    private Region region;

    @Mock
    private Place place;

    private PlaceService placeService;

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(
                placeRepository,
                regionRepository,
                tourApiPlaceClient
        );
    }

    @Test
    @DisplayName("regionId 기준으로 대표 장소 목록을 반환한다")
    void getPlacesByRegion() {
        // given
        Long regionId = 1L;

        when(regionRepository.existsById(regionId)).thenReturn(true);
        when(placeRepository.findByRegionIdAndRepresentativeTrue(regionId)).thenReturn(List.of(place));
        mockPlaceForListResponse();

        TourApiPlaceItem tourApiItem = createTourApiPlaceItem();
        when(tourApiPlaceClient.getPlaceDetail("1001")).thenReturn(tourApiItem);

        // when
        List<PlaceListResponse> responses = placeService.getPlacesByRegion(regionId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).tourContentId()).isEqualTo("1001");
        assertThat(responses.get(0).title()).isEqualTo("TourAPI 공산성");
        assertThat(responses.get(0).address()).isEqualTo("TourAPI 충청남도 공주시 웅진로 280");
        assertThat(responses.get(0).latitude()).isEqualByComparingTo(new BigDecimal("36.4623000"));
        assertThat(responses.get(0).longitude()).isEqualByComparingTo(new BigDecimal("127.1248000"));
        assertThat(responses.get(0).firstImageUrl()).isEqualTo("https://tour-api.example.com/image.jpg");
        assertThat(responses.get(0).categoryGroup()).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(responses.get(0).categoryDetail()).isEqualTo(PlaceCategoryDetail.HERITAGE);

        verify(regionRepository).existsById(regionId);
        verify(placeRepository).findByRegionIdAndRepresentativeTrue(regionId);
        verify(tourApiPlaceClient).getPlaceDetail("1001");
    }

    @Test
    @DisplayName("TourAPI 상세 조회 결과가 없으면 DB 장소 정보로 대표 장소 목록을 반환한다")
    void getPlacesByRegionWithTourApiFallback() {
        // given
        Long regionId = 1L;

        when(regionRepository.existsById(regionId)).thenReturn(true);
        when(placeRepository.findByRegionIdAndRepresentativeTrue(regionId)).thenReturn(List.of(place));
        mockPlaceForListResponse();
        when(tourApiPlaceClient.getPlaceDetail("1001")).thenReturn(null);

        // when
        List<PlaceListResponse> responses = placeService.getPlacesByRegion(regionId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).tourContentId()).isEqualTo("1001");
        assertThat(responses.get(0).title()).isEqualTo("공산성");
        assertThat(responses.get(0).address()).isEqualTo("충청남도 공주시 웅진로 280");
        assertThat(responses.get(0).firstImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(responses.get(0).categoryGroup()).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(responses.get(0).categoryDetail()).isEqualTo(PlaceCategoryDetail.HERITAGE);

        verify(regionRepository).existsById(regionId);
        verify(placeRepository).findByRegionIdAndRepresentativeTrue(regionId);
        verify(tourApiPlaceClient).getPlaceDetail("1001");
    }

    @Test
    @DisplayName("존재하지 않는 regionId로 장소 목록을 조회하면 예외가 발생한다")
    void getPlacesByRegionWithInvalidRegion() {
        // given
        Long regionId = 999L;

        when(regionRepository.existsById(regionId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> placeService.getPlacesByRegion(regionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지역을 찾을 수 없습니다.");

        verify(regionRepository).existsById(regionId);
        verifyNoInteractions(placeRepository);
        verifyNoInteractions(tourApiPlaceClient);
    }

    @Test
    @DisplayName("placeId 기준으로 TourAPI 상세 정보를 우선 반영한 장소 상세 정보를 반환한다")
    void getPlace() {
        // given
        Long placeId = 1L;

        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
        mockPlaceForDetailResponse();

        TourApiPlaceItem tourApiItem = createTourApiPlaceItem();
        when(tourApiPlaceClient.getPlaceDetail("1001")).thenReturn(tourApiItem);

        // when
        PlaceDetailResponse response = placeService.getPlace(placeId);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.regionId()).isEqualTo(1L);
        assertThat(response.tourContentId()).isEqualTo("1001");
        assertThat(response.title()).isEqualTo("TourAPI 공산성");
        assertThat(response.address()).isEqualTo("TourAPI 충청남도 공주시 웅진로 280");
        assertThat(response.firstImageUrl()).isEqualTo("https://tour-api.example.com/image.jpg");
        assertThat(response.overview()).isEqualTo("TourAPI 공산성 소개 문구입니다.");
        assertThat(response.categoryGroup()).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(response.categoryDetail()).isEqualTo(PlaceCategoryDetail.HERITAGE);
        assertThat(response.source()).isEqualTo(PlaceSource.TOUR_API);
        assertThat(response.isRepresentative()).isFalse();

        verify(placeRepository).findById(placeId);
        verify(tourApiPlaceClient).getPlaceDetail("1001");
    }

    @Test
    @DisplayName("TourAPI 상세 조회 결과가 없으면 DB 장소 정보로 장소 상세 정보를 반환한다")
    void getPlaceWithTourApiFallback() {
        // given
        Long placeId = 1L;

        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
        mockPlaceForDetailResponse();
        when(tourApiPlaceClient.getPlaceDetail("1001")).thenReturn(null);

        // when
        PlaceDetailResponse response = placeService.getPlace(placeId);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.regionId()).isEqualTo(1L);
        assertThat(response.tourContentId()).isEqualTo("1001");
        assertThat(response.title()).isEqualTo("공산성");
        assertThat(response.address()).isEqualTo("충청남도 공주시 웅진로 280");
        assertThat(response.overview()).isNull();
        assertThat(response.categoryGroup()).isEqualTo(PlaceCategoryGroup.TOURISM);
        assertThat(response.categoryDetail()).isEqualTo(PlaceCategoryDetail.HERITAGE);
        assertThat(response.source()).isEqualTo(PlaceSource.TOUR_API);
        assertThat(response.isRepresentative()).isFalse();

        verify(placeRepository).findById(placeId);
        verify(tourApiPlaceClient).getPlaceDetail("1001");
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
        verifyNoInteractions(tourApiPlaceClient);
    }

    private void mockPlaceForListResponse() {
        when(place.getId()).thenReturn(1L);
        when(place.getTourContentId()).thenReturn("1001");
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
}