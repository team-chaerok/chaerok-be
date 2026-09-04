package com.chaerok.backend.place.service;

import com.chaerok.backend.place.entity.Place;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceSyncServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private Region region;

    @Mock
    private Place place;

    private PlaceSyncService service;

    @BeforeEach
    void setUp() {
        service = new PlaceSyncService(placeRepository);
    }

    @Test
    @DisplayName("신규 TourAPI 장소를 Place로 저장한다")
    void savesNewPlace() {
        TourApiPlaceItem item = createItem(
                "100",
                "공산성",
                "36.4651",
                "127.1190"
        );

        when(placeRepository.findByTourContentId("100"))
                .thenReturn(Optional.empty());

        service.syncPlaces(region, List.of(item));

        ArgumentCaptor<Place> captor =
                ArgumentCaptor.forClass(Place.class);

        verify(placeRepository).save(captor.capture());

        Place saved = captor.getValue();

        assertThat(saved.getRegion()).isSameAs(region);
        assertThat(saved.getTourContentId()).isEqualTo("100");
        assertThat(saved.getTitle()).isEqualTo("공산성");
        assertThat(saved.getLatitude())
                .isEqualByComparingTo(new BigDecimal("36.4651"));
        assertThat(saved.getLongitude())
                .isEqualByComparingTo(new BigDecimal("127.1190"));
    }

    @Test
    @DisplayName("기존 TourAPI 장소는 새로 저장하지 않고 정보를 갱신한다")
    void updatesExistingPlace() {
        TourApiPlaceItem item = createItem(
                "100",
                "공산성",
                "36.4651",
                "127.1190"
        );

        when(placeRepository.findByTourContentId("100"))
                .thenReturn(Optional.of(place));

        service.syncPlaces(region, List.of(item));

        verify(place).updateFromTourApi(
                item.title(),
                item.address(),
                new BigDecimal("36.4651"),
                new BigDecimal("127.1190"),
                item.firstImageUrl(),
                item.lDongRegnCd(),
                item.lDongSignguCd(),
                item.lclsSystm1(),
                item.lclsSystm2(),
                item.lclsSystm3(),
                PlaceCategoryMapper.toGroup(
                        item.lclsSystm1(),
                        item.lclsSystm3()
                ),
                PlaceCategoryMapper.toDetail(
                        item.lclsSystm1(),
                        item.lclsSystm3()
                )
        );

        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("contentId가 없으면 장소를 저장하지 않는다")
    void ignoresItemWithoutContentId() {
        TourApiPlaceItem item = createItem(
                null,
                "공산성",
                "36.4651",
                "127.1190"
        );

        service.syncPlaces(region, List.of(item));

        verify(placeRepository, never())
                .findByTourContentId(any());
        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("contentId가 빈 문자열이면 장소를 저장하지 않는다")
    void ignoresItemWithBlankContentId() {
        TourApiPlaceItem item = createItem(
                " ",
                "공산성",
                "36.4651",
                "127.1190"
        );

        service.syncPlaces(region, List.of(item));

        verify(placeRepository, never())
                .findByTourContentId(any());
        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("장소명이 없으면 장소를 저장하지 않는다")
    void ignoresItemWithoutTitle() {
        TourApiPlaceItem item = createItem(
                "100",
                null,
                "36.4651",
                "127.1190"
        );

        service.syncPlaces(region, List.of(item));

        verify(placeRepository, never())
                .findByTourContentId(any());
        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("장소명이 빈 문자열이면 장소를 저장하지 않는다")
    void ignoresItemWithBlankTitle() {
        TourApiPlaceItem item = createItem(
                "100",
                " ",
                "36.4651",
                "127.1190"
        );

        service.syncPlaces(region, List.of(item));

        verify(placeRepository, never())
                .findByTourContentId(any());
        verify(placeRepository, never())
                .save(any(Place.class));
    }

    @Test
    @DisplayName("위경도가 없으면 null로 저장한다")
    void savesNullCoordinatesWhenCoordinatesAreMissing() {
        TourApiPlaceItem item = createItem(
                "100",
                "공산성",
                null,
                ""
        );

        when(placeRepository.findByTourContentId("100"))
                .thenReturn(Optional.empty());

        service.syncPlaces(region, List.of(item));

        ArgumentCaptor<Place> captor =
                ArgumentCaptor.forClass(Place.class);

        verify(placeRepository).save(captor.capture());

        Place saved = captor.getValue();

        assertThat(saved.getLatitude()).isNull();
        assertThat(saved.getLongitude()).isNull();
    }

    private TourApiPlaceItem createItem(
            String contentId,
            String title,
            String latitude,
            String longitude
    ) {
        return new TourApiPlaceItem(
                contentId,
                title,
                "충남 공주시 웅진로 280",
                latitude,
                longitude,
                "https://example.com/image.jpg",
                "34",
                "20",
                "HS",
                "HS01",
                "HS0101",
                "공산성 소개"
        );
    }
}