package com.chaerok.backend.place.service;

import com.chaerok.backend.global.exception.RegionNotFoundException;
import com.chaerok.backend.place.dto.PlaceSearchResponse;
import com.chaerok.backend.place.external.KakaoLocalClient;
import com.chaerok.backend.place.external.KakaoPlaceItem;
import com.chaerok.backend.place.external.TourApiPlaceClient;
import com.chaerok.backend.place.external.TourApiPlaceItem;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlaceSearchService {

    private static final int TOUR_API_MIN_RESULT_COUNT = 5;

    private static final String GONGJU_SIGNGU_CODE = "150";
    private static final String BUYEO_SIGNGU_CODE = "760";
    private static final String SEOSAN_SIGNGU_CODE = "210";
    private static final String YESAN_SIGNGU_CODE = "810";

    private final RegionRepository regionRepository;
    private final TourApiPlaceClient tourApiPlaceClient;
    private final KakaoLocalClient kakaoLocalClient;

    public List<PlaceSearchResponse> searchPlaces(Long regionId, String keyword) {
        Region region = findRegion(regionId);

        List<PlaceSearchResponse> tourApiResults = searchTourApi(region, keyword);

        if (tourApiResults.size() >= TOUR_API_MIN_RESULT_COUNT) {
            return tourApiResults;
        }

        List<PlaceSearchResponse> kakaoResults = searchKakao(region, keyword);

        return mergeResults(tourApiResults, kakaoResults);
    }

    private Region findRegion(Long regionId) {
        return regionRepository.findById(regionId)
                .orElseThrow(RegionNotFoundException::new);
    }

    private List<PlaceSearchResponse> searchTourApi(Region region, String keyword) {
        List<TourApiPlaceItem> items = tourApiPlaceClient.searchPlacesByKeyword(
                keyword,
                region.getLdongRegnCd(),
                region.getLdongSignguCd()
        );

        return items.stream()
                .map(PlaceSearchResponse::fromTourApi)
                .toList();
    }

    private List<PlaceSearchResponse> searchKakao(Region region, String keyword) {
        RegionCenter center = getRegionCenter(region);

        List<KakaoPlaceItem> items = kakaoLocalClient.searchPlacesByKeyword(
                keyword,
                center.longitude(),
                center.latitude()
        );

        return items.stream()
                .map(PlaceSearchResponse::fromKakao)
                .toList();
    }

    private List<PlaceSearchResponse> mergeResults(
            List<PlaceSearchResponse> tourApiResults,
            List<PlaceSearchResponse> kakaoResults
    ) {
        Map<String, PlaceSearchResponse> results = new LinkedHashMap<>();

        for (PlaceSearchResponse response : tourApiResults) {
            results.put(createDeduplicationKey(response), response);
        }

        for (PlaceSearchResponse response : kakaoResults) {
            results.putIfAbsent(createDeduplicationKey(response), response);
        }

        return results.values().stream()
                .toList();
    }

    private String createDeduplicationKey(PlaceSearchResponse response) {
        String title = normalize(response.title());
        String address = normalize(response.address());

        return title + "|" + address;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\s+", "")
                .toLowerCase();
    }

    private RegionCenter getRegionCenter(Region region) {
        return switch (region.getLdongSignguCd()) {
            case GONGJU_SIGNGU_CODE -> new RegionCenter(
                    new BigDecimal("127.1190"),
                    new BigDecimal("36.4465")
            );
            case BUYEO_SIGNGU_CODE -> new RegionCenter(
                    new BigDecimal("126.9119"),
                    new BigDecimal("36.2757")
            );
            case SEOSAN_SIGNGU_CODE -> new RegionCenter(
                    new BigDecimal("126.4503"),
                    new BigDecimal("36.7845")
            );
            case YESAN_SIGNGU_CODE -> new RegionCenter(
                    new BigDecimal("126.8447"),
                    new BigDecimal("36.6829")
            );
            default -> new RegionCenter(
                    new BigDecimal("126.8000"),
                    new BigDecimal("36.5000")
            );
        };
    }

    private record RegionCenter(
            BigDecimal longitude,
            BigDecimal latitude
    ) {
    }
}