package com.chaerok.backend.place.service;

import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.entity.PlaceSource;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.entity.Region;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaceSyncService {

    private final PlaceRepository placeRepository;

    public void syncPlaces(Region region, List<TourApiPlaceItem> items) {
        items.forEach(item -> saveOrUpdatePlace(region, item));
    }

    private void saveOrUpdatePlace(Region region, TourApiPlaceItem item) {
        if (item.contentId() == null || item.contentId().isBlank() || item.title() == null || item.title().isBlank()) {
            return;
        }

        BigDecimal latitude = toBigDecimal(item.latitude());
        BigDecimal longitude = toBigDecimal(item.longitude());

        PlaceCategoryGroup categoryGroup = PlaceCategoryMapper.toGroup(
                item.lclsSystm1(),
                item.lclsSystm3()
        );

        PlaceCategoryDetail categoryDetail = PlaceCategoryMapper.toDetail(
                item.lclsSystm1(),
                item.lclsSystm3()
        );

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
