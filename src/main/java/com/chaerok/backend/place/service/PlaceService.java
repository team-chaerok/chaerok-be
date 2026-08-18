package com.chaerok.backend.place.service;

import com.chaerok.backend.global.exception.PlaceNotFoundException;
import com.chaerok.backend.global.exception.RegionNotFoundException;
import com.chaerok.backend.place.dto.PlaceDetailResponse;
import com.chaerok.backend.place.dto.PlaceListResponse;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final RegionRepository regionRepository;
    private final TourApiPlaceClient tourApiPlaceClient;

    public List<PlaceListResponse> getPlacesByRegion(Long regionId) {
        long totalStart = System.currentTimeMillis();

        try {
            long regionCheckStart = System.currentTimeMillis();

            if (!regionRepository.existsById(regionId)) {
                throw new RegionNotFoundException();
            }

            log.info(
                    "Place list region check elapsed={}ms, regionId={}",
                    System.currentTimeMillis() - regionCheckStart,
                    regionId
            );

            long placeQueryStart = System.currentTimeMillis();

            List<Place> representativePlaces =
                    placeRepository.findByRegionIdAndRepresentativeTrue(regionId);

            log.info(
                    "Place list DB query elapsed={}ms, representativeCount={}, regionId={}",
                    System.currentTimeMillis() - placeQueryStart,
                    representativePlaces.size(),
                    regionId
            );

            long tourApiTargetCount = representativePlaces.stream()
                    .filter(place -> place.getTourContentId() != null)
                    .filter(place -> !place.getTourContentId().isBlank())
                    .count();

            log.info(
                    "Place list TourAPI target count={}, totalCount={}, regionId={}",
                    tourApiTargetCount,
                    representativePlaces.size(),
                    regionId
            );

            long tourApiStart = System.currentTimeMillis();

            List<PlaceListResponse> result = representativePlaces.stream()
                    .map(this::toPlaceListResponseWithTourApi)
                    .toList();

            log.info(
                    "Place list TourAPI total elapsed={}ms, regionId={}",
                    System.currentTimeMillis() - tourApiStart,
                    regionId
            );

            return result;

        } finally {
            log.info(
                    "Place list total elapsed={}ms, regionId={}",
                    System.currentTimeMillis() - totalStart,
                    regionId
            );
        }
    }

    public List<PlaceListResponse> getExternalPlaces(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(RegionNotFoundException::new);

        return tourApiPlaceClient.getPlacesByRegion(
                        region.getLdongRegnCd(),
                        region.getLdongSignguCd()
                ).stream()
                .map(PlaceListResponse::fromTourApi)
                .toList();
    }

    private PlaceListResponse toPlaceListResponseWithTourApi(Place place) {
        TourApiPlaceItem tourApiItem = tourApiPlaceClient.getPlaceDetail(place.getTourContentId());

        if (tourApiItem == null) {
            return PlaceListResponse.from(place);
        }

        return PlaceListResponse.from(place, tourApiItem);
    }

    public PlaceDetailResponse getPlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(PlaceNotFoundException::new);

        TourApiPlaceItem tourApiItem = tourApiPlaceClient.getPlaceDetail(place.getTourContentId());

        if (tourApiItem == null) {
            return PlaceDetailResponse.from(place);
        }

        return PlaceDetailResponse.from(place, tourApiItem);
    }
}