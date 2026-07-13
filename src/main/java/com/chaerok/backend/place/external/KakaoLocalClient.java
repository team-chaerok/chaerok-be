package com.chaerok.backend.place.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoLocalClient {

    private static final String BASE_URL = "https://dapi.kakao.com";
    private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";
    private static final String CATEGORY_SEARCH_PATH = "/v2/local/search/category.json";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private static final int DEFAULT_RADIUS = 10000;
    private static final int DEFAULT_SIZE = 15;

    private final WebClient.Builder webClientBuilder;

    @Value("${external.kakao.api-key}")
    private String restApiKey;

    public List<KakaoPlaceItem> searchPlacesByKeyword(
            String keyword,
            BigDecimal longitude,
            BigDecimal latitude
    ) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        try {
            KakaoPlaceResponse response = webClientBuilder
                    .baseUrl(BASE_URL)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(KEYWORD_SEARCH_PATH)
                            .queryParam("query", keyword)
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("radius", DEFAULT_RADIUS)
                            .queryParam("size", DEFAULT_SIZE)
                            .build())
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .bodyToMono(KakaoPlaceResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                return List.of();
            }

            return response.getItems();
        } catch (WebClientResponseException e) {
            log.warn("Kakao Local keyword search response error. keyword={}, status={}, body={}",
                    keyword, e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (WebClientRequestException e) {
            log.warn("Kakao Local keyword search request error. keyword={}, message={}",
                    keyword, e.getMessage());
            return List.of();
        } catch (RuntimeException e) {
            log.error("Kakao Local keyword search unexpected error. keyword={}, message={}",
                    keyword, e.getMessage(), e);
            return List.of();
        }
    }

    public List<KakaoPlaceItem> searchPlacesByCategory(
            String categoryGroupCode,
            BigDecimal longitude,
            BigDecimal latitude,
            int radius
    ) {
        if (categoryGroupCode == null || categoryGroupCode.isBlank()) {
            return List.of();
        }

        if (longitude == null || latitude == null) {
            return List.of();
        }

        try {
            KakaoPlaceResponse response = webClientBuilder
                    .baseUrl(BASE_URL)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(CATEGORY_SEARCH_PATH)
                            .queryParam("category_group_code", categoryGroupCode)
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("radius", radius)
                            .queryParam("size", DEFAULT_SIZE)
                            .queryParam("sort", "distance")
                            .build())
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .bodyToMono(KakaoPlaceResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                return List.of();
            }

            return response.getItems();
        } catch (WebClientResponseException e) {
            log.warn("Kakao Local category search response error. categoryGroupCode={}, status={}, body={}",
                    categoryGroupCode, e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (WebClientRequestException e) {
            log.warn("Kakao Local category search request error. categoryGroupCode={}, message={}",
                    categoryGroupCode, e.getMessage());
            return List.of();
        } catch (RuntimeException e) {
            log.error("Kakao Local category search unexpected error. categoryGroupCode={}, message={}",
                    categoryGroupCode, e.getMessage(), e);
            return List.of();
        }
    }
}