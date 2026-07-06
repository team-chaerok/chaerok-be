package com.chaerok.backend.place.service;

import com.chaerok.backend.place.dto.PlaceDetailResponse;
import com.chaerok.backend.place.dto.PlaceListResponse;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final RegionRepository regionRepository;
    private final TourApiPlaceClient tourApiPlaceClient;

    public List<PlaceListResponse> getPlacesByRegion(Long regionId) {
        if (!regionRepository.existsById(regionId)) {
            throw new IllegalArgumentException("지역을 찾을 수 없습니다.");
        }

        List<Place> representativePlaces =
                placeRepository.findByRegionIdAndRepresentativeTrue(regionId);

        return representativePlaces.stream()
                .map(this::toPlaceListResponseWithTourApi)
                .toList();
    }

    public List<PlaceListResponse> getExternalPlaces(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new IllegalArgumentException("지역을 찾을 수 없습니다."));

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
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다."));

        TourApiPlaceItem tourApiItem = tourApiPlaceClient.getPlaceDetail(place.getTourContentId());

        if (tourApiItem == null) {
            return PlaceDetailResponse.from(place);
        }

        return PlaceDetailResponse.from(place, tourApiItem);
    }
}