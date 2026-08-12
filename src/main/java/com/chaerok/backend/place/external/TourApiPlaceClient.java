package com.chaerok.backend.place.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiPlaceClient {

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2";
    private static final String AREA_BASED_LIST_PATH = "/areaBasedList2";
    private static final String SEARCH_KEYWORD_PATH = "/searchKeyword2";
    private static final String DETAIL_COMMON_PATH = "/detailCommon2";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient.Builder webClientBuilder;

    @Value("${external.tour-api.key}")
    private String serviceKey;

    public List<TourApiPlaceItem> getPlacesByRegion(
            String lDongRegnCd,
            String lDongSignguCd
    ) {
        long start = System.currentTimeMillis();

        try {
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
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                return List.of();
            }

            if (!response.isSuccess()) {
                log.warn("TourAPI areaBasedList2 failed. lDongRegnCd={}, lDongSignguCd={}",
                        lDongRegnCd, lDongSignguCd);
                return List.of();
            }

            return response.getItems();

        } catch (WebClientResponseException e) {
            log.warn("TourAPI areaBasedList2 response error. status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();

        } catch (WebClientRequestException e) {
            log.warn("TourAPI areaBasedList2 request error. message={}", e.getMessage());
            return List.of();

        } catch (RuntimeException e) {
            log.error("TourAPI areaBasedList2 unexpected error. message={}", e.getMessage(), e);
            return List.of();

        } finally {
            log.info(
                    "TourAPI areaBasedList2 elapsed={}ms, lDongRegnCd={}, lDongSignguCd={}",
                    System.currentTimeMillis() - start,
                    lDongRegnCd,
                    lDongSignguCd
            );
        }
    }

    public List<TourApiPlaceItem> searchPlacesByKeyword(
            String keyword,
            String lDongRegnCd,
            String lDongSignguCd
    ) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        try {
            TourApiPlaceResponse response = webClientBuilder
                    .baseUrl(BASE_URL)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SEARCH_KEYWORD_PATH)
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("MobileOS", "ETC")
                            .queryParam("MobileApp", "Chaerok")
                            .queryParam("_type", "json")
                            .queryParam("numOfRows", 20)
                            .queryParam("pageNo", 1)
                            .queryParam("arrange", "A")
                            .queryParam("keyword", keyword)
                            .queryParam("lDongRegnCd", lDongRegnCd)
                            .queryParam("lDongSignguCd", lDongSignguCd)
                            .build())
                    .retrieve()
                    .bodyToMono(TourApiPlaceResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                return List.of();
            }

            if (!response.isSuccess()) {
                log.warn("TourAPI searchKeyword2 failed. keyword={}, lDongRegnCd={}, lDongSignguCd={}",
                        keyword, lDongRegnCd, lDongSignguCd);
                return List.of();
            }

            return response.getItems();
        } catch (WebClientResponseException e) {
            log.warn("TourAPI searchKeyword2 response error. keyword={}, status={}, body={}",
                    keyword, e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (WebClientRequestException e) {
            log.warn("TourAPI searchKeyword2 request error. keyword={}, message={}",
                    keyword, e.getMessage());
            return List.of();
        } catch (RuntimeException e) {
            log.error("TourAPI searchKeyword2 unexpected error. keyword={}, message={}",
                    keyword, e.getMessage(), e);
            return List.of();
        }
    }

    public TourApiPlaceItem getPlaceDetail(String contentId) {
        if (contentId == null || contentId.isBlank()) {
            return null;
        }

        try {
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
                            .queryParam("numOfRows", 10)
                            .queryParam("pageNo", 1)
                            .build())
                    .retrieve()
                    .bodyToMono(TourApiPlaceResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                return null;
            }

            if (!response.isSuccess()) {
                log.warn(
                        "TourAPI detailCommon2 failed. contentId={}, resultCode={}, resultMsg={}",
                        contentId
                );
                return null;
            }

            return response.getItems().stream()
                    .findFirst()
                    .orElse(null);

        } catch (WebClientResponseException e) {
            log.warn(
                    "TourAPI detailCommon2 response error. contentId={}, status={}, body={}",
                    contentId,
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            return null;

        } catch (WebClientRequestException e) {
            log.warn(
                    "TourAPI detailCommon2 request error. contentId={}, message={}",
                    contentId,
                    e.getMessage()
            );
            return null;

        } catch (RuntimeException e) {
            log.error(
                    "TourAPI detailCommon2 unexpected error. contentId={}, message={}",
                    contentId,
                    e.getMessage(),
                    e
            );
            return null;
        }
    }
}