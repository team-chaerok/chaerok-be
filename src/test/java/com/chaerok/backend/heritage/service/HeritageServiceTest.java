package com.chaerok.backend.heritage.service;

import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.heritage.dto.HeritagePlaceResponse;
import com.chaerok.backend.heritage.exception.HeritageErrorCode;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.exception.PlaceErrorCode;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.entity.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeritageServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private TourApiPlaceClient tourApiPlaceClient;

    @Mock
    private Place place;

    @Mock
    private Region region;

    private HeritageService heritageService;

    @BeforeEach
    void setUp() {
        heritageService = new HeritageService(
                placeRepository,
                tourApiPlaceClient
        );
    }

    @Test
    void TourAPI_유적지_분류이면_유적지로_판별한다() {
        // given
        Long placeId = 1L;
        String contentId = "100";

        givenPlace(placeId, contentId);

        TourApiPlaceItem item = createTourApiItem(
                contentId,
                "HS"
        );

        when(tourApiPlaceClient.getPlaceDetail(contentId))
                .thenReturn(item);

        // when
        HeritagePlaceResponse response =
                heritageService.getHeritagePlace(placeId);

        // then
        assertThat(response.heritage()).isTrue();
    }

    @Test
    void TourAPI_유적지_분류가_아니면_일반_장소로_판별한다() {
        // given
        Long placeId = 1L;
        String contentId = "100";

        givenPlace(placeId, contentId);

        TourApiPlaceItem item = createTourApiItem(
                contentId,
                "EX"
        );

        when(tourApiPlaceClient.getPlaceDetail(contentId))
                .thenReturn(item);

        // when
        HeritagePlaceResponse response =
                heritageService.getHeritagePlace(placeId);

        // then
        assertThat(response.heritage()).isFalse();
    }

    @Test
    void 궁남지는_TourAPI_분류와_관계없이_유적지로_판별한다() {
        // given
        Long placeId = 1L;
        String contentId = "125984";

        givenPlace(placeId, contentId);

        TourApiPlaceItem item = createTourApiItem(
                contentId,
                "EX"
        );

        when(tourApiPlaceClient.getPlaceDetail(contentId))
                .thenReturn(item);

        // when
        HeritagePlaceResponse response =
                heritageService.getHeritagePlace(placeId);

        // then
        assertThat(response.heritage()).isTrue();
    }

    @Test
    void TourAPI_조회_실패시_DB가_HERITAGE이면_유적지로_fallback한다() {
        // given
        Long placeId = 1L;
        String contentId = "100";

        givenPlace(placeId, contentId);

        when(place.getCategoryDetail())
                .thenReturn(PlaceCategoryDetail.HERITAGE);
        when(place.getTitle()).thenReturn("공산성");
        when(place.getAddress()).thenReturn("충남 공주시");

        when(tourApiPlaceClient.getPlaceDetail(contentId))
                .thenReturn(null);

        // when
        HeritagePlaceResponse response =
                heritageService.getHeritagePlace(placeId);

        // then
        assertThat(response.heritage()).isTrue();
        assertThat(response.placeId()).isEqualTo(placeId);
        assertThat(response.contentId()).isEqualTo(contentId);
        assertThat(response.title()).isEqualTo("공산성");
        assertThat(response.address()).isEqualTo("충남 공주시");
    }

    @Test
    void TourAPI_조회_실패시_DB가_HERITAGE가_아니면_일반_장소로_fallback한다() {
        // given
        Long placeId = 1L;
        String contentId = "100";

        givenPlace(placeId, contentId);

        when(place.getCategoryDetail())
                .thenReturn(PlaceCategoryDetail.WALK);

        when(tourApiPlaceClient.getPlaceDetail(contentId))
                .thenReturn(null);

        // when
        HeritagePlaceResponse response =
                heritageService.getHeritagePlace(placeId);

        // then
        assertThat(response.heritage()).isFalse();
    }

    @Test
    void 존재하지_않는_장소는_PLACE_NOT_FOUND_예외를_반환한다() {
        // given
        Long placeId = 1L;

        when(placeRepository.findById(placeId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                heritageService.getHeritagePlace(placeId)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND)
        );
    }

    @Test
    void TourAPI_contentId가_null이면_NOT_TOUR_API_PLACE_예외를_반환한다() {
        // given
        Long placeId = 1L;

        when(placeRepository.findById(placeId))
                .thenReturn(Optional.of(place));
        when(place.getTourContentId())
                .thenReturn(null);

        // when & then
        assertThatThrownBy(() ->
                heritageService.getHeritagePlace(placeId)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(HeritageErrorCode.NOT_TOUR_API_PLACE)
        );
    }

    @Test
    void TourAPI_contentId가_빈값이면_NOT_TOUR_API_PLACE_예외를_반환한다() {
        // given
        Long placeId = 1L;

        when(placeRepository.findById(placeId))
                .thenReturn(Optional.of(place));
        when(place.getTourContentId())
                .thenReturn(" ");

        // when & then
        assertThatThrownBy(() ->
                heritageService.getHeritagePlace(placeId)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(HeritageErrorCode.NOT_TOUR_API_PLACE)
        );
    }

    private void givenPlace(
            Long placeId,
            String contentId
    ) {
        when(placeRepository.findById(placeId))
                .thenReturn(Optional.of(place));

        when(place.getId()).thenReturn(placeId);
        when(place.getTourContentId()).thenReturn(contentId);
        when(place.getRegion()).thenReturn(region);

        when(region.getProvinceName()).thenReturn("충청남도");
        when(region.getCityCountyName()).thenReturn("공주시");
    }

    private TourApiPlaceItem createTourApiItem(
            String contentId,
            String lclsSystm1
    ) {
        return new TourApiPlaceItem(
                contentId,
                "공산성",
                "충남 공주시",
                null,
                null,
                null,
                null,
                null,
                lclsSystm1,
                null,
                null,
                null
        );
    }
}