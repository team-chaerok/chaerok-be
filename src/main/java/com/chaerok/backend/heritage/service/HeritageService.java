package com.chaerok.backend.heritage.service;

import com.chaerok.backend.heritage.dto.HeritagePlaceResponse;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HeritageService {

    private static final String GUNGNAMJI_CONTENT_ID = "125984";

    private final PlaceRepository placeRepository;
    private final TourApiPlaceClient tourApiPlaceClient;

    public HeritagePlaceResponse getHeritagePlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소입니다."));

        String contentId = place.getTourContentId();

        if (contentId == null || contentId.isBlank()) {
            throw new IllegalArgumentException("TourAPI 장소가 아닙니다.");
        }

        TourApiPlaceItem item = tourApiPlaceClient.getPlaceDetail(contentId);

        if (item == null) {
            throw new IllegalStateException("TourAPI 장소 정보를 조회할 수 없습니다.");
        }

        boolean heritage = isHeritage(item);

        String regionName = place.getRegion().getProvinceName()
                + " "
                + place.getRegion().getCityCountyName();

        return HeritagePlaceResponse.of(
                place.getId(),
                regionName,
                heritage,
                item
        );
    }

    private boolean isHeritage(TourApiPlaceItem item) {
        boolean heritageCategory = "HS".equals(item.lclsSystm1());
        boolean heritageException = GUNGNAMJI_CONTENT_ID.equals(item.contentId());

        return heritageCategory || heritageException;
    }
}