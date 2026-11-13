package com.chaerok.backend.heritage.service;

import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.heritage.dto.HeritagePlaceResponse;
import com.chaerok.backend.heritage.exception.HeritageErrorCode;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.exception.PlaceErrorCode;
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

    private final PlaceRepository placeRepository;
    private final TourApiPlaceClient tourApiPlaceClient;

    public HeritagePlaceResponse getHeritagePlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() ->
                        new BusinessException(
                                PlaceErrorCode.PLACE_NOT_FOUND
                        )
                );

        String contentId = place.getTourContentId();

        if (contentId == null || contentId.isBlank()) {
            throw new BusinessException(
                    HeritageErrorCode.NOT_TOUR_API_PLACE
            );
        }

        String regionName = place.getRegion().getProvinceName()
                + " "
                + place.getRegion().getCityCountyName();

        TourApiPlaceItem item =
                tourApiPlaceClient.getPlaceDetail(contentId);

        if (item == null) {
            return createFallbackResponse(
                    place,
                    regionName
            );
        }

        boolean heritage = HeritagePolicy.isHeritage(item.lclsSystm1(), item.contentId());

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
        boolean heritage = HeritagePolicy.isHeritage(place);

        return HeritagePlaceResponse.fromPlace(
                place,
                regionName,
                heritage
        );
    }

}
