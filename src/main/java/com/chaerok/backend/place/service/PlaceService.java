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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final RegionRepository regionRepository;
    private final TourApiPlaceClient tourApiPlaceClient;

    public List<PlaceListResponse> getPlacesByRegion(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new IllegalArgumentException("지역을 찾을 수 없습니다."));

        List<TourApiPlaceItem> tourApiItems = tourApiPlaceClient.getPlacesByRegion(
                region.getLdongRegnCd(),
                region.getLdongSignguCd()
        );

        tourApiItems.forEach(item -> saveOrUpdatePlace(region, item));

        return placeRepository.findByRegionId(regionId).stream()
                .map(PlaceListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaceDetailResponse getPlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다."));

        return PlaceDetailResponse.from(place);
    }

    private void saveOrUpdatePlace(Region region, TourApiPlaceItem item) {
        if (item.contentId() == null || item.title() == null) {
            return;
        }

        BigDecimal latitude = toBigDecimal(item.latitude());
        BigDecimal longitude = toBigDecimal(item.longitude());
        PlaceCategoryGroup categoryGroup = PlaceCategoryMapper.toGroup(item.lclsSystm3());
        PlaceCategoryDetail categoryDetail = PlaceCategoryMapper.toDetail(item.lclsSystm3());

        placeRepository.findByTourContentId(item.contentId())
                .ifPresentOrElse(
                        place -> place.updateFromTourApi(
                                item.title(),
                                item.address(),
                                latitude,
                                longitude,
                                item.firstImageUrl(),
                                item.lDongRegnCd(),
                                item.lDongSignguCd(),
                                item.lclsSystm1(),
                                item.lclsSystm2(),
                                item.lclsSystm3(),
                                categoryGroup,
                                categoryDetail
                        ),
                        () -> placeRepository.save(Place.create(
                                region,
                                item.contentId(),
                                item.title(),
                                item.address(),
                                latitude,
                                longitude,
                                item.firstImageUrl(),
                                item.lDongRegnCd(),
                                item.lDongSignguCd(),
                                item.lclsSystm1(),
                                item.lclsSystm2(),
                                item.lclsSystm3(),
                                categoryGroup,
                                categoryDetail,
                                false,
                                PlaceSource.TOUR_API
                        ))
                );
    }

    private BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return new BigDecimal(value);
    }
}