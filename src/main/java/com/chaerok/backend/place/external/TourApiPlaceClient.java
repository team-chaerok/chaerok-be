package com.chaerok.backend.place.external;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TourApiPlaceClient {

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2";
    private static final String AREA_BASED_LIST_PATH = "/areaBasedList2";
    private static final String DETAIL_COMMON_PATH = "/detailCommon2";

    private final WebClient.Builder webClientBuilder;

    @Value("${external.tour-api.key}")
    private String serviceKey;

    public List<TourApiPlaceItem> getPlacesByRegion(
            String lDongRegnCd,
            String lDongSignguCd
    ) {
        TourApiPlaceResponse response = webClientBuilder
                .baseUrl(BASE_URL)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(AREA_BASED_LIST_PATH)
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "Chaerok")
                        .queryParam("_type", "json")
                        .queryParam("numOfRows", 50)
                        .queryParam("pageNo", 1)
                        .queryParam("arrange", "A")
                        .queryParam("lDongRegnCd", lDongRegnCd)
                        .queryParam("lDongSignguCd", lDongSignguCd)
                        .build())
                .retrieve()
                .bodyToMono(TourApiPlaceResponse.class)
                .block();

        if (response == null) {
            return List.of();
        }

        return response.getItems();
    }

    public TourApiPlaceItem getPlaceDetail(String contentId) {
        TourApiPlaceResponse response = webClientBuilder
                .baseUrl(BASE_URL)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(DETAIL_COMMON_PATH)
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "Chaerok")
                        .queryParam("_type", "json")
                        .queryParam("contentId", contentId)
                        .queryParam("defaultYN", "Y")
                        .queryParam("firstImageYN", "Y")
                        .queryParam("addrinfoYN", "Y")
                        .queryParam("mapinfoYN", "Y")
                        .queryParam("overviewYN", "Y")
                        .build())
                .retrieve()
                .bodyToMono(TourApiPlaceResponse.class)
                .block();

        if (response == null) {
            return null;
        }

        return response.getItems().stream()
                .findFirst()
                .orElse(null);
    }
}