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
    private final PlaceSyncService placeSyncService;

    public List<PlaceListResponse> getPlacesByRegion(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new IllegalArgumentException("지역을 찾을 수 없습니다."));

        List<TourApiPlaceItem> tourApiItems = tourApiPlaceClient.getPlacesByRegion(
                region.getLdongRegnCd(),
                region.getLdongSignguCd()
        );

        placeSyncService.syncPlaces(region, tourApiItems);

        return placeRepository.findByRegionId(regionId).stream()
                .map(PlaceListResponse::from)
                .toList();
    }

    public PlaceDetailResponse getPlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다."));

        return PlaceDetailResponse.from(place);
    }
}