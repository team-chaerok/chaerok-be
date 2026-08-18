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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final RegionRepository regionRepository;
    private final TourApiPlaceClient tourApiPlaceClient;

    public List<PlaceListResponse> getPlacesByRegion(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(RegionNotFoundException::new);

        List<Place> representativePlaces =
                placeRepository.findByRegionIdAndRepresentativeTrue(regionId);

        Set<String> targetContentIds = representativePlaces.stream()
                .map(Place::getTourContentId)
                .filter(contentId -> contentId != null && !contentId.isBlank())
                .collect(Collectors.toSet());

        Map<String, TourApiPlaceItem> tourApiPlaces =
                tourApiPlaceClient.getPlacesByContentIds(
                        region.getLdongRegnCd(),
                        region.getLdongSignguCd(),
                        targetContentIds
                );

        return representativePlaces.stream()
                .map(place -> toPlaceListResponse(
                        place,
                        tourApiPlaces.get(place.getTourContentId())
                ))
                .toList();
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

    private PlaceListResponse toPlaceListResponse(
            Place place,
            TourApiPlaceItem tourApiItem
    ) {
        if (tourApiItem == null) {
            return PlaceListResponse.from(place);
        }

        return PlaceListResponse.from(place, tourApiItem);
    }

    public PlaceDetailResponse getPlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(PlaceNotFoundException::new);

        TourApiPlaceItem tourApiItem =
                tourApiPlaceClient.getPlaceDetail(place.getTourContentId());

        if (tourApiItem == null) {
            return PlaceDetailResponse.from(place);
        }

        return PlaceDetailResponse.from(place, tourApiItem);
    }
}