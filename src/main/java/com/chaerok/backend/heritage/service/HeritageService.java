package com.chaerok.backend.heritage.service;

import com.chaerok.backend.heritage.dto.HeritagePlaceResponse;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
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

        String regionName = place.getRegion().getProvinceName()
                + " "
                + place.getRegion().getCityCountyName();

        TourApiPlaceItem item = tourApiPlaceClient.getPlaceDetail(contentId);

        if (item == null) {
            return createFallbackResponse(place, regionName);
        }

        boolean heritage = isHeritage(item);

        return HeritagePlaceResponse.of(
                place.getId(),
                regionName,
                heritage,
                item
        );
    }

    private HeritagePlaceResponse createFallbackResponse(
            Place place,
            String regionName
    ) {
        boolean heritage =
                place.getCategoryDetail() == PlaceCategoryDetail.HERITAGE;

        return HeritagePlaceResponse.fromPlace(
                place,
                regionName,
                heritage
        );
    }

    private boolean isHeritage(TourApiPlaceItem item) {
        boolean heritageCategory = "HS".equals(item.lclsSystm1());
        boolean heritageException = GUNGNAMJI_CONTENT_ID.equals(item.contentId());

        return heritageCategory || heritageException;
    }
}