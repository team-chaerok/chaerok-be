package com.chaerok.backend.place.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiPlaceClient {

    private static final String AREA_BASED_LIST_PATH = "/areaBasedList2";
    private static final String SEARCH_KEYWORD_PATH = "/searchKeyword2";
    private static final String DETAIL_COMMON_PATH = "/detailCommon2";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration AREA_TOTAL_TIMEOUT = Duration.ofSeconds(10);
    private static final int AREA_PAGE_SIZE = 50;

    private final WebClient tourApiWebClient;

    @Value("${external.tour-api.key}")
    private String serviceKey;

    public List<TourApiPlaceItem> getPlacesByRegion(
            String lDongRegnCd,
            String lDongSignguCd
    ) {
        List<TourApiPlaceItem> allItems = new ArrayList<>();
        int pageNo = 1;

        long deadlineNanos =
                System.nanoTime() + AREA_TOTAL_TIMEOUT.toNanos();

        try {
            while (true) {
                if (System.nanoTime() >= deadlineNanos) {
                    log.warn(
                            "TourAPI areaBasedList2 total timeout. " +
                                    "pageNo={}, collectedItems={}, " +
                                    "lDongRegnCd={}, lDongSignguCd={}",
                            pageNo,
                            allItems.size(),
                            lDongRegnCd,
                            lDongSignguCd
                    );
                    break;
                }

                TourApiPlaceResponse response =
                        requestPlacesByRegionPage(
                                lDongRegnCd,
                                lDongSignguCd,
                                pageNo
                        );

                if (response == null) {
                    break;
                }

                if (!response.isSuccess()) {
                    log.warn(
                            "TourAPI areaBasedList2 failed. " +
                                    "pageNo={}, lDongRegnCd={}, lDongSignguCd={}, " +
                                    "resultCode={}, resultMsg={}",
                            pageNo,
                            lDongRegnCd,
                            lDongSignguCd,
                            response.getResultCode(),
                            response.getResultMsg()
                    );
                    break;
                }

                List<TourApiPlaceItem> items = response.getItems();

                if (items.isEmpty()) {
                    break;
                }

                allItems.addAll(items);

                int totalCount = response.getTotalCount();

                if (totalCount <= pageNo * AREA_PAGE_SIZE) {
                    break;
                }

                pageNo++;
            }

            return allItems;

        } catch (WebClientResponseException e) {
            log.warn(
                    "TourAPI areaBasedList2 response error. status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            return allItems;

        } catch (WebClientRequestException e) {
            log.warn(
                    "TourAPI areaBasedList2 request error. message={}",
                    e.getMessage()
            );
            return allItems;

        } catch (RuntimeException e) {
            log.error(
                    "TourAPI areaBasedList2 unexpected error. message={}",
                    e.getMessage(),
                    e
            );
            return allItems;
        }
    }

    public Map<String, TourApiPlaceItem> getPlacesByContentIds(
            String lDongRegnCd,
            String lDongSignguCd,
            Set<String> targetContentIds
    ) {
        if (targetContentIds == null || targetContentIds.isEmpty()) {
            return Map.of();
        }

        Map<String, TourApiPlaceItem> matchedPlaces = new HashMap<>();
        Set<String> remainingContentIds = new HashSet<>(targetContentIds);

        int pageNo = 1;

        try {
            while (!remainingContentIds.isEmpty()) {
                TourApiPlaceResponse response = requestPlacesByRegionPage(
                        lDongRegnCd,
                        lDongSignguCd,
                        pageNo
                );

                if (response == null) {
                    break;
                }

                if (!response.isSuccess()) {
                    log.warn(
                            "TourAPI areaBasedList2 failed while matching. pageNo={}, resultCode={}, resultMsg={}",
                            pageNo,
                            response.getResultCode(),
                            response.getResultMsg()
                    );
                    break;
                }

                List<TourApiPlaceItem> items = response.getItems();

                for (TourApiPlaceItem item : items) {
                    if (item.contentId() == null) {
                        continue;
                    }

                    if (remainingContentIds.remove(item.contentId())) {
                        matchedPlaces.put(item.contentId(), item);
                    }
                }

                if (remainingContentIds.isEmpty()) {
                    break;
                }

                int totalCount = response.getTotalCount();

                if (items.isEmpty() || totalCount <= pageNo * AREA_PAGE_SIZE) {
                    break;
                }

                pageNo++;
            }

            return matchedPlaces;

        } catch (WebClientResponseException e) {
            log.warn(
                    "TourAPI areaBasedList2 matching response error. status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            return matchedPlaces;

        } catch (WebClientRequestException e) {
            log.warn(
                    "TourAPI areaBasedList2 matching request error. message={}",
                    e.getMessage()
            );
            return matchedPlaces;

        } catch (RuntimeException e) {
            log.error(
                    "TourAPI areaBasedList2 matching unexpected error. message={}",
                    e.getMessage(),
                    e
            );
            return matchedPlaces;
        }
    }

    private TourApiPlaceResponse requestPlacesByRegionPage(
            String lDongRegnCd,
            String lDongSignguCd,
            int pageNo
    ) {
        return tourApiWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(AREA_BASED_LIST_PATH)
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "Chaerok")
                        .queryParam("_type", "json")
                        .queryParam("numOfRows", AREA_PAGE_SIZE)
                        .queryParam("pageNo", pageNo)
                        .queryParam("arrange", "A")
                        .queryParam("lDongRegnCd", lDongRegnCd)
                        .queryParam("lDongSignguCd", lDongSignguCd)
                        .build())
                .retrieve()
                .bodyToMono(TourApiPlaceResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .block();
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
            TourApiPlaceResponse response = tourApiWebClient
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
                log.warn(
                        "TourAPI searchKeyword2 failed. keyword={}, lDongRegnCd={}, lDongSignguCd={}, resultCode={}, resultMsg={}",
                        keyword,
                        lDongRegnCd,
                        lDongSignguCd,
                        response.getResultCode(),
                        response.getResultMsg()
                );
                return List.of();
            }

            return response.getItems();

        } catch (WebClientResponseException e) {
            log.warn(
                    "TourAPI searchKeyword2 response error. keyword={}, status={}, body={}",
                    keyword,
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            return List.of();

        } catch (WebClientRequestException e) {
            log.warn(
                    "TourAPI searchKeyword2 request error. keyword={}, message={}",
                    keyword,
                    e.getMessage()
            );
            return List.of();

        } catch (RuntimeException e) {
            log.error(
                    "TourAPI searchKeyword2 unexpected error. keyword={}, message={}",
                    keyword,
                    e.getMessage(),
                    e
            );
            return List.of();
        }
    }

    public TourApiPlaceItem getPlaceDetail(String contentId) {
        if (contentId == null || contentId.isBlank()) {
            return null;
        }

        try {
            TourApiPlaceResponse response = tourApiWebClient
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
                        contentId,
                        response.getResultCode(),
                        response.getResultMsg()
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